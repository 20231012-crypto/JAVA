package com.eaut.canteen.model;

import java.time.LocalDateTime;

/**
 * The set of conditions the admin order list filters on. Every field is optional — null means
 * "don't filter on this" — so one query serves the unfiltered list and every combination of
 * filters without a separate method per permutation.
 *
 * <p>A record rather than six parameters because the same set has to travel through
 * {@code findFiltered} and {@code countFiltered} in lockstep: the paged rows and the total count
 * must agree, and a mismatched argument list between the two is exactly how a pager starts lying
 * about how many pages there are.
 *
 * @param status        exact order status, or null for any
 * @param from          inclusive lower bound on created_at, already converted to the storage zone
 *                      by {@link com.eaut.canteen.util.AppClock}
 * @param to            exclusive upper bound on created_at, same conversion
 * @param paymentMethod exact payment method, or null for any
 * @param channel       ONLINE / COUNTER, or null for any
 * @param query         free text matched against the order code, and the customer's name, phone
 *                      and student id
 */
public record OrderFilter(
        OrderStatus status,
        LocalDateTime from,
        LocalDateTime to,
        PaymentMethod paymentMethod,
        OrderChannel channel,
        String query) {

    /** Everything, unfiltered — the list's default view. */
    public static OrderFilter none() {
        return new OrderFilter(null, null, null, null, null, null);
    }

    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }

    /** True when any condition is set, so the UI can offer "xóa bộ lọc" only when it would do something. */
    public boolean isActive() {
        return status != null || from != null || to != null
                || paymentMethod != null || channel != null || hasQuery();
    }
}
