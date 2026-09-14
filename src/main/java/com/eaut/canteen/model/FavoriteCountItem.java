package com.eaut.canteen.model;

/** One row in the admin "món được yêu thích nhiều nhất" report — real counts from `favorites`. */
public record FavoriteCountItem(int productId, String productName, String imageFilename, int favoriteCount) {

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }
}
