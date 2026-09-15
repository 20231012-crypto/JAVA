<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- Vertical rail for /admin/* only; every other role keeps the shared top nav.

     Collapsed to an icon strip and expanded on hover OR focus-within — focus matters, otherwise
     the whole admin menu is unreachable by keyboard. On touch screens hover never fires, so the
     rail turns into a horizontal scrolling strip instead (see the media query in style.css).

     Each entry is permission-gated the same way the top nav is, so a role without
     products.manage simply does not see that row. --%>
<%-- requestPath is the real route (set in header.jsp); servletPath here would be this
     include's own file path. --%>
<c:set var="path" value="${requestPath}" />
<nav class="admin-rail" aria-label="Điều hướng quản trị">
    <ul class="admin-rail-list">
        <c:if test="${sessionScope.user.permissions['admin.dashboard']}">
            <li>
                <a class="admin-rail-link ${path eq '/admin' ? 'is-active' : ''}" href="${ctx}/admin">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-dashboard"/></svg></span>
                    <span class="admin-rail-label">Tổng quan</span>
                </a>
            </li>
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
        <c:if test="${sessionScope.user.permissions['reports.view']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/reports') ? 'is-active' : ''}"
                   href="${ctx}/admin/reports">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-trending-up"/></svg></span>
                    <span class="admin-rail-label">Thống kê doanh thu</span>
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
        <c:if test="${sessionScope.user.permissions['reports.view']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/store-report') ? 'is-active' : ''}"
                   href="${ctx}/admin/store-report">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-store"/></svg></span>
                    <span class="admin-rail-label">Quản lý cửa hàng</span>
                </a>
            </li>
        </c:if>

        <li class="admin-rail-divider" role="presentation"></li>

        <c:if test="${sessionScope.user.permissions['stock.import']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/stock-imports') ? 'is-active' : ''}"
                   href="${ctx}/admin/stock-imports">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-inbox-in"/></svg></span>
                    <span class="admin-rail-label">Nhập hàng vào kho</span>
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
        <c:if test="${sessionScope.user.permissions['banners.manage']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/banners') ? 'is-active' : ''}"
                   href="${ctx}/admin/banners">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-image"/></svg></span>
                    <span class="admin-rail-label">Banner trang chủ</span>
                </a>
            </li>
        </c:if>
        <c:if test="${sessionScope.user.permissions['wallet.topup']}">
            <li>
                <a class="admin-rail-link ${fn:startsWith(path, '/admin/wallet') ? 'is-active' : ''}"
                   href="${ctx}/admin/wallet">
                    <span class="admin-rail-icon" aria-hidden="true"><svg class="icon" aria-hidden="true"><use href="#i-wallet"/></svg></span>
                    <span class="admin-rail-label">Ví EAUT Pay</span>
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
    </ul>
</nav>
