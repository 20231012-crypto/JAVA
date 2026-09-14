package com.eaut.canteen.model;

/**
 * One real, recently-placed order item — backs the "vừa có người đặt món này" toast on the
 * catalog page. Deliberately carries no customer identity, only what's needed to feel current:
 * the dish and which building it's headed to.
 */
public record RecentActivityItem(String productName, String buildingName, int minutesAgo) {

    public String getProductName() {
        return productName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public int getMinutesAgo() {
        return minutesAgo;
    }
}
