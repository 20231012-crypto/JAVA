<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<header class="site-header">
    <a class="logo" href="${ctx}/products">Căng tin EAUT</a>

    <nav class="site-nav">
        <c:choose>
            <c:when test="${empty sessionScope.user}">
                <a href="${ctx}/products">Thực đơn</a>
            </c:when>
            <c:when test="${sessionScope.user.role == 'ADMIN'}">
                <a href="${ctx}/admin">Tổng quan</a>
                <a href="${ctx}/admin/categories">Danh mục</a>
                <a href="${ctx}/admin/products">Sản phẩm</a>
                <a href="${ctx}/admin/buildings">Tòa nhà</a>
                <a href="${ctx}/admin/stock-imports">Nhập hàng</a>
                <a href="${ctx}/admin/staff">Nhân viên</a>
            </c:when>
            <c:when test="${sessionScope.user.role == 'SALES_STAFF'}">
                <a href="${ctx}/sales/orders">Đơn hàng</a>
                <a href="${ctx}/sales/counter-sale">Bán tại quầy</a>
            </c:when>
            <c:when test="${sessionScope.user.role == 'STORE_STAFF'}">
                <a href="${ctx}/store/transfers">Chuyển hàng lên kệ</a>
                <a href="${ctx}/store/orders">Đơn cần giao</a>
            </c:when>
            <c:otherwise>
                <a href="${ctx}/products">Thực đơn</a>
                <a href="${ctx}/cart">Giỏ hàng</a>
                <a href="${ctx}/orders">Đơn hàng của tôi</a>
            </c:otherwise>
        </c:choose>
    </nav>

    <c:choose>
        <c:when test="${empty sessionScope.user}">
            <div class="site-nav">
                <a href="${ctx}/login">Đăng nhập</a>
                <a class="btn btn-primary btn-sm" href="${ctx}/register">Đăng ký</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="user-info">
                <span class="avatar"><c:out value="${fn:substring(sessionScope.user.fullName, 0, 1)}" /></span>
                <span><c:out value="${sessionScope.user.fullName}" /></span>
                <a href="${ctx}/logout">Đăng xuất</a>
            </div>
        </c:otherwise>
    </c:choose>
</header>
