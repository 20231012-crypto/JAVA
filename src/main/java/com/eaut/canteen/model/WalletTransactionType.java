package com.eaut.canteen.model;

public enum WalletTransactionType {
    TOPUP("Nạp tiền"),
    PAYMENT("Thanh toán đơn hàng"),
    REFUND("Hoàn tiền");

    private final String displayName;

    WalletTransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
