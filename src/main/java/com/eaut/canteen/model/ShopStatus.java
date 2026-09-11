package com.eaut.canteen.model;

import java.time.LocalDateTime;

/** The single-row "Mở đơn / Tạm ngưng nhận đơn" switch — see ShopStatusDAO. */
public class ShopStatus {

    private boolean acceptingOrders = true;
    private Integer updatedBy;
    private String updatedByName;
    private LocalDateTime updatedAt;

    public boolean isAcceptingOrders() {
        return acceptingOrders;
    }

    public void setAcceptingOrders(boolean acceptingOrders) {
        this.acceptingOrders = acceptingOrders;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
