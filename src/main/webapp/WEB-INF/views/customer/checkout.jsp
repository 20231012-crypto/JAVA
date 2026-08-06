<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Thanh toán</h1>

        <c:if test="${not empty error}">
            <div class="alert alert-error"><c:out value="${error}" /></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/checkout/place">
            <div class="checkout-layout">
                <div>
                    <div class="form-group">
                        <label for="buildingId">Giao đến tòa nhà</label>
                        <select id="buildingId" name="buildingId" required>
                            <option value="">-- Chọn tòa nhà --</option>
                            <c:forEach var="b" items="${buildings}">
                                <option value="${b.buildingId}">
                                    <c:out value="${b.name}" /> (phí ship <fmt:formatNumber value="${b.shippingFee}" type="number" groupingUsed="true" />₫)
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="note">Ghi chú (số phòng, tầng...)</label>
                        <textarea id="note" name="note" rows="3"></textarea>
                    </div>
                    <div class="form-group">
                        <label>Phương thức thanh toán</label>
                        <p style="color:var(--color-text-muted);">Thanh toán khi nhận hàng (COD)</p>
                    </div>
                </div>

                <div class="order-summary">
                    <h2 style="margin-bottom:12px;">Đơn hàng của bạn</h2>
                    <c:forEach var="item" items="${cart.items}">
                        <div class="summary-row">
                            <span><c:out value="${item.productName}" /> x${item.quantity}</span>
                            <span><fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true" />₫</span>
                        </div>
                    </c:forEach>
                    <div class="summary-row summary-total">
                        <span>Tạm tính</span>
                        <span><fmt:formatNumber value="${cart.subtotal}" type="number" groupingUsed="true" />₫</span>
                    </div>
                    <p style="color:var(--color-text-muted); font-size:0.85rem; margin-top:8px;">
                        Phí ship sẽ được cộng theo tòa nhà bạn chọn.
                    </p>
                    <button type="submit" class="btn btn-primary" style="width:100%; margin-top:16px;">Đặt hàng</button>
                </div>
            </div>
        </form>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
