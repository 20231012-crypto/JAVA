<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<header class="site-header">
    <a class="logo" href="${ctx}/products">
        <img src="${ctx}/assets/images/brand/eaut-logo.jpg" alt="EAUT" style="height:32px; vertical-align:middle; margin-right:8px;"
             onerror="this.style.display='none'">
        Căng tin EAUT
    </a>

    <nav class="site-nav">
        <c:choose>
            <%-- Guest or logged-in customer: same shopper-facing menu either way. --%>
            <c:when test="${empty sessionScope.user or sessionScope.user.role.customerDefault}">
                <a href="${ctx}/products">Thực đơn</a>
                <c:if test="${not empty sessionScope.user}">
                    <a href="${ctx}/cart" id="nav-cart-link" class="nav-cart-link">
                        Giỏ hàng
                        <span id="cart-badge" class="cart-badge" ${empty sessionScope.cart or sessionScope.cart.totalItemCount == 0 ? 'hidden' : ''}><c:out value="${sessionScope.cart.totalItemCount}" /></span>
                    </a>
                    <a href="${ctx}/orders">Đơn hàng của tôi</a>
                    <a href="${ctx}/wallet">Ví của tôi</a>
                </c:if>
            </c:when>
            <%-- Staff/admin: each link only shows if this user's role actually carries that
                 permission — an admin-created custom role only sees what it was granted. --%>
            <c:otherwise>
                <c:if test="${sessionScope.user.permissions['admin.dashboard']}"><a href="${ctx}/admin">Tổng quan</a></c:if>
                <c:if test="${sessionScope.user.permissions['categories.manage']}"><a href="${ctx}/admin/categories">Danh mục</a></c:if>
                <c:if test="${sessionScope.user.permissions['products.manage']}"><a href="${ctx}/admin/products">Sản phẩm</a></c:if>
                <c:if test="${sessionScope.user.permissions['buildings.manage']}"><a href="${ctx}/admin/buildings">Tòa nhà</a></c:if>
                <c:if test="${sessionScope.user.permissions['stock.import']}"><a href="${ctx}/admin/stock-imports">Nhập hàng</a></c:if>
                <c:if test="${sessionScope.user.permissions['staff.manage']}"><a href="${ctx}/admin/staff">Nhân viên</a></c:if>
                <c:if test="${sessionScope.user.permissions['roles.manage']}"><a href="${ctx}/admin/roles">Vai trò &amp; phân quyền</a></c:if>
                <c:if test="${sessionScope.user.permissions['wallet.topup']}"><a href="${ctx}/admin/wallet">Nạp ví EAUT Pay</a></c:if>
                <c:if test="${sessionScope.user.permissions['orders.queue']}"><a href="${ctx}/sales/orders">Đơn hàng</a></c:if>
                <c:if test="${sessionScope.user.permissions['sales.counter']}"><a href="${ctx}/sales/counter-sale">Bán tại quầy</a></c:if>
                <c:if test="${sessionScope.user.permissions['store.transfer']}"><a href="${ctx}/store/transfers">Chuyển hàng lên kệ</a></c:if>
                <c:if test="${sessionScope.user.permissions['store.fulfillment']}"><a href="${ctx}/store/orders">Đơn cần giao</a></c:if>
            </c:otherwise>
        </c:choose>
    </nav>

    <c:choose>
        <c:when test="${empty sessionScope.user}">
            <div class="site-nav">
                <a class="btn btn-primary btn-sm" href="${ctx}/login">Đăng nhập / Đăng ký</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="user-info">
                <span class="avatar"><c:out value="${fn:substring(sessionScope.user.fullName, 0, 1)}" /></span>
                <span><c:out value="${sessionScope.user.fullName}" /></span>
                <c:if test="${sessionScope.user.role.customerDefault}">
                    <span class="wallet-badge" title="Số dư ví EAUT Pay">💳 <fmt:formatNumber value="${sessionScope.user.walletBalance}" type="number" groupingUsed="true" />đ</span>
                </c:if>
                <a href="${ctx}/logout">Đăng xuất</a>
            </div>
        </c:otherwise>
    </c:choose>
</header>
