package com.eaut.canteen.model;

import java.math.BigDecimal;

public class StockImportItem {

    private int importItemId;
    private int importId;
    private int productId;
    private String productName;
    private int quantity;
    private BigDecimal unitCost;

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
}
