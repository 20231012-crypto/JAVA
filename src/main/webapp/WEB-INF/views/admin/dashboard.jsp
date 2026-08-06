<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Tổng quan quản lý</h1>

        <div class="stat-grid" style="margin-top:20px;">
            <div class="stat-tile">
                <div class="stat-label">Sản phẩm</div>
                <div class="stat-value">${productCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Sắp hết hàng trên kệ (&lt; 5)</div>
                <div class="stat-value">${lowStockCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Danh mục</div>
                <div class="stat-value">${categoryCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Tòa nhà</div>
                <div class="stat-value">${buildingCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Nhân viên</div>
                <div class="stat-value">${staffCount}</div>
            </div>
        </div>

        <div class="filter-bar">
            <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admin/products">Quản lý sản phẩm</a>
            <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admin/categories">Quản lý danh mục</a>
            <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admin/buildings">Tòa nhà & phí ship</a>
            <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admin/stock-imports">Nhập hàng</a>
            <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admin/staff">Tài khoản nhân viên</a>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
