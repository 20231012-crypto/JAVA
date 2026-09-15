package com.eaut.canteen.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Order {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int orderId;
    private String orderCode;
    private Integer customerId;
    private Integer buildingId;
    private String buildingName;
    private String customerName;
    /** Contact number for the delivery screen's "Gọi khách" button; null for walk-in COUNTER sales. */
    private String customerPhone;
    private String customerStudentId;
    private String customerClassName;
    private OrderChannel channel;
    private Integer soldBy;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private int loyaltyPointsUsed;
    private BigDecimal loyaltyDiscountAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Integer paymentConfirmedBy;
    private LocalDateTime paymentConfirmedAt;
    private LocalDateTime estimatedReadyAt;
    private String note;
    /**
     * Refund bookkeeping. refundedAt doubles as the "already refunded" flag — the admin refund
     * claims the order with a conditional UPDATE on it, so a second click finds it non-null and is
     * refused rather than paying out twice. The amount is stored rather than assumed to be
     * totalAmount because a cancelled-but-unpaid COD order is refunded zero, and that has to read
     * differently from never having been processed.
     */
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    private LocalDateTime refundedAt;
    private Integer refundedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    /** Short ticket code shown to customers/kitchen board (e.g. "A-142"); assigned by OrderDAOImpl#insert. */
    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    /** MSSV — null unless the customer is an EAUT student who's gone through the checkout info gate. */
    public String getCustomerStudentId() {
        return customerStudentId;
    }

    public void setCustomerStudentId(String customerStudentId) {
        this.customerStudentId = customerStudentId;
    }

    public String getCustomerClassName() {
        return customerClassName;
    }

    public void setCustomerClassName(String customerClassName) {
        this.customerClassName = customerClassName;
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

    /** EAUT Smart ID automatic discount (see AppConfig "smartId.discountPercent"); zero when not applicable. */
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    /** Points redeemed on this order (tích điểm) — see AppConfig "loyalty.redeemValuePerPoint". */
    public int getLoyaltyPointsUsed() {
        return loyaltyPointsUsed;
    }

    public void setLoyaltyPointsUsed(int loyaltyPointsUsed) {
        this.loyaltyPointsUsed = loyaltyPointsUsed;
    }

    /** đồng value of loyaltyPointsUsed — tracked separately from discountAmount so the two show as distinct line items. */
    public BigDecimal getLoyaltyDiscountAmount() {
        return loyaltyDiscountAmount;
    }

    public void setLoyaltyDiscountAmount(BigDecimal loyaltyDiscountAmount) {
        this.loyaltyDiscountAmount = loyaltyDiscountAmount;
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

    /** Set when the order is confirmed (see OrderActionServlet) — drives the KDS countdown timer on the sales Kanban board. */
    public LocalDateTime getEstimatedReadyAt() {
        return estimatedReadyAt;
    }

    public void setEstimatedReadyAt(LocalDateTime estimatedReadyAt) {
        this.estimatedReadyAt = estimatedReadyAt;
    }

    /** Epoch millis for the JS countdown timer — JSTL/EL can't format java.time types, and JS needs a plain number anyway. */
    public Long getEstimatedReadyAtEpochMillis() {
        return estimatedReadyAt == null ? null : estimatedReadyAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public Integer getRefundedBy() {
        return refundedBy;
    }

    public void setRefundedBy(Integer refundedBy) {
        this.refundedBy = refundedBy;
    }

    /** Drives the "Đã hoàn tiền" badge and hides the refund button on an order already settled. */
    public boolean isRefunded() {
        return refundedAt != null;
    }

    public String getRefundedAtDisplay() {
        return refundedAt == null ? "" : refundedAt.format(DISPLAY_FORMAT);
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

    /** "5 phút" / "1 giờ 20 phút" since creation — for the live-feeling Kanban board (sales/order-queue.jsp, store/ship-queue.jsp). Computed fresh on each call, not cached. */
    public String getElapsedDisplay() {
        if (createdAt == null) {
            return "";
        }
        long minutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        if (minutes < 1) {
            return "vừa xong";
        }
        if (minutes < 60) {
            return minutes + " phút";
        }
        return (minutes / 60) + " giờ " + (minutes % 60) + " phút";
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
