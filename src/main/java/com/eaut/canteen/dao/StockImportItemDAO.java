package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.StockImportItem;

public interface StockImportItemDAO {

    void insert(Connection conn, StockImportItem item) throws SQLException;

    /** Kèm tên món và tồn kho hiện tại, để màn kiểm hàng không phải hỏi từng món một. */
    List<StockImportItem> findByImportId(Connection conn, int importId) throws SQLException;

    /**
     * Ghi số thực nhận cho một dòng.
     *
     * <p>Chốt {@code WHERE received_quantity = ?} là để hai người cùng kiểm một phiếu không cộng
     * kho hai lần: người thứ hai thấy 0 hàng đổi và cả giao dịch bị rollback.
     *
     * @return số hàng đã đổi; 0 nghĩa là dòng vừa bị người khác cập nhật
     */
    int updateReceived(Connection conn, int importItemId, int expectedReceived, int newReceived)
            throws SQLException;

    /** Xóa sạch các dòng của một phiếu — bước đầu của "sửa phiếu" (xóa rồi chèn lại). */
    void deleteByImportId(Connection conn, int importId) throws SQLException;
}
