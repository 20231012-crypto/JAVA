<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<main class="page-content">
    <div class="container">
        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
            <h1>Đơn cần giao</h1>
            <span class="hint">Tự động cập nhật mỗi 15 giây</span>
        </div>

        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <div class="card" style="padding:14px 18px; margin-bottom:16px; display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
            <span class="hint">Đang trực (${fn:length(onDutyStaff)}):</span>
            <c:forEach var="s" items="${onDutyStaff}">
                <span class="badge badge-confirmed"><c:out value="${s.fullName}" /></span>
            </c:forEach>
            <form method="post" action="${ctx}/duty/toggle">
                <input type="hidden" name="redirect" value="/store/orders">
                <c:choose>
                    <c:when test="${sessionScope.user.onDuty}">
                        <input type="hidden" name="onDuty" value="false">
                        <button type="submit" class="btn btn-sm btn-secondary">Kết thúc ca của tôi</button>
                    </c:when>
                    <c:otherwise>
                        <input type="hidden" name="onDuty" value="true">
                        <button type="submit" class="btn btn-sm btn-primary">Bắt đầu ca của tôi</button>
                    </c:otherwise>
                </c:choose>
            </form>
        </div>

        <div class="kanban-board">
            <div class="kanban-column">
                <div class="kanban-column-header"><h3>📦 Sẵn sàng lấy hàng</h3><span class="kanban-count">${fn:length(confirmedOrders)}</span></div>
                <c:choose>
                    <c:when test="${empty confirmedOrders}"><div class="kanban-empty">Không có đơn nào</div></c:when>
                    <c:otherwise>
                        <c:forEach var="o" items="${confirmedOrders}">
                            <div class="kanban-card">
                                <div class="kanban-ticket">
                                    <span class="kanban-ticket-code"><c:out value="${o.orderCode}" /></span>
                                    <span class="kanban-elapsed">⏱ ${o.elapsedDisplay}</span>
                                </div>
                                <div><c:out value="${o.buildingName}" /> · <fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true" />₫</div>
                                <div class="hint"><c:out value="${o.paymentMethod.displayName}" /> · <c:out value="${o.paymentStatus.displayName}" /></div>
                                <form method="post" action="${ctx}/store/orders/pick" style="margin-top:10px;">
                                    <input type="hidden" name="orderId" value="${o.orderId}">
                                    <button type="submit" class="btn btn-sm btn-primary">Lấy hàng &amp; giao</button>
                                </form>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="kanban-column">
                <div class="kanban-column-header"><h3>🚴 Đang giao</h3><span class="kanban-count">${fn:length(shippingOrders)}</span></div>
                <c:choose>
                    <c:when test="${empty shippingOrders}"><div class="kanban-empty">Không có đơn nào</div></c:when>
                    <c:otherwise>
                        <c:forEach var="o" items="${shippingOrders}">
                            <div class="kanban-card">
                                <div class="kanban-ticket">
                                    <span class="kanban-ticket-code"><c:out value="${o.orderCode}" /></span>
                                    <span class="kanban-elapsed">⏱ ${o.elapsedDisplay}</span>
                                </div>
                                <div><c:out value="${o.buildingName}" /> · <fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true" />₫</div>
                                <div class="hint"><c:out value="${o.paymentMethod.displayName}" /> · <c:out value="${o.paymentStatus.displayName}" /></div>
                                <form method="post" action="${ctx}/store/orders/complete" style="margin-top:10px;">
                                    <input type="hidden" name="orderId" value="${o.orderId}">
                                    <button type="submit" class="btn btn-sm btn-secondary">Đã giao xong</button>
                                </form>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</main>
<script>
    setTimeout(function () { window.location.reload(); }, 15000);
</script>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
