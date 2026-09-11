package com.eaut.canteen.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A customer's self-service "nạp ví" request. There is no real bank API link — this is the same
 * trust model as VietQR order payments (see VietQRUtil): the customer transfers manually, a human
 * (admin/staff) checks the bank account and confirms, which is what actually credits the wallet.
 */
public class WalletTopupRequest {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int requestId;
    private int userId;
    private String customerName; // populated by findAllPending's join for the admin queue; unused for the customer's own view
    private BigDecimal amount;
    private WalletTopupStatus status;
    private Integer confirmedBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public WalletTopupStatus getStatus() {
        return status;
    }

    public void setStatus(WalletTopupStatus status) {
        this.status = status;
    }

    public Integer getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(Integer confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedAtDisplay() {
        return createdAt == null ? "" : createdAt.format(DISPLAY_FORMAT);
    }

    /** The bank-transfer content the customer must keep unedited — see VietQRUtil#transferNote-equivalent usage. */
    public String getTransferNote() {
        return "NAPVI" + requestId;
    }
}
