package com.eaut.canteen.filter;

import java.io.IOException;
import java.util.List;

import com.eaut.canteen.model.User;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Single merged auth+RBAC gate. Two separate filters would need a guaranteed execution
 * order (a role check assuming an auth check already ran) which the servlet spec does not
 * promise for annotation-declared filters — so both checks live in one filter instead.
 *
 * <p>Route -&gt; required-permission is a static table, not a fixed role, so which actual roles
 * can reach a route is entirely up to what's assigned in /admin/roles — a brand-new role with the
 * right permission gets in with zero code changes here. Rules are checked most-specific-first
 * (see RULES) since some sub-paths need a narrower permission than the rest of their prefix
 * (e.g. "/sales/orders/confirm" is stricter than plain "/sales/orders").
 */
@WebFilter("/*")
public class SecurityFilter implements Filter {

    private record PathRule(String prefix, String permission) {
    }

    private static final List<PathRule> RULES = List.of(
            new PathRule("/admin/buildings", "buildings.manage"),
            new PathRule("/admin/banners", "banners.manage"),
            new PathRule("/admin/categories", "categories.manage"),
            new PathRule("/admin/products", "products.manage"),
            new PathRule("/admin/staff", "staff.manage"),
            new PathRule("/admin/roles", "roles.manage"),
            new PathRule("/admin/stock-imports", "stock.import"),
            new PathRule("/admin/wallet", "wallet.topup"),
            new PathRule("/admin/reports", "reports.view"),
            new PathRule("/admin", "admin.dashboard"),
            new PathRule("/sales/orders/confirm", "orders.action"),
            new PathRule("/sales/orders/reject", "orders.action"),
            new PathRule("/sales/orders/cancel", "orders.action"),
            new PathRule("/sales/orders/mark-paid", "orders.payment_confirm"),
            new PathRule("/sales/orders", "orders.queue"),
            new PathRule("/sales/counter-sale", "sales.counter"),
            new PathRule("/sales/shop-status", "shop.status"),
            new PathRule("/store/transfers", "store.transfer"),
            new PathRule("/store/orders", "store.fulfillment"));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getServletPath();

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login?redirect=" + path);
            return;
        }

        String requiredPermission = resolveRequiredPermission(path);
        if (requiredPermission != null && !user.hasPermission(requiredPermission)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    /** null means "no admin/sales/store rule applies" — e.g. /cart, /checkout, /orders, which only require being logged in. */
    private String resolveRequiredPermission(String path) {
        for (PathRule rule : RULES) {
            if (isUnderPrefix(path, rule.prefix())) {
                return rule.permission();
            }
        }
        return null;
    }

    private boolean isPublic(String path) {
        return path.isEmpty() || path.equals("/")
                || path.equals("/login") || path.equals("/auth/google") || path.equals("/logout")
                || path.equals("/manifest.json") || path.equals("/sw.js")
                || isUnderPrefix(path, "/products")
                || isUnderPrefix(path, "/assets")
                || isUnderPrefix(path, "/images")
                || path.equals("/banner-image");
    }

    private boolean isUnderPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
