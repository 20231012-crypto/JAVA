<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Giỏ hàng</h1>

        <c:choose>
            <c:when test="${empty cart.items}">
                <div class="empty-state">
                    <h2>Giỏ hàng đang trống</h2>
                    <p>Hãy chọn vài món ngon từ thực đơn nhé.</p>
                    <p style="margin-top:16px;">
                        <a class="btn btn-primary" href="${pageContext.request.contextPath}/products">Xem thực đơn</a>
                    </p>
                </div>
            </c:when>
            <c:otherwise>
                <table class="data-table cart-table" style="margin-top:20px;">
                    <thead>
                        <tr>
                            <th>Sản phẩm</th>
                            <th>Đơn giá</th>
                            <th>Số lượng</th>
                            <th>Thành tiền</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="item" items="${cart.items}">
                            <tr>
                                <td><c:out value="${item.productName}" /></td>
                                <td><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true" />₫</td>
                                <td>
                                    <form method="post" action="${pageContext.request.contextPath}/cart/update" class="qty-form">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="productId" value="${item.productId}">
                                        <input type="number" name="quantity" value="${item.quantity}" min="1"
                                               onchange="this.form.submit()">
                                    </form>
                                </td>
                                <td><fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true" />₫</td>
                                <td>
                                    <form method="post" action="${pageContext.request.contextPath}/cart/remove">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="productId" value="${item.productId}">
                                        <button type="submit" class="btn btn-danger btn-sm">Xóa</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>

                <div class="order-summary" style="max-width:360px; margin:24px 0 0 auto;">
                    <div class="summary-row summary-total">
                        <span>Tạm tính</span>
                        <span><fmt:formatNumber value="${cart.subtotal}" type="number" groupingUsed="true" />₫</span>
                    </div>
                </div>
                <div style="text-align:right; margin-top:16px;">
                    <a class="btn btn-primary" href="${pageContext.request.contextPath}/checkout">Tiến hành thanh toán</a>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
