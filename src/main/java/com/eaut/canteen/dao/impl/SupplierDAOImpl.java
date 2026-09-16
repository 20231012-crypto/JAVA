package com.eaut.canteen.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.SupplierDAO;
import com.eaut.canteen.model.Supplier;

public class SupplierDAOImpl implements SupplierDAO {

    private static final String INSERT =
            "INSERT INTO suppliers (name, phone, email, address, note) VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE suppliers SET name = ?, phone = ?, email = ?, address = ?, note = ? " +
            "WHERE supplier_id = ?";

    private static final String SET_ACTIVE =
            "UPDATE suppliers SET is_active = ? WHERE supplier_id = ?";

    private static final String BASE_SELECT =
            "SELECT supplier_id, name, phone, email, address, note, is_active FROM suppliers ";

    private static final String FIND_BY_ID = BASE_SELECT + "WHERE supplier_id = ?";

    private static final String FIND_ACTIVE = BASE_SELECT + "WHERE is_active ORDER BY name";

    /**
     * Danh sách kèm số liệu. Công nợ chỉ tính trên phiếu đã nhận hàng — một phiếu còn ở trạng thái
     * "chưa nhập" là dự định mua, chưa phải khoản nợ, nên gộp vào sẽ thổi phồng số tiền đang nợ.
     *
     * <p>Tổng tiền phải tính lại từ các dòng ở đây thay vì đọc một cột có sẵn, vì phiếu không lưu
     * tổng (xem StockImport.getTotal). Gom trong một truy vấn con thay vì N+1 câu cho mỗi NCC.
     */
    private static final String FIND_WITH_STATS =
            "SELECT s.supplier_id, s.name, s.phone, s.email, s.address, s.note, s.is_active, " +
            "       COALESCE(t.import_count, 0)  AS import_count, " +
            "       COALESCE(t.total_value, 0)   AS total_value, " +
            "       COALESCE(t.total_debt, 0)    AS total_debt " +
            "FROM suppliers s " +
            "LEFT JOIN ( " +
            "    SELECT i.supplier_id, " +
            "           COUNT(*)                                   AS import_count, " +
            "           SUM(i.line_total)                          AS total_value, " +
            "           SUM(GREATEST(i.line_total - i.paid_amount, 0)) AS total_debt " +
            "    FROM ( " +
            "        SELECT si.supplier_id, si.paid_amount, " +
            "               GREATEST(COALESCE(( " +
            "                   SELECT SUM(it.unit_cost * it.quantity) " +
            "                   FROM stock_import_items it WHERE it.import_id = si.import_id " +
            "               ), 0) - si.discount_amount + si.other_cost, 0) AS line_total " +
            "        FROM stock_imports si " +
            "        WHERE si.supplier_id IS NOT NULL AND si.status IN ('PARTIAL', 'RECEIVED') " +
            "    ) i GROUP BY i.supplier_id " +
            ") t ON t.supplier_id = s.supplier_id " +
            "ORDER BY s.is_active DESC, s.name";

    private static final String NAME_EXISTS =
            "SELECT 1 FROM suppliers WHERE lower(name) = lower(?) AND is_active AND supplier_id <> ?";

    @Override
    public int insert(Connection conn, Supplier supplier) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bindEditable(ps, supplier);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public void update(Connection conn, Supplier supplier) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            bindEditable(ps, supplier);
            ps.setInt(6, supplier.getSupplierId());
            ps.executeUpdate();
        }
    }

    @Override
    public void setActive(Connection conn, int supplierId, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SET_ACTIVE)) {
            ps.setBoolean(1, active);
            ps.setInt(2, supplierId);
            ps.executeUpdate();
        }
    }

    @Override
    public Supplier findById(Connection conn, int supplierId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public List<Supplier> findAllActive(Connection conn) throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                suppliers.add(map(rs));
            }
        }
        return suppliers;
    }

    @Override
    public List<Supplier> findAllWithStats(Connection conn) throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_WITH_STATS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Supplier supplier = map(rs);
                supplier.setImportCount(rs.getInt("import_count"));
                supplier.setTotalValue(rs.getBigDecimal("total_value"));
                supplier.setTotalDebt(rs.getBigDecimal("total_debt"));
                suppliers.add(supplier);
            }
        }
        return suppliers;
    }

    @Override
    public boolean nameExists(Connection conn, String name, Integer excludeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(NAME_EXISTS)) {
            ps.setString(1, name);
            ps.setInt(2, excludeId == null ? -1 : excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void bindEditable(PreparedStatement ps, Supplier supplier) throws SQLException {
        ps.setString(1, supplier.getName());
        ps.setString(2, supplier.getPhone());
        ps.setString(3, supplier.getEmail());
        ps.setString(4, supplier.getAddress());
        ps.setString(5, supplier.getNote());
    }

    private Supplier map(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setSupplierId(rs.getInt("supplier_id"));
        supplier.setName(rs.getString("name"));
        supplier.setPhone(rs.getString("phone"));
        supplier.setEmail(rs.getString("email"));
        supplier.setAddress(rs.getString("address"));
        supplier.setNote(rs.getString("note"));
        supplier.setActive(rs.getBoolean("is_active"));
        supplier.setTotalValue(BigDecimal.ZERO);
        supplier.setTotalDebt(BigDecimal.ZERO);
        return supplier;
    }
}
