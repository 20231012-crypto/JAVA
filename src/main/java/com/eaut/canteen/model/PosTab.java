package com.eaut.canteen.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One order being rung up at the till.
 *
 * <p>A till needs more than one of these at a time: a student gets to the front, realises they left
 * their card at the table, and the queue behind them should not have to wait. So each tab holds its
 * own cart, customer, note and discount, and the cashier moves between them.
 *
 * <p>Serializable because it lives in the HTTP session, like {@link Cart} before it.
 */
public class PosTab implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String label;
    private final Cart cart = new Cart();

    /** Optional. Attaching a student is what lets a counter sale earn them loyalty points. */
    private Integer customerId;
    private String customerName;

    private String note;
    private BigDecimal discount = BigDecimal.ZERO;

    /**
     * When set, adding the same dish twice produces two lines rather than one line of quantity two.
     * Useful when two people at the same table order the same thing and want separate notes.
     */
    private boolean splitLines;

    /**
     * Custom items — "Sản phẩm tùy chỉnh" — have no row in products, so they get synthetic negative
     * ids. Negative because every real product id is positive, which keeps them from ever colliding
     * and makes "is this a real product?" a sign test rather than a lookup.
     */
    private int nextCustomItemId = -1;

    public PosTab(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Cart getCart() {
        return cart;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomer(Integer customerId, String customerName) {
        this.customerId = customerId;
        this.customerName = customerName;
    }

    public void clearCustomer() {
        this.customerId = null;
        this.customerName = null;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getDiscount() {
        return discount == null ? BigDecimal.ZERO : discount;
    }

    /** Never negative, and never more than the bill — a discount cannot turn into a payout. */
    public void setDiscount(BigDecimal discount) {
        if (discount == null || discount.signum() < 0) {
            this.discount = BigDecimal.ZERO;
            return;
        }
        BigDecimal subtotal = cart.getSubtotal();
        this.discount = discount.min(subtotal);
    }

    public boolean isSplitLines() {
        return splitLines;
    }

    public void setSplitLines(boolean splitLines) {
        this.splitLines = splitLines;
    }

    public int nextCustomItemId() {
        return nextCustomItemId--;
    }

    public BigDecimal getSubtotal() {
        return cart.getSubtotal();
    }

    /**
     * What the customer hands over. The discount is clamped on the way in, so this can never go
     * below zero.
     */
    public BigDecimal getTotal() {
        return cart.getSubtotal().subtract(getDiscount()).max(BigDecimal.ZERO);
    }

    public int getItemCount() {
        return cart.getTotalItemCount();
    }

    public boolean isEmpty() {
        return cart.isEmpty();
    }

    /**
     * Clears everything except the tab's identity, so the cashier can start the next sale on the
     * same tab without closing and reopening it.
     */
    public void reset() {
        cart.clear();
        clearCustomer();
        note = null;
        discount = BigDecimal.ZERO;
        nextCustomItemId = -1;
    }

    /**
     * VAT contained in this sale, given each dish's own rate.
     *
     * <p>Prices are tax-inclusive, so this is the portion of what was already charged, not an
     * addition to it — {@code line x rate / (100 + rate)}. Summed per line rather than over the
     * total because a basket can mix rates, and custom items carry none.
     */
    public BigDecimal taxAmount(java.util.Map<Integer, BigDecimal> ratesByProductId) {
        BigDecimal tax = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal rate = ratesByProductId.get(item.getProductId());
            if (rate == null || rate.signum() <= 0) {
                continue;
            }
            tax = tax.add(item.getLineTotal().multiply(rate)
                    .divide(BigDecimal.valueOf(100).add(rate), 0, RoundingMode.HALF_UP));
        }
        return tax;
    }
}
