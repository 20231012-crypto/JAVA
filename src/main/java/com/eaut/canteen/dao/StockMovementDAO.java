package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.eaut.canteen.model.StockLocation;
import com.eaut.canteen.model.StockMovement;
import com.eaut.canteen.model.StockMovementReason;

/**
 * The stock ledger. Every write that changes warehouse_stock or shelf_stock must also record a
 * movement here, in the SAME transaction as the change — a ledger that can be half-written is
 * worse than none, because it looks authoritative while being wrong.
 */
public interface StockMovementDAO {

    /**
     * Records one movement. {@code delta} is signed and must not be zero (the database rejects it):
     * a movement that changes nothing explains nothing.
     */
    void insert(Connection conn, int productId, StockLocation location, int delta,
                StockMovementReason reason, Integer refOrderId, String note, int createdBy) throws SQLException;

    /**
     * Filtered page of the ledger, newest first. Null arguments mean "any", so the same method
     * serves the product's own history and the site-wide recent-activity view.
     */
    List<StockMovement> findFiltered(Connection conn, Integer productId, StockMovementReason reason,
                                     LocalDateTime from, LocalDateTime to, int limit, int offset) throws SQLException;

    /** Total rows {@link #findFiltered} would return for the same arguments, ignoring paging. */
    int countFiltered(Connection conn, Integer productId, StockMovementReason reason,
                      LocalDateTime from, LocalDateTime to) throws SQLException;
}
