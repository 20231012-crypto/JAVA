<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <a class="back-link" href="${ctx}/admin/orders">&#8249; Về danh sách đơn</a>
        <h1>Đơn <c:out value="${order.orderCode}" /></h1>

        <c:if test="${not empty sessionScope.actionMessage}">
            <div class="alert alert-success"><c:out value="${sessionScope.actionMessage}" /></div>
            <c:remove var="actionMessage" scope="session" />
        </c:if>
        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <div class="stat-grid">
            <div class="stat-tile">
                <div class="stat-label">Trạng thái</div>
                <div class="stat-value stat-value-sm">
                    <span class="badge badge-${fn:toLowerCase(order.orderStatus)}">${order.orderStatus.displayName}</span>
                </div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Tổng tiền</div>
                <div class="stat-value"><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true" />₫</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Thanh toán</div>
                <div class="stat-value stat-value-sm">${order.paymentMethod.displayName}</div>
                <div class="stat-label">
                    <c:choose>
                        <c:when test="${order.paymentStatus == 'PAID'}">Đã thanh toán</c:when>
                        <c:otherwise>Chưa thanh toán</c:otherwise>
                    </c:choose>
                </div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Đặt lúc</div>
                <div class="stat-value stat-value-sm"><c:out value="${order.createdAtDisplay}" /></div>
            </div>
        </div>

        <h2>Khách hàng</h2>
        <div class="table-scroll">
            <table class="data-table">
            <tbody>
                <tr><th>Họ tên</th><td>
                    <c:choose>
                        <c:when test="${not empty order.customerName}"><c:out value="${order.customerName}" /></c:when>
                        <c:otherwise><span class="hint">Khách vãng lai (bán tại quầy)</span></c:otherwise>
                    </c:choose>
                </td></tr>
                <c:if test="${not empty order.customerStudentId}">
                    <tr><th>MSSV</th><td><c:out value="${order.customerStudentId}" /> <c:out value="${order.customerClassName}" /></td></tr>
                </c:if>
                <c:if test="${not empty order.customerPhone}">
                    <tr><th>Điện thoại</th><td><a href="tel:${fn:escapeXml(order.customerPhone)}"><c:out value="${order.customerPhone}" /></a></td></tr>
                </c:if>
                <tr><th>Nơi nhận</th><td>
                    <c:choose>
                        <c:when test="${not empty order.buildingName}"><c:out value="${order.buildingName}" /></c:when>
                        <c:otherwise>Nhận tại quầy</c:otherwise>
                    </c:choose>
                </td></tr>
                <c:if test="${not empty order.note}">
                    <tr><th>Ghi chú</th><td><c:out value="${order.note}" /></td></tr>
                </c:if>
            </tbody>
        </table>
        </div>

        <h2>Các món</h2>
        <div class="table-scroll">
            <table class="data-table">
            <thead><tr><th>Món</th><th>Đơn giá</th><th>SL</th><th>Thành tiền</th></tr></thead>
            <tbody>
                <c:forEach var="item" items="${items}">
                    <tr>
                        <td><c:out value="${item.productName}" /></td>
                        <td><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true" />₫</td>
                        <td>${item.quantity}</td>
                        <td><fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true" />₫</td>
                    </tr>
                </c:forEach>
            </tbody>
            <tfoot>
                <tr><th colspan="3">Tạm tính</th><td><fmt:formatNumber value="${order.subtotal}" type="number" groupingUsed="true" />₫</td></tr>
                <tr><th colspan="3">Phí giao hàng</th><td><fmt:formatNumber value="${order.shippingFee}" type="number" groupingUsed="true" />₫</td></tr>
                <c:if test="${order.discountAmount > 0}">
                    <tr><th colspan="3">Giảm giá EAUT Smart ID</th><td>-<fmt:formatNumber value="${order.discountAmount}" type="number" groupingUsed="true" />₫</td></tr>
                </c:if>
                <c:if test="${order.walletDiscountAmount > 0}">
                    <tr><th colspan="3">Ưu đãi trả bằng Ví EAUT Pay</th><td>-<fmt:formatNumber value="${order.walletDiscountAmount}" type="number" groupingUsed="true" />₫</td></tr>
                </c:if>
                <c:if test="${order.loyaltyPointsUsed > 0}">
                    <tr><th colspan="3">Dùng ${order.loyaltyPointsUsed} điểm</th><td>-<fmt:formatNumber value="${order.loyaltyDiscountAmount}" type="number" groupingUsed="true" />₫</td></tr>
                </c:if>
                <tr><th colspan="3">Tổng cộng</th><td><strong><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true" />₫</strong></td></tr>
            </tfoot>
        </table>
        </div>

        <h2>Lịch sử trạng thái</h2>
        <c:if test="${empty history}">
            <p class="hint">Đơn này chưa có bản ghi chuyển trạng thái nào.</p>
        </c:if>
        <ol class="timeline">
            <c:forEach var="h" items="${history}">
                <li class="timeline-item">
                    <div class="timeline-dot" aria-hidden="true"></div>
                    <div class="timeline-body">
                        <div class="timeline-head">
                            <c:choose>
                                <c:when test="${empty h.oldStatus}">Tạo đơn</c:when>
                                <c:otherwise>${h.oldStatus.displayName} &rarr; ${h.newStatus.displayName}</c:otherwise>
                            </c:choose>
                            <span class="timeline-time"><c:out value="${h.changedAtDisplay}" /></span>
                        </div>
                        <c:if test="${not empty h.note}"><p class="timeline-note"><c:out value="${h.note}" /></p></c:if>
                        <c:if test="${not empty h.changedByName}">
                            <p class="hint">Bởi <c:out value="${h.changedByName}" /></p>
                        </c:if>
                    </div>
                </li>
            </c:forEach>
        </ol>

        <%-- Refund. Only rendered for an admin who actually holds orders.refund, so a sales-staff
             account never sees a button it would be 403'd on. --%>
        <c:if test="${sessionScope.user.permissions['orders.refund']}">
            <h2>Can thiệp của quản trị</h2>
            <c:choose>
                <c:when test="${order.refunded}">
                    <div class="alert alert-success">
                        Đơn này đã được hoàn
                        <strong><fmt:formatNumber value="${order.refundedAmount}" type="number" groupingUsed="true" />₫</strong>
                        lúc <c:out value="${order.refundedAtDisplay}" />.
                    </div>
                </c:when>
                <c:when test="${order.orderStatus == 'CANCELLED' or order.orderStatus == 'REJECTED'}">
                    <p class="hint">Đơn đã ở trạng thái ${order.orderStatus.displayName}, hàng đã được trả về kệ trước đó.</p>
                </c:when>
                <c:otherwise>
                    <div class="danger-zone">
                        <h3>Hủy đơn và hoàn trả</h3>
                        <p>
                            Thao tác này sẽ hủy đơn, trả hàng về kệ,
                            <c:if test="${order.paymentStatus == 'PAID' and not empty order.customerId}">
                                hoàn <strong><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true" />₫</strong> vào Ví EAUT Pay của khách,
                            </c:if>
                            <c:if test="${order.loyaltyPointsUsed > 0}">
                                trả lại <strong>${order.loyaltyPointsUsed} điểm</strong> khách đã dùng,
                            </c:if>
                            và thu hồi điểm đã tích cho đơn này. <strong>Không thể hoàn tác.</strong>
                        </p>
                        <c:if test="${order.paymentMethod == 'VIETQR' and order.paymentStatus == 'PAID'}">
                            <p class="hint">Đơn trả bằng VietQR: tiền được hoàn vào Ví EAUT Pay, không chuyển ngược về ngân hàng.</p>
                        </c:if>
                        <form method="post" action="${ctx}/admin/orders/refund" data-confirm-code="${fn:escapeXml(order.orderCode)}">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="orderId" value="${order.orderId}">
                            <div class="form-group">
                                <label for="refund-reason">Lý do</label>
                                <input type="text" id="refund-reason" name="reason" maxlength="200"
                                       placeholder="Ví dụ: hết món đột xuất, giao nhầm đơn">
                            </div>
                            <%-- Typing the order code is the deliberate-action gate: it makes an
                                 accidental click impossible and forces the admin to look at WHICH
                                 order they are about to refund. Enforced again in JS below so the
                                 button stays disabled until it matches. --%>
                            <div class="form-group">
                                <label for="refund-confirm">Gõ lại mã đơn <code><c:out value="${order.orderCode}" /></code> để xác nhận</label>
                                <input type="text" id="refund-confirm" name="confirmCode" autocomplete="off"
                                       data-confirm-input aria-describedby="refund-confirm-hint" required>
                                <span class="hint" id="refund-confirm-hint">Máy chủ kiểm tra lại mã này trước khi hoàn tiền.</span>
                            </div>
                            <button type="submit" class="btn btn-danger" data-confirm-submit>
                                Hủy đơn &amp; hoàn trả
                            </button>
                        </form>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
