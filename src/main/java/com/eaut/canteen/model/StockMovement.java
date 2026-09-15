package com.eaut.canteen.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One line of the stock ledger: a signed change to one counter, with the reason and the person
 * behind it. Reading the ledger for a product and summing delta reproduces its current quantity,
 * which is what makes a wrong count traceable instead of merely wrong.
 */
public class StockMovement {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int movementId;
    private int productId;
    private StockLocation location;
    private int delta;
    private StockMovementReason reason;
    private Integer refOrderId;
    private String note;
    private int createdBy;
    private LocalDateTime createdAt;

    /** Joined for display so the ledger table does not need a lookup per row. */
    private String productName;
    private String createdByName;
    private String refOrderCode;

    public int getMovementId() {
        return movementId;
    }

    public void setMovementId(int movementId) {
        this.movementId = movementId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public StockLocation getLocation() {
        return location;
    }

    public void setLocation(StockLocation location) {
        this.location = location;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public StockMovementReason getReason() {
        return reason;
    }

    public void setReason(StockMovementReason reason) {
        this.reason = reason;
    }

    public Integer getRefOrderId() {
        return refOrderId;
    }

    public void setRefOrderId(Integer refOrderId) {
        this.refOrderId = refOrderId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getRefOrderCode() {
        return refOrderCode;
    }

    public void setRefOrderCode(String refOrderCode) {
        this.refOrderCode = refOrderCode;
    }

    /** "+12" / "-3" — the sign is the information, so it is shown even when positive. */
    public String getDeltaDisplay() {
        return (delta > 0 ? "+" : "") + delta;
    }

    public boolean isIncrease() {
        return delta > 0;
    }

    public String getCreatedAtDisplay() {
        return createdAt == null ? "" : createdAt.format(DISPLAY_FORMAT);
    }
}
