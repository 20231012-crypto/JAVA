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
