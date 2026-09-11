package com.eaut.canteen.model;

public enum LoyaltyTransactionType {
    EARN("Tích điểm"),
    REDEEM("Đổi điểm");

    private final String displayName;

    LoyaltyTransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
