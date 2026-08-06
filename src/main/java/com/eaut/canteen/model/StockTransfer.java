package com.eaut.canteen.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StockTransfer {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int transferId;
    private int storeStaffId;
    private String storeStaffName;
    private int productId;
    private String productName;
    private int quantity;
    private LocalDateTime transferredAt;

    public int getTransferId() {
        return transferId;
    }

    public void setTransferId(int transferId) {
        this.transferId = transferId;
    }

    public int getStoreStaffId() {
        return storeStaffId;
    }

    public void setStoreStaffId(int storeStaffId) {
        this.storeStaffId = storeStaffId;
    }

    public String getStoreStaffName() {
        return storeStaffName;
    }

    public void setStoreStaffName(String storeStaffName) {
        this.storeStaffName = storeStaffName;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getTransferredAt() {
        return transferredAt;
    }

    public void setTransferredAt(LocalDateTime transferredAt) {
        this.transferredAt = transferredAt;
    }

    public String getTransferredAtDisplay() {
        return transferredAt == null ? "" : transferredAt.format(DISPLAY_FORMAT);
    }
}
