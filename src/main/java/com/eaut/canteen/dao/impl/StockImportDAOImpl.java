package com.eaut.canteen.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.dao.StockImportDAO;
import com.eaut.canteen.model.StockImport;
import com.eaut.canteen.model.StockImportFilter;
import com.eaut.canteen.model.StockImportStatus;

public class StockImportDAOImpl implements StockImportDAO {

    private static final String NEXT_CODE = "SELECT nextval('stock_import_code_seq')";

    private static final String INSERT =
            "INSERT INTO stock_imports (code, admin_id, supplier_id, supplier_name, status, " +
            "expected_date, discount_amount, other_cost, paid_amount, note) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_HEADER =
            "UPDATE stock_imports SET supplier_id = ?, supplier_name = ?, expected_date = ?, " +
            "discount_amount = ?, other_cost = ?, paid_amount = ?, note = ?, " +
            "updated_at = CURRENT_TIMESTAMP WHERE import_id = ?";

    private static final String UPDATE_PAYMENT =
            "UPDATE stock_imports SET paid_amount = ?, updated_at = CURRENT_TIMESTAMP WHERE import_id = ?";

    private static final String DELETE = "DELETE FROM stock_imports WHERE import_id = ?";

    /**
     * Tổng tiền hàng và số lượng được gộp bằng truy vấn con thay vì JOIN + GROUP BY trên cả bảng.
     * Lý do thực dụng: danh sách có phân trang, LIMIT phải áp trên số phiếu chứ không phải số dòng
     * hàng, và một JOIN sẽ nhân bản hàng trước khi LIMIT kịp cắt.
     */
    private static final String BASE_SELECT =
            "SELECT i.import_id, i.code, i.admin_id, i.supplier_id, i.supplier_name, i.status, " +
            "       i.expected_date, i.discount_amount, i.other_cost, i.paid_amount, i.note, " +
            "       i.imported_at, i.received_at, " +
            "       u.full_name AS admin_name, r.full_name AS received_by_name, " +
            "       s.name AS supplier_current_name, " +
            "       COALESCE(agg.subtotal, 0)   AS subtotal, " +
            "       COALESCE(agg.line_count, 0) AS line_count, " +
            "       COALESCE(agg.ordered_qty, 0)  AS ordered_qty, " +
            "       COALESCE(agg.received_qty, 0) AS received_qty " +
            "FROM stock_imports i " +
            "JOIN users u ON i.admin_id = u.user_id " +
            "LEFT JOIN users r ON i.received_by = r.user_id " +
            "LEFT JOIN suppliers s ON i.supplier_id = s.supplier_id " +
            "LEFT JOIN ( " +
            "    SELECT import_id, SUM(unit_cost * quantity) AS subtotal, COUNT(*) AS line_count, " +
            "           SUM(quantity) AS ordered_qty, SUM(received_quantity) AS received_qty " +
            "    FROM stock_import_items GROUP BY import_id " +
            ") agg ON agg.import_id = i.import_id ";

    private static final String FIND_BY_ID = BASE_SELECT + "WHERE i.import_id = ?";

    private static final String COUNT_BY_STATUS =
            "SELECT status, COUNT(*) AS n FROM stock_imports GROUP BY status";

    private static final String INCOMING =
            "SELECT it.product_id, SUM(it.quantity - it.received_quantity) AS incoming " +
            "FROM stock_import_items it " +
            "JOIN stock_imports i ON i.import_id = it.import_id " +
            "WHERE i.status IN ('DRAFT', 'PARTIAL') AND it.quantity > it.received_quantity " +
            "GROUP BY it.product_id";

    @Override
    public int insert(Connection conn, StockImport stockImport) throws SQLException {
        // Mã lấy từ sequence trước khi chèn, cùng lý do với order_ticket_seq: có mã sẵn nên chỉ một
        // câu INSERT, không phải chèn rồi UPDATE lại, và hai người tạo phiếu cùng lúc không đụng mã.
        String code;
        try (PreparedStatement seq = conn.prepareStatement(NEXT_CODE);
             ResultSet rs = seq.executeQuery()) {
            rs.next();
            code = "PN-" + rs.getLong(1);
        }
        stockImport.setCode(code);

        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setInt(2, stockImport.getAdminId());
            setNullableInt(ps, 3, stockImport.getSupplierId());
            ps.setString(4, stockImport.getSupplierName());
            ps.setString(5, stockImport.getStatus().name());
            setNullableDate(ps, 6, stockImport.getExpectedDate());
            ps.setBigDecimal(7, stockImport.getDiscountAmount());
            ps.setBigDecimal(8, stockImport.getOtherCost());
            ps.setBigDecimal(9, stockImport.getPaidAmount());
            ps.setString(10, stockImport.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public void updateHeader(Connection conn, StockImport stockImport) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_HEADER)) {
            setNullableInt(ps, 1, stockImport.getSupplierId());
            ps.setString(2, stockImport.getSupplierName());
            setNullableDate(ps, 3, stockImport.getExpectedDate());
            ps.setBigDecimal(4, stockImport.getDiscountAmount());
            ps.setBigDecimal(5, stockImport.getOtherCost());
            ps.setBigDecimal(6, stockImport.getPaidAmount());
            ps.setString(7, stockImport.getNote());
            ps.setInt(8, stockImport.getImportId());
            ps.executeUpdate();
        }
    }

    @Override
    public int updateStatus(Connection conn, int importId, StockImportStatus expected,
                            StockImportStatus next, Integer actorId) throws SQLException {
        // WHERE status = ? là chốt chống hai người cùng bấm: người thứ hai đổi 0 hàng và bị rollback,
        // thay vì cộng kho lần thứ hai. Cùng khuôn với OrderDAO.updateStatus và markRefunded.
        StringBuilder sql = new StringBuilder(
                "UPDATE stock_imports SET status = ?, updated_at = CURRENT_TIMESTAMP");
        if (next == StockImportStatus.RECEIVED || next == StockImportStatus.PARTIAL) {
            sql.append(", received_at = CURRENT_TIMESTAMP, received_by = ?");
        } else if (next == StockImportStatus.CANCELLED) {
            sql.append(", cancelled_at = CURRENT_TIMESTAMP");
        }
        sql.append(" WHERE import_id = ? AND status = ?");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, next.name());
            if (next == StockImportStatus.RECEIVED || next == StockImportStatus.PARTIAL) {
                setNullableInt(ps, idx++, actorId);
            }
            ps.setInt(idx++, importId);
            ps.setString(idx, expected.name());
            return ps.executeUpdate();
        }
    }

    @Override
    public void updatePayment(Connection conn, int importId, BigDecimal paidAmount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_PAYMENT)) {
            ps.setBigDecimal(1, paidAmount);
            ps.setInt(2, importId);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Connection conn, int importId) throws SQLException {
        // stock_import_items có ON DELETE CASCADE nên các dòng đi theo. An toàn vì chỉ phiếu chưa
        // từng nhận hàng mới xóa được — không có stock_movements nào trỏ về đây để mồ côi.
        try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setInt(1, importId);
            ps.executeUpdate();
        }
    }

    @Override
    public StockImport findById(Connection conn, int importId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, importId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public List<StockImport> findFiltered(Connection conn, StockImportFilter filter, int limit, int offset)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(filter, params);
        String sql = BASE_SELECT + where + " ORDER BY i.imported_at DESC, i.import_id DESC LIMIT ? OFFSET ?";

        List<StockImport> imports = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindAll(ps, params);
            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    imports.add(map(rs));
                }
            }
        }
        return imports;
    }

    @Override
    public int countFiltered(Connection conn, StockImportFilter filter) throws SQLException {
        List<Object> params = new ArrayList<>();
        // Dùng CHUNG buildWhere với findFiltered. Hai mệnh đề WHERE viết tay riêng cho hai truy vấn
        // là cách bộ đếm trang bắt đầu lệch với số hàng thật sự hiển thị.
        String where = buildWhere(filter, params);
        String sql = "SELECT COUNT(*) FROM stock_imports i " +
                "LEFT JOIN suppliers s ON i.supplier_id = s.supplier_id " + where;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindAll(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public Map<StockImportStatus, Integer> countByStatus(Connection conn) throws SQLException {
        Map<StockImportStatus, Integer> counts = new EnumMap<>(StockImportStatus.class);
        for (StockImportStatus status : StockImportStatus.values()) {
            counts.put(status, 0);
        }
        try (PreparedStatement ps = conn.prepareStatement(COUNT_BY_STATUS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(StockImportStatus.fromDb(rs.getString("status")), rs.getInt("n"));
            }
        }
        return counts;
    }

    @Override
    public Map<Integer, Integer> incomingByProduct(Connection conn) throws SQLException {
        Map<Integer, Integer> incoming = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(INCOMING);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                incoming.put(rs.getInt("product_id"), rs.getInt("incoming"));
            }
        }
        return incoming;
    }

    /** Dựng WHERE động; giá trị luôn đi qua ? chứ không nối chuỗi. */
    private String buildWhere(StockImportFilter filter, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (filter.status() != null) {
            where.append(" AND i.status = ?");
            params.add(filter.status().name());
        }
        if (filter.supplierId() != null) {
            where.append(" AND i.supplier_id = ?");
            params.add(filter.supplierId());
        }
        if (filter.from() != null) {
            where.append(" AND i.imported_at >= ?");
            params.add(Timestamp.valueOf(filter.from()));
        }
        if (filter.to() != null) {
            where.append(" AND i.imported_at < ?");
            params.add(Timestamp.valueOf(filter.to()));
        }
        if (filter.hasQuery()) {
            // unaccent để gõ "minh anh" tìm ra "Minh Ánh" — cùng cách trang đơn hàng đang làm.
            where.append(" AND (unaccent(i.code) ILIKE unaccent(?)")
                 .append(" OR unaccent(COALESCE(i.supplier_name, '')) ILIKE unaccent(?)")
                 .append(" OR unaccent(COALESCE(s.name, '')) ILIKE unaccent(?)")
                 .append(" OR unaccent(COALESCE(i.note, '')) ILIKE unaccent(?))");
            String like = "%" + filter.query().trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        return where.toString();
    }

    private int bindAll(PreparedStatement ps, List<Object> params) throws SQLException {
        int idx = 1;
        for (Object param : params) {
            ps.setObject(idx++, param);
        }
        return idx;
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.INTEGER);
        } else {
            ps.setInt(idx, value);
        }
    }

    private void setNullableDate(PreparedStatement ps, int idx, java.time.LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.DATE);
        } else {
            ps.setDate(idx, Date.valueOf(value));
        }
    }

    private StockImport map(ResultSet rs) throws SQLException {
        StockImport stockImport = new StockImport();
        stockImport.setImportId(rs.getInt("import_id"));
        stockImport.setCode(rs.getString("code"));
        stockImport.setAdminId(rs.getInt("admin_id"));
        stockImport.setAdminName(rs.getString("admin_name"));

        int supplierId = rs.getInt("supplier_id");
        stockImport.setSupplierId(rs.wasNull() ? null : supplierId);

        // Tên NCC hiện tại được ưu tiên khi phiếu còn trỏ tới một NCC có thật, để sửa chính tả ở
        // trang NCC là cả danh sách phiếu đọc đúng ngay. Bản chụp supplier_name chỉ dùng khi NCC
        // đã bị gỡ liên kết — khi đó nó là thứ duy nhất còn nói được hàng mua của ai.
        String currentName = rs.getString("supplier_current_name");
        String snapshot = rs.getString("supplier_name");
        stockImport.setSupplierName(currentName != null ? currentName : snapshot);

        stockImport.setStatus(StockImportStatus.fromDb(rs.getString("status")));
        Date expected = rs.getDate("expected_date");
        stockImport.setExpectedDate(expected == null ? null : expected.toLocalDate());
        stockImport.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        stockImport.setOtherCost(rs.getBigDecimal("other_cost"));
        stockImport.setPaidAmount(rs.getBigDecimal("paid_amount"));
        stockImport.setNote(rs.getString("note"));
        stockImport.setImportedAt(rs.getTimestamp("imported_at").toLocalDateTime());

        Timestamp receivedAt = rs.getTimestamp("received_at");
        stockImport.setReceivedAt(receivedAt == null ? null : receivedAt.toLocalDateTime());
        stockImport.setReceivedByName(rs.getString("received_by_name"));

        stockImport.setSubtotal(rs.getBigDecimal("subtotal"));
        stockImport.setLineCount(rs.getInt("line_count"));
        stockImport.setOrderedQuantity(rs.getInt("ordered_qty"));
        stockImport.setReceivedQuantity(rs.getInt("received_qty"));
        return stockImport;
    }
}
