package com.eaut.canteen.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class CartItem implements Serializable {

    private int productId;
    private String productName;
    private BigDecimal unitPrice;
    private String imageFilename;
    private int quantity;

    public CartItem(int productId, String productName, BigDecimal unitPrice, String imageFilename, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.imageFilename = imageFilename;
        this.quantity = quantity;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
