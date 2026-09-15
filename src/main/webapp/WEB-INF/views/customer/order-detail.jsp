<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
            <h1>Đơn hàng <c:out value="${order.orderCode}" /></h1>
            <span class="badge badge-${fn:toLowerCase(order.orderStatus)}"><c:out value="${order.orderStatus.displayName}" /></span>
        </div>

        <div class="detail-layout" style="margin-top:20px;">
            <div class="order-summary">
                <div class="summary-row"><span>Đặt lúc</span><span><c:out value="${order.createdAtDisplay}" /></span></div>
                <div class="summary-row"><span>Giao đến</span><span><c:out value="${order.buildingName}" /></span></div>
                <div class="summary-row"><span>Thanh toán</span><span><c:out value="${order.paymentMethod.displayName}" /></span></div>
                <div class="summary-row"><span>Trạng thái thanh toán</span><span><c:out value="${order.paymentStatus.displayName}" /></span></div>
                <c:if test="${not empty order.note}">
                    <div class="summary-row"><span>Ghi chú</span><span><c:out value="${order.note}" /></span></div>
                </c:if>
                <div class="summary-row"><span>Tạm tính</span><span><fmt:formatNumber value="${order.subtotal}" type="number" groupingUsed="true" />₫</span></div>
                <div class="summary-row"><span>Phí ship</span><span><fmt:formatNumber value="${order.shippingFee}" type="number" groupingUsed="true" />₫</span></div>
                <c:if test="${order.discountAmount > 0}">
                    <div class="summary-row" style="color:var(--color-gold);"><span><svg class="icon" aria-hidden="true"><use href="#i-id-card"/></svg> Giảm giá Smart ID</span><span>-<fmt:formatNumber value="${order.discountAmount}" type="number" groupingUsed="true" />₫</span></div>
                </c:if>
                <c:if test="${order.loyaltyPointsUsed > 0}">
                    <div class="summary-row" style="color:var(--color-gold);"><span><svg class="icon" aria-hidden="true"><use href="#i-gift"/></svg> Điểm tích luỹ (${order.loyaltyPointsUsed} điểm)</span><span>-<fmt:formatNumber value="${order.loyaltyDiscountAmount}" type="number" groupingUsed="true" />₫</span></div>
                </c:if>
                <div class="summary-row summary-total"><span>Tổng cộng</span><span><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true" />₫</span></div>

                <c:if test="${not empty qrImageUrl}">
                    <div style="text-align:center; margin-top:20px; padding-top:16px; border-top:1px solid rgba(0,0,0,0.1);">
                        <p style="font-weight:600; margin-bottom:8px;">Quét mã để chuyển khoản</p>
                        <img src="${qrImageUrl}" alt="Mã VietQR" style="width:100%; max-width:220px; border-radius:8px;">
                        <p style="margin-top:8px; font-size:0.9rem;">Nội dung chuyển khoản: <strong><c:out value="${transferNote}" /></strong></p>
                        <p style="color:var(--color-danger); font-size:0.85rem;">Vui lòng giữ nguyên nội dung chuyển khoản để được xác nhận nhanh.</p>
                    </div>
                </c:if>
            </div>

            <div>
                <h2 style="margin-bottom:12px;">Sản phẩm</h2>
                <table class="data-table" style="margin-bottom:24px;">
                    <thead>
                        <tr><th>Sản phẩm</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="item" items="${items}">
                            <tr>
                                <td><c:out value="${item.productName}" /></td>
                                <td>${item.quantity}</td>
                                <td><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true" />₫</td>
                                <td><fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true" />₫</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>

                <h2 style="margin-bottom:12px;">Lịch sử trạng thái</h2>
                <table class="data-table">
                    <thead>
                        <tr><th>Thời gian</th><th>Trạng thái</th><th>Thực hiện bởi</th><th>Ghi chú</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="h" items="${history}">
                            <tr>
                                <td><c:out value="${h.changedAtDisplay}" /></td>
                                <td><c:out value="${h.newStatus.displayName}" /></td>
                                <td><c:out value="${h.changedByName}" /></td>
                                <td><c:out value="${h.note}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
