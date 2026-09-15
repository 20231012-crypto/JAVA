package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.Product;

public interface ProductDAO {

    List<Product> findAllActive(Connection conn) throws SQLException;

    List<Product> findAllActiveByCategory(Connection conn, int categoryId) throws SQLException;

    /**
     * One page of the customer menu. Any argument may be left out: a null/blank query means no
     * name filter, a null categoryId means every category, and an unrecognised sort falls back to
     * ordering by name. Name matching ignores both case and Vietnamese diacritics, so "banh mi"
     * finds "Bánh mì".
     */
    List<Product> search(Connection conn, String query, Integer categoryId, String sort,
            int limit, int offset) throws SQLException;

    /** Total matches for the same filters as {@link #search}, for the page count. */
    int countSearch(Connection conn, String query, Integer categoryId) throws SQLException;

    /**
     * Best sellers over the last {@code days}, counted from COMPLETED orders only — the same
     * definition the "Đã bán" progress bar uses, so the two never disagree.
     */
    List<Product> findBestSellers(Connection conn, int days, int limit) throws SQLException;

    /** Products whose original_price is set and higher than the current price, biggest saving first. */
    List<Product> findOnPromo(Connection conn, int limit) throws SQLException;

    /** This customer's favorited products, newest first — the "Món yêu thích" section. */
    List<Product> findFavoritesByUser(Connection conn, int userId, int limit) throws SQLException;

    Product findById(Connection conn, int productId) throws SQLException;

    /** All products regardless of active status, with both warehouse and shelf quantities — admin view. */
    List<Product> findAllForAdmin(Connection conn) throws SQLException;

    /** Inserts the product and its 1:1 warehouse_stock/shelf_stock rows (quantity 0). */
    void insert(Connection conn, Product product) throws SQLException;

    void update(Connection conn, Product product) throws SQLException;

    void updateImage(Connection conn, int productId, String imageFilename) throws SQLException;

    void setActive(Connection conn, int productId, boolean active) throws SQLException;

    /**
     * The manual sold-out switch. Distinct from {@link #setActive}: this keeps the dish on the menu
     * and greys it out, rather than hiding it.
     */
    void setAvailable(Connection conn, int productId, boolean available) throws SQLException;

    /** Per-dish low-stock warning level. */
    void updateLowStockThreshold(Connection conn, int productId, int threshold) throws SQLException;

    /**
     * Dishes whose shelf count has fallen to their own threshold, worst first. Replaces filtering
     * the whole product list in Java against a hardcoded 5 — which was written twice, in
     * StoreReportServlet and AdminDashboardServlet, with no reference between them.
     */
    List<Product> findLowStock(Connection conn) throws SQLException;
}
