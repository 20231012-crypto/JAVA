package com.eaut.canteen.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Product {

    private int productId;
    private int categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    /** "Was" price shown crossed out when set and greater than price; NULL = no promo. Admin-set, never fabricated. */
    private BigDecimal originalPrice;
    /** Real "Đã bán X/Y" progress target (Y); NULL = no progress bar shown. Admin-set. */
    private Integer promoTargetQuantity;
    /** X in "Đã bán X/Y" — real completed-order quantity, populated by DAO methods that join it (catalog/admin listing). */
    private int soldQuantity;
    private String imageFilename;
    private String unit;
    private boolean active;
    private int avgPrepMinutes = 10;
    /** Populated only by DAO methods that join shelf_stock (e.g. catalog listing). */
    private int shelfQuantity;
    /** Populated only by DAO methods that join warehouse_stock (e.g. admin listing). */
    private int warehouseQuantity;
    /** Not from the products table — set manually by the controller from FavoriteDAO for the current session's user, if logged in. */
    private boolean favoritedByCurrentUser;

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Integer getPromoTargetQuantity() {
        return promoTargetQuantity;
    }

    public void setPromoTargetQuantity(Integer promoTargetQuantity) {
        this.promoTargetQuantity = promoTargetQuantity;
    }

    public int getSoldQuantity() {
        return soldQuantity;
    }

    public void setSoldQuantity(int soldQuantity) {
        this.soldQuantity = soldQuantity;
    }

    public boolean isOnPromo() {
        return originalPrice != null && originalPrice.compareTo(price) > 0;
    }

    public int getDiscountPercent() {
        if (!isOnPromo()) {
            return 0;
        }
        return originalPrice.subtract(price)
                .multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    public boolean isShowSoldProgress() {
        return promoTargetQuantity != null && promoTargetQuantity > 0;
    }

    public int getSoldProgressPercent() {
        if (!isShowSoldProgress()) {
            return 0;
        }
        return Math.min(100, (int) Math.round(soldQuantity * 100.0 / promoTargetQuantity));
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Backs the KDS countdown timer (orders.estimated_ready_at) — admin-editable per product, default 10. */
    public int getAvgPrepMinutes() {
        return avgPrepMinutes;
    }

    public void setAvgPrepMinutes(int avgPrepMinutes) {
        this.avgPrepMinutes = avgPrepMinutes;
    }

    public int getShelfQuantity() {
        return shelfQuantity;
    }

    public void setShelfQuantity(int shelfQuantity) {
        this.shelfQuantity = shelfQuantity;
    }

    public boolean isInStock() {
        return shelfQuantity > 0;
    }

    public int getWarehouseQuantity() {
        return warehouseQuantity;
    }

    public void setWarehouseQuantity(int warehouseQuantity) {
        this.warehouseQuantity = warehouseQuantity;
    }

    public boolean isFavoritedByCurrentUser() {
        return favoritedByCurrentUser;
    }

    public void setFavoritedByCurrentUser(boolean favoritedByCurrentUser) {
        this.favoritedByCurrentUser = favoritedByCurrentUser;
    }
}
