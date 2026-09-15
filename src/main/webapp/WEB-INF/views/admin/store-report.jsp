<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Quản lý cửa hàng</h1>

        <form class="catalog-toolbar" method="get" action="${ctx}/admin/store-report">
            <span class="catalog-count">Thống kê từ đơn đã <strong>hoàn thành</strong>.</span>
            <span class="catalog-sort">
                <label for="days">Khoảng thời gian</label>
                <select id="days" name="days" onchange="this.form.submit()">
                    <option value="7" ${days == 7 ? 'selected' : ''}>7 ngày</option>
                    <option value="30" ${days == 30 ? 'selected' : ''}>30 ngày</option>
                    <option value="90" ${days == 90 ? 'selected' : ''}>90 ngày</option>
                    <option value="365" ${days == 365 ? 'selected' : ''}>1 năm</option>
                </select>
                <noscript><button type="submit" class="btn btn-secondary btn-sm">Áp dụng</button></noscript>
            </span>
        </form>

        <h2 style="font-size:1rem;">Sản phẩm bán chạy (${days} ngày)</h2>
        <c:choose>
            <c:when test="${empty bestSellers}">
                <div class="empty-state">
                    <h2>Chưa có dữ liệu bán hàng</h2>
                    <p>Chưa có đơn nào hoàn thành trong ${days} ngày qua.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-scroll">
                    <table class="data-table">
                    <thead>
                        <tr><th>#</th><th>Sản phẩm</th><th>Danh mục</th><th>Giá</th><th>Đã bán</th><th>Tồn kệ</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="p" items="${bestSellers}" varStatus="loop">
                            <tr>
                                <td>${loop.index + 1}</td>
                                <td><c:out value="${p.name}" /></td>
                                <td><c:out value="${p.categoryName}" /></td>
                                <td><fmt:formatNumber value="${p.price}" type="number" groupingUsed="true" />₫</td>
                                <td><strong>${p.soldQuantity}</strong></td>
                                <td>${p.shelfQuantity}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                </div>
                <p class="hint" style="margin-top:8px;">
                    "Đã bán" là tổng số lượng bán được từ trước tới nay; thứ tự xếp hạng tính theo
                    lượng bán trong ${days} ngày gần đây.
                </p>
            </c:otherwise>
        </c:choose>

        <h2 style="font-size:1rem; margin-top:24px;">Sắp hết hàng trên kệ (dưới ${lowStockThreshold})</h2>
        <c:choose>
            <c:when test="${empty lowStock}">
                <div class="empty-state"><h2>Không có sản phẩm nào sắp hết</h2></div>
            </c:when>
            <c:otherwise>
                <div class="table-scroll">
                    <table class="data-table">
                    <thead><tr><th>Sản phẩm</th><th>Tồn kệ</th><th>Tồn kho</th></tr></thead>
                    <tbody>
                        <c:forEach var="p" items="${lowStock}">
                            <tr>
                                <td><c:out value="${p.name}" /></td>
                                <td><span class="badge badge-cancelled">${p.shelfQuantity}</span></td>
                                <td>${p.warehouseQuantity}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                </div>
            </c:otherwise>
        </c:choose>

        <h2 style="font-size:1rem; margin-top:24px;">Món được yêu thích nhiều nhất</h2>
        <c:choose>
            <c:when test="${empty mostFavorited}">
                <div class="empty-state"><h2>Chưa có ai lưu món yêu thích</h2></div>
            </c:when>
            <c:otherwise>
                <div class="table-scroll">
                    <table class="data-table">
                    <thead><tr><th>#</th><th>Sản phẩm</th><th>Lượt yêu thích</th></tr></thead>
                    <tbody>
                        <c:forEach var="f" items="${mostFavorited}" varStatus="loop">
                            <tr>
                                <td>${loop.index + 1}</td>
                                <td><c:out value="${f.productName}" /></td>
                                <td><strong>${f.favoriteCount}</strong></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
