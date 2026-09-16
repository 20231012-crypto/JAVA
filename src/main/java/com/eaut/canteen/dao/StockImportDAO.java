package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.model.StockImport;
import com.eaut.canteen.model.StockImportFilter;
import com.eaut.canteen.model.StockImportStatus;

public interface StockImportDAO {

    /** Sinh mã phiếu từ sequence rồi chèn. Phiếu mới luôn ở DRAFT — kho chưa động đến. */
    int insert(Connection conn, StockImport stockImport) throws SQLException;

    /** Sửa phần đầu phiếu. Gọi được hay không do {@link StockImportStatus#isEditable()} quyết định. */
    void updateHeader(Connection conn, StockImport stockImport) throws SQLException;

    /**
     * Đổi trạng thái có chốt: chỉ đổi khi phiếu vẫn đang ở {@code expected}.
     *
     * @return số hàng đã đổi — 0 nghĩa là ai đó vừa đổi trạng thái phiếu này ở tab khác, gọi phải
     *         rollback. Cùng khuôn với OrderDAO.updateStatus.
     */
    int updateStatus(Connection conn, int importId, StockImportStatus expected,
                     StockImportStatus next, Integer actorId) throws SQLException;

    /** Ghi số tiền đã trả nhà cung cấp. */
    void updatePayment(Connection conn, int importId, java.math.BigDecimal paidAmount) throws SQLException;

    /** Xóa hẳn. Chỉ dùng cho phiếu chưa từng nhận hàng — xem {@link StockImportStatus#isDeletable()}. */
    void delete(Connection conn, int importId) throws SQLException;

    StockImport findById(Connection conn, int importId) throws SQLException;

    List<StockImport> findFiltered(Connection conn, StockImportFilter filter, int limit, int offset)
            throws SQLException;

    int countFiltered(Connection conn, StockImportFilter filter) throws SQLException;

    /** Số phiếu theo từng trạng thái, cho các tab đếm trên đầu danh sách. */
    Map<StockImportStatus, Integer> countByStatus(Connection conn) throws SQLException;

    /**
     * "Hàng đang về": với mỗi món, tổng số đã đặt mà chưa nhận, gộp từ mọi phiếu còn mở.
     *
     * <p>Đây là con số phân biệt "hết hàng, phải đặt gấp" với "hết hàng nhưng chiều nay có" — trang
     * tồn kho trước đây không có nên nhìn đâu cũng thấy phải đặt thêm.
     */
    Map<Integer, Integer> incomingByProduct(Connection conn) throws SQLException;
}
