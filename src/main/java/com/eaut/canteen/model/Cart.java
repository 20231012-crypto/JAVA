package com.eaut.canteen.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Session-scoped shopping cart — not persisted, no DAO. */
public class Cart implements Serializable {

    private final Map<Integer, CartItem> items = new LinkedHashMap<>();

    public void addOrIncrement(int productId, String productName, BigDecimal unitPrice, String imageFilename, int quantity) {
        CartItem existing = items.get(productId);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            items.put(productId, new CartItem(productId, productName, unitPrice, imageFilename, quantity));
        }
    }

    public void updateQuantity(int productId, int quantity) {
        if (quantity <= 0) {
            items.remove(productId);
            return;
        }
        CartItem item = items.get(productId);
        if (item != null) {
            item.setQuantity(quantity);
        }
    }

    public void removeItem(int productId) {
        items.remove(productId);
    }

    public Collection<CartItem> getItems() {
        return items.values();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getTotalItemCount() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal getSubtotal() {
        return items.values().stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void clear() {
        items.clear();
    }
}
