package com.eaut.canteen.filter;

import java.io.IOException;

import com.eaut.canteen.model.Role;
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
 */
@WebFilter("/*")
public class SecurityFilter implements Filter {

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

        if (isRoleMismatch(path, "/admin", user.getRole(), Role.ADMIN)
                || isRoleMismatch(path, "/sales", user.getRole(), Role.SALES_STAFF)
                || isRoleMismatch(path, "/store", user.getRole(), Role.STORE_STAFF)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isRoleMismatch(String path, String prefix, Role actual, Role required) {
        return isUnderPrefix(path, prefix) && actual != required;
    }

    private boolean isPublic(String path) {
        return path.isEmpty() || path.equals("/")
                || path.equals("/login") || path.equals("/auth/google") || path.equals("/logout")
                || path.equals("/manifest.json") || path.equals("/sw.js")
                || isUnderPrefix(path, "/products")
                || isUnderPrefix(path, "/assets")
                || isUnderPrefix(path, "/images");
    }

    private boolean isUnderPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
