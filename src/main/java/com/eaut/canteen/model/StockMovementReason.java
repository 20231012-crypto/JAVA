package com.eaut.canteen.model;

/**
 * Why a stock number changed. Mirrors the CHECK on stock_movements.reason.
 *
 * <p>The direction is carried by the signed delta, not by the reason, so these read as events
 * rather than as "in" and "out" — a TRANSFER writes two rows, TRANSFER_OUT negative on the
 * warehouse and TRANSFER_IN positive on the shelf, and both halves of the move stay visible.
 */
public enum StockMovementReason {
    IMPORT("Nhập kho", "Nhận hàng từ nhà cung cấp"),
    TRANSFER_OUT("Xuất kho", "Chuyển từ kho lên kệ"),
    TRANSFER_IN("Nhập kệ", "Nhận lên kệ bán"),
    SALE("Bán hàng", "Trừ kệ khi khách đặt"),
    RESTOCK("Hoàn hàng", "Trả lại kệ khi đơn bị hủy hoặc từ chối"),
    WRITE_OFF("Hủy hàng", "Hỏng, hết hạn, rơi vỡ"),
    STOCK_TAKE("Kiểm kê", "Chỉnh về số đếm thực tế");

    private final String displayName;
    private final String hint;

    StockMovementReason(String displayName, String hint) {
        this.displayName = displayName;
        this.hint = hint;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHint() {
        return hint;
    }

    /**
     * The two reasons an admin may record by hand. Everything else is written as a side effect of
     * an operation that already happened (a sale, a transfer, a cancellation), so offering them in
     * the adjustment form would let someone log a sale that never occurred.
     */
    public boolean isManual() {
        return this == WRITE_OFF || this == STOCK_TAKE;
    }
}
