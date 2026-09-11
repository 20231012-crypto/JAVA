package com.eaut.canteen.model;

public enum WalletTopupStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã cộng vào ví"),
    REJECTED("Đã từ chối");

    private final String displayName;

    WalletTopupStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
