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
                </tr>
            </thead>
            <tbody>
                <c:forEach var="p" items="${products}">
                    <tr>
                        <td><c:out value="${p.name}" /></td>
                        <td><c:out value="${p.categoryName}" /></td>
                        <td><fmt:formatNumber value="${p.price}" type="number" groupingUsed="true" />₫</td>
                        <td>${p.warehouseQuantity}</td>
                        <td>${p.shelfQuantity}</td>
                        <td>
                            <c:choose>
                                <c:when test="${p.active}"><span class="badge badge-completed">Đang bán</span></c:when>
                                <c:otherwise><span class="badge badge-cancelled">Đã ẩn</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="table-actions">
                            <a class="btn btn-sm btn-secondary" href="${pageContext.request.contextPath}/admin/products/form?id=${p.productId}">Sửa</a>
                            <form method="post" action="${pageContext.request.contextPath}/admin/products/toggle">
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
