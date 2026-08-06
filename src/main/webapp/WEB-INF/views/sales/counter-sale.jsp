<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Bán hàng tại quầy</h1>

        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <div class="checkout-layout" style="margin-top:20px;">
            <div>
                <h2 style="margin-bottom:12px;">Chọn sản phẩm</h2>
                <div class="card-grid">
                    <c:forEach var="p" items="${products}">
                        <div class="card product-card">
                            <c:choose>
                                <c:when test="${not empty p.imageFilename}">
                                    <img src="${pageContext.request.contextPath}/images/${p.imageFilename}" alt="${p.name}">
                                </c:when>
                                <c:otherwise>
                                    <div class="product-image-placeholder"><c:out value="${p.name}" /></div>
                                </c:otherwise>
                            </c:choose>
                            <div class="card-body">
                                <div class="product-name"><c:out value="${p.name}" /></div>
                                <div class="product-price"><fmt:formatNumber value="${p.price}" type="number" groupingUsed="true" />₫</div>
                                <div style="color:var(--color-text-muted); font-size:0.85rem; margin-bottom:8px;">Tồn kệ: ${p.shelfQuantity}</div>
                                <form method="post" action="${pageContext.request.contextPath}/sales/counter-sale/add">
                                    <input type="hidden" name="productId" value="${p.productId}">
                                    <input type="hidden" name="quantity" value="1">
                                    <button type="submit" class="btn btn-secondary btn-sm" ${p.shelfQuantity <= 0 ? 'disabled' : ''}>Thêm</button>
                                </form>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>

            <div class="order-summary">
                <h2 style="margin-bottom:12px;">Hóa đơn</h2>
                <c:choose>
                    <c:when test="${empty cart.items}">
                        <p style="color:var(--color-text-muted);">Chưa chọn sản phẩm nào.</p>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="item" items="${cart.items}">
                            <div class="summary-row">
                                <span><c:out value="${item.productName}" /> x${item.quantity}</span>
                                <span>
                                    <fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true" />₫
                                    <form method="post" action="${pageContext.request.contextPath}/sales/counter-sale/remove" style="display:inline;">
                                        <input type="hidden" name="productId" value="${item.productId}">
                                        <button type="submit" class="btn btn-sm btn-danger">Xóa</button>
                                    </form>
                                </span>
                            </div>
                        </c:forEach>
                        <div class="summary-row summary-total">
                            <span>Tổng cộng</span>
                            <span><fmt:formatNumber value="${cart.subtotal}" type="number" groupingUsed="true" />₫</span>
                        </div>

                        <form method="post" action="${pageContext.request.contextPath}/sales/counter-sale/complete" style="margin-top:16px;">
                            <div class="form-group">
                                <label for="paymentMethod">Hình thức thanh toán</label>
                                <select id="paymentMethod" name="paymentMethod">
                                    <option value="CASH">Tiền mặt</option>
                                </select>
                            </div>
                            <button type="submit" class="btn btn-primary" style="width:100%;">Hoàn tất thanh toán</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
