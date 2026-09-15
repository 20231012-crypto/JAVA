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
            <h1>Đơn hàng</h1>
            <c:if test="${boardMode}">
                <span class="hint">Tự động cập nhật mỗi 15 giây</span>
            </c:if>
        </div>

        <c:if test="${boardMode}">
            <div class="card" style="padding:14px 18px; margin:16px 0; display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:16px;">
                <c:if test="${sessionScope.user.permissions['shop.status']}">
                    <form method="post" action="${ctx}/sales/shop-status" style="display:flex; align-items:center; gap:10px;">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="redirect" value="/sales/orders">
                        <c:choose>
                            <c:when test="${shopStatus.acceptingOrders}">
                                <span class="badge badge-completed">🟢 Đang mở nhận đơn</span>
                                <input type="hidden" name="accepting" value="false">
                                <button type="submit" class="btn btn-sm btn-danger">Tạm ngưng nhận đơn</button>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge-rejected">🔴 Đang tạm ngưng nhận đơn</span>
                                <input type="hidden" name="accepting" value="true">
                                <button type="submit" class="btn btn-sm btn-primary">Mở nhận đơn lại</button>
                            </c:otherwise>
                        </c:choose>
                    </form>
                </c:if>

                <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
                    <span class="hint">Đang trực (${fn:length(onDutyStaff)}):</span>
                    <c:forEach var="s" items="${onDutyStaff}">
                        <span class="badge badge-confirmed"><c:out value="${s.fullName}" /></span>
                    </c:forEach>
                    <form method="post" action="${ctx}/duty/toggle">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="redirect" value="/sales/orders">
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
            </div>
        </c:if>

        <div class="filter-bar">
            <a class="filter-chip ${empty selectedStatus ? 'active' : ''}" href="?">Bảng trực tiếp</a>
            <a class="filter-chip ${selectedStatus == 'PENDING' ? 'active' : ''}" href="?status=PENDING">Chờ xác nhận</a>
            <a class="filter-chip ${selectedStatus == 'CONFIRMED' ? 'active' : ''}" href="?status=CONFIRMED">Đã xác nhận</a>
            <a class="filter-chip ${selectedStatus == 'SHIPPING' ? 'active' : ''}" href="?status=SHIPPING">Đang giao</a>
            <a class="filter-chip ${selectedStatus == 'COMPLETED' ? 'active' : ''}" href="?status=COMPLETED">Hoàn thành</a>
            <a class="filter-chip ${selectedStatus == 'REJECTED' ? 'active' : ''}" href="?status=REJECTED">Bị từ chối</a>
            <a class="filter-chip ${selectedStatus == 'CANCELLED' ? 'active' : ''}" href="?status=CANCELLED">Đã hủy</a>
            <a class="filter-chip ${selectedStatus == 'ALL' ? 'active' : ''}" href="?status=ALL">Tất cả</a>
        </div>

        <c:if test="${boardMode}">
            <div class="kanban-board">
                <div class="kanban-column">
                    <div class="kanban-column-header"><h3>🟠 Đơn mới chờ duyệt</h3><span class="kanban-count">${fn:length(pendingOrders)}</span></div>
                    <c:choose>
                        <c:when test="${empty pendingOrders}"><div class="kanban-empty">Không có đơn chờ duyệt</div></c:when>
                        <c:otherwise>
                            <c:forEach var="o" items="${pendingOrders}">
                                <div class="kanban-card">
                                    <div class="kanban-ticket">
                                        <span class="kanban-ticket-code"><c:out value="${o.orderCode}" /></span>
                                        <span class="kanban-elapsed">⏱ ${o.elapsedDisplay}</span>
                                    </div>
                                    <c:if test="${o.channel == 'ONLINE'}">
                                        <div style="font-weight:600;">
                                            <c:out value="${o.customerName}" />
                                            <c:if test="${not empty o.customerStudentId}"> · MSSV <c:out value="${o.customerStudentId}" /></c:if>
                                            <c:if test="${not empty o.customerClassName}"> · <c:out value="${o.customerClassName}" /></c:if>
                                        </div>
                                    </c:if>
                                    <div><c:out value="${o.channel == 'ONLINE' ? o.buildingName : 'Bán tại quầy'}" /> · <fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true" />₫</div>
                                    <div class="hint"><c:out value="${o.paymentMethod.displayName}" /> · <c:out value="${o.paymentStatus.displayName}" /></div>
                                    <div class="table-actions" style="margin-top:10px;">
                                        <a class="btn btn-sm btn-secondary" href="${ctx}/sales/orders/detail?id=${o.orderId}">Xem</a>
                                        <form method="post" action="${ctx}/sales/orders/confirm" style="display:inline;">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <input type="hidden" name="orderId" value="${o.orderId}">
                                            <button type="submit" class="btn btn-sm btn-primary">Duyệt</button>
                                        </form>
                                        <form method="post" action="${ctx}/sales/orders/reject" style="display:inline;">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <input type="hidden" name="orderId" value="${o.orderId}">
                                            <button type="submit" class="btn btn-sm btn-danger">Từ chối</button>
                                        </form>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="kanban-column">
                    <div class="kanban-column-header"><h3>🔵 Đã duyệt, chờ lấy hàng</h3><span class="kanban-count">${fn:length(confirmedOrders)}</span></div>
                    <c:choose>
                        <c:when test="${empty confirmedOrders}"><div class="kanban-empty">Không có đơn nào</div></c:when>
                        <c:otherwise>
                            <c:forEach var="o" items="${confirmedOrders}">
                                <div class="kanban-card">
                                    <div class="kanban-ticket">
                                        <span class="kanban-ticket-code"><c:out value="${o.orderCode}" /></span>
                                        <span class="kanban-elapsed">⏱ ${o.elapsedDisplay}</span>
                                    </div>
                                    <c:if test="${o.channel == 'ONLINE'}">
                                        <div style="font-weight:600;">
                                            <c:out value="${o.customerName}" />
                                            <c:if test="${not empty o.customerStudentId}"> · MSSV <c:out value="${o.customerStudentId}" /></c:if>
                                        </div>
                                    </c:if>
                                    <div><c:out value="${o.buildingName}" /> · <fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true" />₫</div>
                                    <c:if test="${not empty o.estimatedReadyAtEpochMillis}">
                                        <div class="countdown-timer" data-ready-at="${o.estimatedReadyAtEpochMillis}" style="margin-top:6px; font-weight:700; color:var(--color-info);"></div>
                                    </c:if>
                                    <div class="table-actions" style="margin-top:10px;">
                                        <a class="btn btn-sm btn-secondary" href="${ctx}/sales/orders/detail?id=${o.orderId}">Xem</a>
                                        <form method="post" action="${ctx}/sales/orders/cancel" style="display:inline;">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <input type="hidden" name="orderId" value="${o.orderId}">
                                            <button type="submit" class="btn btn-sm btn-danger">Hủy</button>
                                        </form>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="kanban-column">
                    <div class="kanban-column-header"><h3>🟢 Đang giao</h3><span class="kanban-count">${fn:length(shippingOrders)}</span></div>
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
                                    <div class="hint">Đang chờ nhân viên cửa hàng giao xong</div>
                                    <div class="table-actions" style="margin-top:10px;">
                                        <a class="btn btn-sm btn-secondary" href="${ctx}/sales/orders/detail?id=${o.orderId}">Xem</a>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </c:if>

        <c:if test="${!boardMode}">
            <c:choose>
                <c:when test="${empty orders}">
                    <div class="empty-state"><h2>Không có đơn hàng nào</h2></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                            <tr><th>Mã đơn</th><th>Kênh</th><th>Khách/Tòa nhà</th><th>Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th><th></th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="o" items="${orders}">
                                <tr>
                                    <td><c:out value="${o.orderCode}" /></td>
                                    <td>${o.channel == 'ONLINE' ? 'Web' : 'Tại quầy'}</td>
                                    <td><c:out value="${o.buildingName}" /></td>
                                    <td><fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true" />₫</td>
                                    <td><c:out value="${o.paymentStatus.displayName}" /></td>
                                    <td><span class="badge badge-${fn:toLowerCase(o.orderStatus)}"><c:out value="${o.orderStatus.displayName}" /></span></td>
                                    <td><a href="${ctx}/sales/orders/detail?id=${o.orderId}">Xem</a></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>
</main>
<c:if test="${boardMode}">
    <script>
        // Simple polling refresh for a "live" board — no WebSocket/SSE infra needed for this scale.
        setTimeout(function () { window.location.reload(); }, 15000);

        // KDS countdown timers — estimatedReadyAt is a fixed server timestamp (epoch ms); this
        // just re-renders "còn X phút" client-side every second, no server round-trip needed.
        function renderCountdowns() {
            document.querySelectorAll(".countdown-timer").forEach(function (el) {
                var readyAt = Number(el.getAttribute("data-ready-at"));
                var remainingMs = readyAt - Date.now();
                if (remainingMs <= 0) {
                    el.textContent = "⏰ Đã quá giờ dự kiến";
                    el.style.color = "var(--color-danger)";
                } else {
                    var totalSeconds = Math.floor(remainingMs / 1000);
                    var minutes = Math.floor(totalSeconds / 60);
                    var seconds = totalSeconds % 60;
                    el.textContent = "🍳 Còn " + minutes + " phút " + (seconds < 10 ? "0" : "") + seconds + " giây";
                }
            });
        }
        renderCountdowns();
        setInterval(renderCountdowns, 1000);
    </script>
</c:if>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
