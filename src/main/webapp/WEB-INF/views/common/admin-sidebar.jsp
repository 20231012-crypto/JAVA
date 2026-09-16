<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- Vertical rail for /admin/* only; every other role keeps the shared top nav.

     Expanded by default, collapsible by the button at the top, and the choice is remembered in
     localStorage (admin.js). It used to be a 64px icon strip that expanded on hover — that stopped
     working once the menu passed a dozen entries, because sixteen indistinguishable glyphs in a
     column is not navigation. Hover also never fires on a touch screen, so below 768px the rail
     becomes a horizontal scrolling strip (see the media query in style.css).

     Entries are grouped, and every entry is gated by the permission of the screen it opens — so
     SALES_STAFF and STORE_STAFF see only their own sections, and an empty group renders nothing
     because its heading is inside the same c:if as its links. --%>
<c:set var="path" value="${requestPath}" />
<nav class="admin-rail" aria-label="Điều hướng quản trị">
    <button type="button" class="admin-rail-toggle" data-rail-toggle
            aria-expanded="true" aria-label="Thu gọn / mở rộng menu quản trị">
        <svg class="icon" aria-hidden="true"><use href="#i-dashboard"/></svg>
        <span class="admin-rail-label">Thu gọn</span>
    </button>

    <ul class="admin-rail-list">
        <c:if test="${sessionScope.user.permissions['admin.dashboard']}">
            <li class="admin-rail-group">Tổng quan</li>
            <li>
                <a class="admin-rail-link ${path eq '/admin' ? 'is-active' : ''}" href="${ctx}/admin">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-dashboard"/></svg></span>
                    <span class="admin-rail-label">Bảng điều khiển</span>
                </a>
            </li>
        </c:if>

        <c:if test="${sessionScope.user.permissions['orders.manage']
                      or sessionScope.user.permissions['reports.view']}">
            <li class="admin-rail-group">Kinh doanh</li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['orders.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/orders') ? 'is-active' : ''}"
                   href="${ctx}/admin/orders">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-receipt"/></svg></span>
                    <span class="admin-rail-label">Quản lý đơn hàng</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['reports.view']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/reports') ? 'is-active' : ''}"
                   href="${ctx}/admin/reports">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-trending-up"/></svg></span>
                    <span class="admin-rail-label">Thống kê doanh thu</span>
                </a>
            </li>
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/store-report') ? 'is-active' : ''}"
                   href="${ctx}/admin/store-report">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-store"/></svg></span>
                    <span class="admin-rail-label">Quản lý cửa hàng</span>
                </a>
            </li>
        </c:if>

        <c:if test="${sessionScope.user.permissions['products.manage']
                      or sessionScope.user.permissions['categories.manage']
                      or sessionScope.user.permissions['stock.adjust']
                      or sessionScope.user.permissions['stock.import']}">
            <li class="admin-rail-group">Sản phẩm &amp; kho</li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['products.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/products') ? 'is-active' : ''}"
                   href="${ctx}/admin/products">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-utensils"/></svg></span>
                    <span class="admin-rail-label">Quản lý sản phẩm</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['categories.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/categories') ? 'is-active' : ''}"
                   href="${ctx}/admin/categories">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-folders"/></svg></span>
                    <span class="admin-rail-label">Danh mục sản phẩm</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['stock.adjust']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/inventory') ? 'is-active' : ''}"
                   href="${ctx}/admin/inventory">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-boxes"/></svg></span>
                    <span class="admin-rail-label">Tồn kho &amp; sổ kho</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['stock.import']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/stock-imports') ? 'is-active' : ''}"
                   href="${ctx}/admin/stock-imports">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-inbox-in"/></svg></span>
                    <span class="admin-rail-label">Nhập hàng vào kho</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['suppliers.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/suppliers') ? 'is-active' : ''}"
                   href="${ctx}/admin/suppliers">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-truck"/></svg></span>
                    <span class="admin-rail-label">Nhà cung cấp</span>
                </a>
            </li>
        </c:if>

        <c:if test="${sessionScope.user.permissions['customers.manage']
                      or sessionScope.user.permissions['staff.manage']
                      or sessionScope.user.permissions['attendance.view']
                      or sessionScope.user.permissions['roles.manage']}">
            <li class="admin-rail-group">Người dùng</li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['customers.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/customers') ? 'is-active' : ''}"
                   href="${ctx}/admin/customers">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-users"/></svg></span>
                    <span class="admin-rail-label">Quản lý khách hàng</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['staff.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/staff') ? 'is-active' : ''}"
                   href="${ctx}/admin/staff">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-briefcase"/></svg></span>
                    <span class="admin-rail-label">Quản lý nhân viên</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['attendance.view']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/attendance') ? 'is-active' : ''}"
                   href="${ctx}/admin/attendance">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-clock"/></svg></span>
                    <span class="admin-rail-label">Chấm công nhân viên</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['roles.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/roles') ? 'is-active' : ''}"
                   href="${ctx}/admin/roles">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-key"/></svg></span>
                    <span class="admin-rail-label">Vai trò &amp; phân quyền</span>
                </a>
            </li>
        </c:if>

        <c:if test="${sessionScope.user.permissions['wallet.topup']}">
            <li class="admin-rail-group">Tài chính</li>
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/wallet') ? 'is-active' : ''}"
                   href="${ctx}/admin/wallet">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-wallet"/></svg></span>
                    <span class="admin-rail-label">Ví EAUT Pay</span>
                </a>
            </li>
        </c:if>

        <c:if test="${sessionScope.user.permissions['banners.manage']
                      or sessionScope.user.permissions['settings.manage']
                      or sessionScope.user.permissions['buildings.manage']}">
            <li class="admin-rail-group">Giao diện &amp; hệ thống</li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['banners.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/banners') ? 'is-active' : ''}"
                   href="${ctx}/admin/banners">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-image"/></svg></span>
                    <span class="admin-rail-label">Banner trang chủ</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['settings.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/effects') ? 'is-active' : ''}"
                   href="${ctx}/admin/effects">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-sparkles"/></svg></span>
                    <span class="admin-rail-label">Hiệu ứng trang chủ</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['buildings.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/buildings') ? 'is-active' : ''}"
                   href="${ctx}/admin/buildings">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-building"/></svg></span>
                    <span class="admin-rail-label">Tòa nhà &amp; phí ship</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['settings.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/settings') ? 'is-active' : ''}"
                   href="${ctx}/admin/settings">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-sliders"/></svg></span>
                    <span class="admin-rail-label">Cài đặt hệ thống</span>
                </a>
            </li>
        </c:if>
    </ul>
</nav>

<%-- The command palette and the alert badge. Rendered once per admin page here rather than in each
     view, next to the rail they belong with. Hidden until Ctrl+K; with JavaScript off it simply
     never appears, and every screen it reaches is still linked from the rail. --%>
<div class="palette-overlay" id="command-palette" hidden role="dialog" aria-modal="true"
     aria-label="Tìm kiếm nhanh">
    <div class="palette-box">
        <div class="palette-input-row">
            <svg class="icon" aria-hidden="true"><use href="#i-search"/></svg>
            <label class="visually-hidden" for="palette-input">Tìm món, đơn hàng hoặc khách hàng</label>
            <input type="search" id="palette-input" data-palette-input autocomplete="off"
                   placeholder="Tìm món, mã đơn, tên khách...">
            <button type="button" class="btn btn-sm btn-secondary" data-palette-close>Đóng</button>
        </div>
        <div class="palette-results" data-palette-results></div>
        <p class="palette-footnote">Mở nhanh bằng <kbd>Ctrl</kbd> + <kbd>K</kbd>, đóng bằng <kbd>Esc</kbd>.</p>
    </div>
</div>
