<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
            <h1>Đơn hàng <c:out value="${order.orderCode}" /></h1>
            <span class="badge badge-${fn:toLowerCase(order.orderStatus)}"><c:out value="${order.orderStatus.displayName}" /></span>
        </div>

        <c:if test="${order.paymentMethod == 'VIETQR' && order.paymentStatus == 'UNPAID'}">
            <div class="alert alert-error">Đơn chưa thanh toán. Kiểm tra ngân hàng theo nội dung <strong><c:out value="${transferNote}" /></strong> rồi bấm "Đã nhận thanh toán" — cần thanh toán xong mới xác nhận được đơn.</div>
        </c:if>

        <c:if test="${order.orderStatus == 'PENDING' || order.orderStatus == 'CONFIRMED'}">
            <div style="display:flex; gap:10px; margin:16px 0; flex-wrap:wrap;">
                <c:if test="${order.paymentMethod == 'VIETQR' && order.paymentStatus == 'UNPAID'}">
                    <form method="post" action="${pageContext.request.contextPath}/sales/orders/mark-paid">
                        <input type="hidden" name="orderId" value="${order.orderId}">
                        <button type="submit" class="btn btn-primary">Đã nhận thanh toán</button>
                    </form>
                </c:if>
                <c:if test="${order.orderStatus == 'PENDING'}">
                    <form method="post" action="${pageContext.request.contextPath}/sales/orders/confirm">
                        <input type="hidden" name="orderId" value="${order.orderId}">
                        <button type="submit" class="btn btn-primary">Xác nhận đơn</button>
                    </form>
                    <form method="post" action="${pageContext.request.contextPath}/sales/orders/reject">
                        <input type="hidden" name="orderId" value="${order.orderId}">
                        <input type="hidden" name="note" value="Từ chối bởi nhân viên bán hàng">
                        <button type="submit" class="btn btn-danger">Từ chối đơn</button>
                    </form>
                </c:if>
                <form method="post" action="${pageContext.request.contextPath}/sales/orders/cancel">
                    <input type="hidden" name="orderId" value="${order.orderId}">
                    <input type="hidden" name="note" value="Hủy bởi nhân viên bán hàng">
                    <button type="submit" class="btn btn-secondary">Hủy đơn</button>
                </form>
            </div>
        </c:if>

        <div class="detail-layout">
            <div class="order-summary">
                <div class="summary-row"><span>Kênh</span><span>${order.channel == 'ONLINE' ? 'Đặt qua web' : 'Tại quầy'}</span></div>
                <div class="summary-row"><span>Đặt lúc</span><span><c:out value="${order.createdAtDisplay}" /></span></div>
                <c:if test="${not empty order.buildingName}">
                    <div class="summary-row"><span>Giao đến</span><span><c:out value="${order.buildingName}" /></span></div>
                </c:if>
                <div class="summary-row"><span>Thanh toán</span><span><c:out value="${order.paymentMethod.displayName}" /></span></div>
                <div class="summary-row"><span>Trạng thái thanh toán</span><span><c:out value="${order.paymentStatus.displayName}" /></span></div>
                <c:if test="${not empty order.note}">
                    <div class="summary-row"><span>Ghi chú</span><span><c:out value="${order.note}" /></span></div>
                </c:if>
                <div class="summary-row"><span>Tạm tính</span><span><fmt:formatNumber value="${order.subtotal}" type="number" groupingUsed="true" />₫</span></div>
                <div class="summary-row"><span>Phí ship</span><span><fmt:formatNumber value="${order.shippingFee}" type="number" groupingUsed="true" />₫</span></div>
                <div class="summary-row summary-total"><span>Tổng cộng</span><span><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true" />₫</span></div>

                <c:if test="${not empty qrImageUrl}">
                    <div style="text-align:center; margin-top:20px; padding-top:16px; border-top:1px solid rgba(0,0,0,0.1);">
                        <img src="${qrImageUrl}" alt="Mã VietQR" style="width:100%; max-width:200px; border-radius:8px;">
                        <p style="margin-top:8px; font-size:0.9rem;">Nội dung: <strong><c:out value="${transferNote}" /></strong></p>
                    </div>
                </c:if>
            </div>

            <div>
                <h2 style="margin-bottom:12px;">Sản phẩm</h2>
                <table class="data-table" style="margin-bottom:24px;">
                    <thead><tr><th>Sản phẩm</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead>
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
                    <thead><tr><th>Thời gian</th><th>Trạng thái</th><th>Thực hiện bởi</th><th>Ghi chú</th></tr></thead>
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
