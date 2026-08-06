package com.eaut.canteen.model;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    REJECTED("Bị từ chối"),
    SHIPPING("Đang giao"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
