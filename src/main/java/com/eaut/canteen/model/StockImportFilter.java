package com.eaut.canteen.model;

import java.time.LocalDateTime;

/**
 * Điều kiện lọc danh sách phiếu nhập. Mọi trường đều không bắt buộc — null nghĩa là "không lọc
 * theo tiêu chí này" — nên một truy vấn phục vụ được cả danh sách đầy đủ lẫn mọi tổ hợp bộ lọc.
 *
 * <p>Dùng record thay vì năm tham số rời vì cùng một bộ điều kiện phải đi qua {@code findFiltered}
 * và {@code countFiltered} khớp nhau tuyệt đối; hai danh sách tham số lệch nhau chính là cách bộ
 * đếm trang bắt đầu nói sai số trang — lỗi đã từng xảy ra ở countCustomers/findCustomers.
 *
 * <p>Record nên EL không đọc được accessor: mọi trường cần hiện trên JSP phải có getter tường minh
 * (xem ChartPoint, RevenuePoint).
 *
 * @param status     trạng thái phiếu, hoặc null cho tất cả
 * @param supplierId nhà cung cấp, hoặc null cho tất cả
 * @param from       chặn dưới của imported_at, đã đổi sang múi giờ lưu trữ bởi AppClock
 * @param to         chặn trên của imported_at, cùng cách đổi
 * @param query      tìm tự do trên mã phiếu, tên NCC và ghi chú
 */
public record StockImportFilter(
        StockImportStatus status,
        Integer supplierId,
        LocalDateTime from,
        LocalDateTime to,
        String query) {

    public static StockImportFilter none() {
        return new StockImportFilter(null, null, null, null, null);
    }

    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }

    public boolean isActive() {
        return status != null || supplierId != null || from != null || to != null || hasQuery();
    }

    // EL 5.0 không đọc được accessor của record, nên các getter dưới đây là bắt buộc để JSP
    // giữ lại giá trị đang lọc trong form.

    public StockImportStatus getStatus() {
        return status;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public String getQuery() {
        return query;
    }

    public boolean getActive() {
        return isActive();
    }
}
