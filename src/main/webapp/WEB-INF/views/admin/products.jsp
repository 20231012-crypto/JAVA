<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <div style="display:flex; justify-content:space-between; align-items:center;">
            <h1>Sản phẩm</h1>
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/admin/products/form">Thêm sản phẩm</a>
        </div>

        <table class="data-table" style="margin-top:20px;">
            <thead>
                <tr>
                    <th>Tên sản phẩm</th><th>Danh mục</th><th>Giá</th>
                    <th>Tồn kho</th><th>Tồn kệ</th><th>Trạng thái</th><th></th>
                    <th>Còn hàng</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="p" items="${products}">
                    <tr>
                        <td><c:out value="${p.name}" /></td>
                        <td><c:out value="${p.categoryName}" /></td>
                        <td><fmt:formatNumber value="${p.price}" type="number" groupingUsed="true" />₫</td>
                        <td>${p.warehouseQuantity}</td>
                        <td>
                            ${p.shelfQuantity}
                            <c:if test="${p.active and p.lowStock}">
                                <span class="badge badge-rejected">Sắp hết</span>
                            </c:if>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${p.active}"><span class="badge badge-completed">Đang bán</span></c:when>
                                <c:otherwise><span class="badge badge-cancelled">Đã ẩn</span></c:otherwise>
                            </c:choose>
                        </td>
                        <%-- The quick sold-out switch. Separate from Ẩn/Hiện above: this leaves the
                             dish on the student menu, greyed out, rather than removing it. A plain
                             form so it works without JavaScript like every other admin action. --%>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/admin/products/availability" class="inline-form">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="productId" value="${p.productId}">
                                <input type="hidden" name="available" value="${!p.available}">
                                <button type="submit" class="switch-btn ${p.available ? 'is-on' : ''}"
                                        aria-pressed="${p.available}"
                                        title="${p.available ? 'Đang bán — bấm để báo hết hàng' : 'Đang báo hết — bấm để mở bán lại'}">
                                    <span class="switch-track" aria-hidden="true"><span class="switch-thumb"></span></span>
                                    <span class="switch-btn-text">${p.available ? 'Còn hàng' : 'Hết hàng'}</span>
                                </button>
                            </form>
                        </td>
                        <td class="table-actions">
                            <a class="btn btn-sm btn-secondary" href="${pageContext.request.contextPath}/admin/products/form?id=${p.productId}">Sửa</a>
                            <form method="post" action="${pageContext.request.contextPath}/admin/products/toggle">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="productId" value="${p.productId}">
                                <input type="hidden" name="active" value="${!p.active}">
                                <button type="submit" class="btn btn-sm ${p.active ? 'btn-danger' : 'btn-secondary'}">
                                    ${p.active ? 'Ẩn' : 'Hiện lại'}
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
