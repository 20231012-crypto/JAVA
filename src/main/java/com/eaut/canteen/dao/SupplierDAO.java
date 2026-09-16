package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.Supplier;

public interface SupplierDAO {

    int insert(Connection conn, Supplier supplier) throws SQLException;

    void update(Connection conn, Supplier supplier) throws SQLException;

    /**
     * Ngừng/bật lại một nhà cung cấp. Không có xóa cứng: phiếu nhập cũ trỏ tới hàng này, và một
     * nhà cung cấp "đã nghỉ" vẫn phải đọc được tên trên chứng từ năm ngoái.
     */
    void setActive(Connection conn, int supplierId, boolean active) throws SQLException;

    Supplier findById(Connection conn, int supplierId) throws SQLException;

    /** Chỉ NCC đang hoạt động, để đổ vào ô chọn khi tạo phiếu. */
    List<Supplier> findAllActive(Connection conn) throws SQLException;

    /** Kèm số phiếu, tổng giá trị đã nhập và công nợ còn lại — cho màn hình danh sách. */
    List<Supplier> findAllWithStats(Connection conn) throws SQLException;

    /** Trùng tên là lỗi gõ chứ không phải hai NCC thật; {@code excludeId} để sửa không tự đụng mình. */
    boolean nameExists(Connection conn, String name, Integer excludeId) throws SQLException;
}
