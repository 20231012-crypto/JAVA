<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- On /admin/* the vertical rail carries every admin destination, so the top nav drops those
     links rather than showing each one twice. Sales/store links stay, because the rail has none. --%>
<c:set var="adminRailVisible" value="${fn:startsWith(requestPath, '/admin')}" />
<header class="site-header">
    <a class="logo" href="${ctx}/products">
        <img src="${ctx}/assets/images/brand/eaut-logo.jpg" alt="EAUT" style="height:32px; vertical-align:middle; margin-right:8px;"
             onerror="this.style.display='none'">
        Căng tin EAUT
    </a>

    <%-- Plain GET form: the results are just the menu page with a ?q=, so the search is
         bookmarkable, shareable and works with the back button. --%>
    <form class="nav-search" method="get" action="${ctx}/products" role="search">
        <input type="search" name="q" value="<c:out value='${searchQuery}'/>"
               placeholder="Tìm món ăn, đồ uống..." aria-label="Tìm món trong thực đơn">
        <button type="submit" class="nav-search-btn" aria-label="Tìm kiếm">🔍</button>
    </form>

    <nav class="site-nav">
        <c:choose>
            <%-- Guest or logged-in customer: same shopper-facing menu either way. --%>
            <c:when test="${empty sessionScope.user or sessionScope.user.role.customerDefault}">
                <div class="mega-menu-trigger">
                    <button type="button" class="mega-menu-btn" data-mega-menu-toggle>Danh mục sản phẩm ▾</button>
                    <div class="mega-menu-panel" id="mega-menu-panel" hidden></div>
                </div>
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
                <c:if test="${not adminRailVisible}">
                    <c:if test="${sessionScope.user.permissions['admin.dashboard']}"><a href="${ctx}/admin">Quản trị</a></c:if>
                </c:if>
                <c:if test="${sessionScope.user.permissions['orders.queue']}"><a href="${ctx}/sales/orders">Đơn hàng</a></c:if>
                <c:if test="${sessionScope.user.permissions['sales.counter']}"><a href="${ctx}/sales/counter-sale">Bán tại quầy</a></c:if>
                <c:if test="${sessionScope.user.permissions['store.transfer']}"><a href="${ctx}/store/transfers">Chuyển hàng lên kệ</a></c:if>
                <c:if test="${sessionScope.user.permissions['store.fulfillment']}"><a href="${ctx}/store/orders">Đơn cần giao</a></c:if>
                <a href="${ctx}/attendance">Chấm công</a>
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

<%-- Admin screens also get the vertical rail. Included here rather than in each of the eleven
     admin JSPs so a new /admin/* page picks it up by existing. --%>
<c:if test="${adminRailVisible}">
    <jsp:include page="/WEB-INF/views/common/admin-sidebar.jsp" />
</c:if>
