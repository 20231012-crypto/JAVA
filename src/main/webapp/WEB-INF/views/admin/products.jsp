<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <c:if test="${not empty sessionScope.actionMessage}">
            <div class="alert alert-success"><c:out value="${sessionScope.actionMessage}" /></div>
            <c:remove var="actionMessage" scope="session" />
        </c:if>
        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>
        <div style="display:flex; justify-content:space-between; align-items:center;">
            <h1>Sản phẩm</h1>
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/admin/products/form">Thêm sản phẩm</a>
        </div>

                <%-- The whole table is one form. Bulk selection is therefore plain checkboxes and a
             submit button — it works with JavaScript off, and admin.js only adds the select-all
             convenience on top. --%>
        <form method="post" action="${pageContext.request.contextPath}/admin/products/bulk">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <div class="bulk-bar">
                <span class="bulk-bar-label">Thao tác cho các món đã chọn:</span>
                <button type="submit" name="action" value="sold-out" class="btn btn-sm btn-secondary">Báo hết hàng</button>
                <button type="submit" name="action" value="available" class="btn btn-sm btn-secondary">Mở bán lại</button>
                <button type="submit" name="action" value="hide" class="btn btn-sm btn-danger">Ẩn khỏi menu</button>
                <button type="submit" name="action" value="show" class="btn btn-sm btn-secondary">Hiện lại</button>
            </div>
<div class="table-scroll">
    <table class="data-table" style="margin-top:20px;">
            <thead>
                <tr>
                    <th class="col-select">
                        <label class="visually-hidden" for="select-all-products">Chọn tất cả</label>
                        <input type="checkbox" id="select-all-products" data-select-all="product-pick">
                    </th>
                    <th>Tên sản phẩm</th><th>Danh mục</th><th>Giá</th>
                    <th>Tồn kho</th><th>Tồn kệ</th><th>Trạng thái</th><th></th>
                    <th>Còn hàng</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="p" items="${products}">
                    <tr>
                        <td class="col-select">
                            <label class="visually-hidden" for="pick-${p.productId}">Chọn <c:out value="${p.name}" /></label>
                            <input type="checkbox" id="pick-${p.productId}" name="ids"
                                   value="${p.productId}" data-select-item="product-pick">
                        </td>
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
        </form>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
