package com.eaut.canteen.model;

/**
 * Vòng đời một phiếu nhập hàng. Mirrors the CHECK on stock_imports.status.
 *
 * <p>The whole reason this enum exists is that stock must not move when the paperwork is written —
 * it must move when the goods are counted. A DRAFT is a promise from a supplier; only receiving
 * turns it into stock. That gap is where shortages, breakages and wrong deliveries get caught,
 * and the old one-step form had nowhere to record them.
 *
 * <p>PARTIAL is not a failure state. A supplier delivering 8 of 10 crates today and 2 tomorrow is
 * ordinary, so the phiếu stays open and can be received again. What closes it is either the rest
 * arriving or someone deciding the rest never will ({@code RECEIVED} via "Kết thúc").
 */
public enum StockImportStatus {
    DRAFT("Chưa nhập", "Đã đặt hàng, chưa kiểm hàng — kho chưa cộng"),
    PARTIAL("Nhập một phần", "Đã nhận một phần, vẫn còn hàng đang về"),
    RECEIVED("Đã nhập", "Hàng đã vào kho, phiếu đã chốt"),
    CANCELLED("Đã hủy", "Hủy trước khi nhận bất kỳ món nào");

    private final String displayName;
    private final String hint;

    StockImportStatus(String displayName, String hint) {
        this.displayName = displayName;
        this.hint = hint;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHint() {
        return hint;
    }

    /** Còn nhận hàng được không — quyết định phiếu có hiện nút "Kiểm hàng" hay không. */
    public boolean isOpen() {
        return this == DRAFT || this == PARTIAL;
    }

    /**
     * Sửa được nội dung phiếu (nhà cung cấp, dòng hàng, đơn giá) hay không.
     *
     * <p>Chỉ khi chưa nhận món nào. Sau khi đã nhận, các con số trên phiếu đã được ghi vào
     * warehouse_stock và stock_movements; sửa chúng sẽ làm sổ kho nói một đằng, kho một nẻo —
     * đúng kiểu sai lệch mà cả sổ cái sinh ra để ngăn.
     */
    public boolean isEditable() {
        return this == DRAFT;
    }

    /** Xóa hẳn được hay không. Phiếu đã nhận là chứng từ, chỉ được hủy chứ không được xóa. */
    public boolean isDeletable() {
        return this == DRAFT || this == CANCELLED;
    }

    /** Tên lớp CSS cho badge trạng thái, để JSP không phải chứa logic if/else. */
    public String getBadgeClass() {
        return switch (this) {
            case DRAFT -> "badge-warning";
            case PARTIAL -> "badge-info";
            case RECEIVED -> "badge-success";
            case CANCELLED -> "badge-muted";
        };
    }

    public static StockImportStatus fromDb(String raw) {
        if (raw == null) {
            return DRAFT;
        }
        try {
            return valueOf(raw);
        } catch (IllegalArgumentException e) {
            return DRAFT;
        }
    }
}
