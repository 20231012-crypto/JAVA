package com.eaut.canteen.model;

import java.math.BigDecimal;

public class StockImportItem {

    private int importItemId;
    private int importId;
    private int productId;
    private String productName;
    private int quantity;
    private BigDecimal unitCost;
    private int receivedQuantity;
    private int currentStock;

    public int getImportItemId() {
        return importItemId;
    }

    public void setImportItemId(int importItemId) {
        this.importItemId = importItemId;
    }

    public int getImportId() {
        return importId;
    }

    public void setImportId(int importId) {
        this.importId = importId;
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

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    /**
     * Số đã thực sự đếm được khi hàng về, có thể ít hơn {@code quantity} (số đặt).
     *
     * <p>Chính khoảng chênh giữa hai con số này là lý do quy trình được viết lại: form cũ chỉ có
     * một ô số lượng, nên đặt 10 mà nhà cung cấp giao 8 vẫn được ghi vào kho là 10.
     */
    public int getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(int receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    /** Còn thiếu bao nhiêu so với số đặt. */
    public int getPendingQuantity() {
        int pending = quantity - receivedQuantity;
        return pending < 0 ? 0 : pending;
    }

    public boolean isFullyReceived() {
        return receivedQuantity >= quantity;
    }

    /** Thành tiền theo số ĐẶT — đây là số tiền trên hóa đơn nhà cung cấp gửi. */
    public BigDecimal getLineTotal() {
        return unitCost == null ? BigDecimal.ZERO : unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Thành tiền theo số THỰC NHẬN — số đáng lẽ phải trả nếu giao thiếu.
     *
     * <p>Hai con số này khác nhau là bằng chứng để đi đòi nhà cung cấp, nên cả hai đều hiện trên
     * phiếu chứ không chỉ một.
     */
    public BigDecimal getReceivedTotal() {
        return unitCost == null ? BigDecimal.ZERO
                : unitCost.multiply(BigDecimal.valueOf(receivedQuantity));
    }

    /** Tồn kho hiện tại của món, chỉ nạp ở màn kiểm hàng để người đếm thấy bối cảnh. */
    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }
}
