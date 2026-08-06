package com.eaut.canteen.model;

public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    PAID("Đã thanh toán");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
