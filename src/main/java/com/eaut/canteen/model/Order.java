package com.eaut.canteen.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Order {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int orderId;
    private Integer customerId;
    private Integer buildingId;
    private String buildingName;
    private OrderChannel channel;
    private Integer soldBy;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Integer paymentConfirmedBy;
    private LocalDateTime paymentConfirmedAt;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Integer buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public OrderChannel getChannel() {
        return channel;
    }

    public void setChannel(OrderChannel channel) {
        this.channel = channel;
    }

    public Integer getSoldBy() {
        return soldBy;
    }

    public void setSoldBy(Integer soldBy) {
        this.soldBy = soldBy;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Integer getPaymentConfirmedBy() {
        return paymentConfirmedBy;
    }

    public void setPaymentConfirmedBy(Integer paymentConfirmedBy) {
        this.paymentConfirmedBy = paymentConfirmedBy;
    }

    public LocalDateTime getPaymentConfirmedAt() {
        return paymentConfirmedAt;
    }

    public void setPaymentConfirmedAt(LocalDateTime paymentConfirmedAt) {
        this.paymentConfirmedAt = paymentConfirmedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** JSTL's fmt:formatDate can't format java.time types, so views use this instead. */
    public String getCreatedAtDisplay() {
        return createdAt == null ? "" : createdAt.format(DISPLAY_FORMAT);
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
