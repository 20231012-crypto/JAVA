package com.eaut.canteen.model;

public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng"),
    VIETQR("Chuyển khoản VietQR"),
    CASH("Tiền mặt tại quầy"),
    WALLET("Ví EAUT Pay");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
