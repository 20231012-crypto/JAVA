<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <div class="channel-head">
            <h1>Tổng quan</h1>

            <%-- The two sales channels, the way Sapo puts them: a manager standing at the counter
                 should reach the till in one click, not through a menu. The POS link is gated by
                 sales.counter, so an account that cannot ring up a sale is not shown a door it
                 would be refused at. --%>
            <div class="channel-switch" aria-label="Kênh bán hàng">
                <span class="channel-switch-label">Kênh bán hàng</span>
                <a class="channel-btn" href="${ctx}/products" target="_blank" rel="noopener">
                    <span class="channel-btn-icon" aria-hidden="true"><svg class="icon"><use href="#i-store"/></svg></span>
                    <span class="channel-btn-text">
                        <strong>Website</strong>
                        <small>Trang đặt hàng của sinh viên</small>
                    </span>
                </a>
                <c:if test="${sessionScope.user.permissions['sales.counter']}">
                    <a class="channel-btn channel-btn-primary" href="${ctx}/pos">
                        <span class="channel-btn-icon" aria-hidden="true"><svg class="icon"><use href="#i-receipt"/></svg></span>
                        <span class="channel-btn-text">
                            <strong>POS</strong>
                            <small>Bán hàng tại quầy</small>
                        </span>
                    </a>
                </c:if>
            </div>
        </div>

        <%-- KPI row. Each revenue figure carries its change against the same length of time
             immediately before it — the number alone says nothing about whether trade is up. --%>
        <div class="kpi-grid">
            <div class="kpi-card">
                <div class="kpi-label">Doanh thu hôm nay</div>
                <div class="kpi-value"><fmt:formatNumber value="${revenueToday}" type="number" groupingUsed="true" />₫</div>
                <c:choose>
                    <c:when test="${revenueTodayTrend == null}">
                        <div class="kpi-trend is-flat">Chưa có dữ liệu hôm qua để so sánh</div>
                    </c:when>
                    <c:otherwise>
                        <div class="kpi-trend ${revenueTodayTrend >= 0 ? 'is-up' : 'is-down'}">
                            ${revenueTodayTrend >= 0 ? '▲' : '▼'} ${revenueTodayTrend >= 0 ? revenueTodayTrend : -revenueTodayTrend}% so với hôm qua
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="kpi-card">
                <div class="kpi-label">Doanh thu tuần này</div>
                <div class="kpi-value"><fmt:formatNumber value="${revenueWeek}" type="number" groupingUsed="true" />₫</div>
                <c:choose>
                    <c:when test="${revenueWeekTrend == null}">
                        <div class="kpi-trend is-flat">Chưa có dữ liệu tuần trước</div>
                    </c:when>
                    <c:otherwise>
                        <div class="kpi-trend ${revenueWeekTrend >= 0 ? 'is-up' : 'is-down'}">
                            ${revenueWeekTrend >= 0 ? '▲' : '▼'} ${revenueWeekTrend >= 0 ? revenueWeekTrend : -revenueWeekTrend}% so với tuần trước
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="kpi-card">
                <div class="kpi-label">Doanh thu tháng này</div>
                <div class="kpi-value"><fmt:formatNumber value="${revenueMonth}" type="number" groupingUsed="true" />₫</div>
                <c:choose>
                    <c:when test="${revenueMonthTrend == null}">
                        <div class="kpi-trend is-flat">Chưa có dữ liệu tháng trước</div>
                    </c:when>
                    <c:otherwise>
                        <div class="kpi-trend ${revenueMonthTrend >= 0 ? 'is-up' : 'is-down'}">
                            ${revenueMonthTrend >= 0 ? '▲' : '▼'} ${revenueMonthTrend >= 0 ? revenueMonthTrend : -revenueMonthTrend}% so với tháng trước
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="kpi-card">
                <div class="kpi-label">Số dư ví toàn hệ thống</div>
                <div class="kpi-value"><fmt:formatNumber value="${walletFloat}" type="number" groupingUsed="true" />₫</div>
                <div class="kpi-trend is-flat">${activeCustomers} sinh viên đặt hàng trong 30 ngày</div>
            </div>
        </div>

        <div class="stat-grid">
            <div class="stat-tile">
                <div class="stat-label">Đơn thành công (tháng này)</div>
                <div class="stat-value">${completedThisMonth}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Đơn hủy / từ chối</div>
                <div class="stat-value">${cancelledThisMonth}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Đang chờ xác nhận</div>
                <div class="stat-value">${pendingCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Đang giao</div>
                <div class="stat-value">${shippingCount}</div>
            </div>
        </div>

        <c:if test="${not empty lowStock}">
            <div class="alert alert-error">
                <strong>${fn:length(lowStock)} món sắp hết hàng.</strong>
                <a href="${ctx}/admin/inventory">Xem tồn kho</a>
            </div>
        </c:if>

        <div class="chart-row">
            <section class="chart-card">
                <h2>Doanh thu theo giờ (hôm nay)</h2>
                <p class="hint">Cho thấy rõ giờ cao điểm — thường là 9-10h sáng và 12h trưa.</p>
                <jsp:include page="/WEB-INF/views/admin/_line-chart.jsp">
                    <jsp:param name="seriesAttr" value="hourPoints" />
                    <jsp:param name="chartId" value="chart-hour" />
                    <jsp:param name="unitLabel" value="Khung giờ" />
                </jsp:include>
            </section>

            <section class="chart-card">
                <h2>Doanh thu theo thứ (4 tuần gần nhất)</h2>
                <p class="hint">Ngày nào trong tuần bán chạy nhất.</p>
                <jsp:include page="/WEB-INF/views/admin/_line-chart.jsp">
                    <jsp:param name="seriesAttr" value="weekdayPoints" />
                    <jsp:param name="chartId" value="chart-weekday" />
                    <jsp:param name="unitLabel" value="Thứ" />
                </jsp:include>
            </section>
        </div>

        <section class="chart-card">
            <h2>Phương thức thanh toán (tháng này)</h2>
            <c:choose>
                <c:when test="${empty paymentSegments}">
                    <div class="empty-state"><h2>Chưa có đơn hoàn thành nào trong tháng</h2></div>
                </c:when>
                <c:otherwise>
                    <div class="donut-row">
                        <%-- One circle, one arc per segment, revealed by its dash pattern. The
                             transform rotates the start of the stroke to 12 o'clock; without it
                             SVG starts a circle's stroke at 3 o'clock. --%>
                        <svg class="donut-chart" viewBox="0 0 100 100" role="img"
                             aria-label="Tỷ lệ các phương thức thanh toán trong tháng này">
                            <g transform="rotate(-90 50 50)">
                                <c:forEach var="seg" items="${paymentSegments}">
                                    <circle class="donut-arc" cx="50" cy="50" r="40"
                                            stroke="${seg.color}"
                                            stroke-dasharray="${seg.dashArray}"
                                            stroke-dashoffset="${seg.dashOffset}">
                                        <title><c:out value="${seg.label}" />: ${seg.percent}% (${seg.count} đơn)</title>
                                    </circle>
                                </c:forEach>
                            </g>
                        </svg>
                        <%-- The legend carries the percentage as text beside each swatch, so the
                             chart is never colour-alone — two of these hues sit below 3:1 against
                             white and would be hard to tell apart otherwise. --%>
                        <ul class="donut-legend">
                            <c:forEach var="seg" items="${paymentSegments}">
                                <li>
                                    <span class="donut-swatch" style="background:${seg.color}" aria-hidden="true"></span>
                                    <span class="donut-legend-label"><c:out value="${seg.label}" /></span>
                                    <strong>${seg.percent}%</strong>
                                    <span class="hint">${seg.count} đơn · <fmt:formatNumber value="${seg.amount}" type="number" groupingUsed="true" />₫</span>
                                </li>
                            </c:forEach>
                        </ul>
                    </div>

                    <details class="chart-table-toggle">
                        <summary>Xem dạng bảng</summary>
                        <div class="table-scroll">
                            <table class="data-table">
                            <thead><tr><th>Phương thức</th><th>Số đơn</th><th>Tỷ lệ</th><th>Doanh thu</th></tr></thead>
                            <tbody>
                                <c:forEach var="seg" items="${paymentSegments}">
                                    <tr>
                                        <td><c:out value="${seg.label}" /></td>
                                        <td>${seg.count}</td>
                                        <td>${seg.percent}%</td>
                                        <td><fmt:formatNumber value="${seg.amount}" type="number" groupingUsed="true" />₫</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        </div>
                    </details>
                </c:otherwise>
            </c:choose>
        </section>

        <div class="chart-row">
            <section class="chart-card">
                <h2>Top 5 món bán chạy (30 ngày)</h2>
                <c:choose>
                    <c:when test="${empty bestSellers}"><p class="hint">Chưa có đơn hoàn thành nào.</p></c:when>
                    <c:otherwise>
                        <div class="table-scroll">
                            <table class="data-table">
                            <thead><tr><th>Món</th><th>Đã bán</th><th>Giá</th></tr></thead>
                            <tbody>
                                <c:forEach var="p" items="${bestSellers}">
                                    <tr>
                                        <td><c:out value="${p.name}" /></td>
                                        <td>${p.soldQuantity}</td>
                                        <td><fmt:formatNumber value="${p.price}" type="number" groupingUsed="true" />₫</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="chart-card">
                <h2>Top 5 sinh viên mua nhiều nhất (tháng này)</h2>
                <c:choose>
                    <c:when test="${empty topCustomers}"><p class="hint">Chưa có đơn hoàn thành nào trong tháng.</p></c:when>
                    <c:otherwise>
                        <div class="table-scroll">
                            <table class="data-table">
                            <thead><tr><th>Sinh viên</th><th>Số đơn</th><th>Tổng chi</th><th>Điểm</th></tr></thead>
                            <tbody>
                                <c:forEach var="cus" items="${topCustomers}">
                                    <tr>
                                        <td>
                                            <c:out value="${cus.fullName}" />
                                            <c:if test="${not empty cus.studentId}"><div class="hint"><c:out value="${cus.studentId}" /></div></c:if>
                                        </td>
                                        <td>${cus.orderCount}</td>
                                        <td><fmt:formatNumber value="${cus.totalSpent}" type="number" groupingUsed="true" />₫</td>
                                        <td>${cus.loyaltyPoints}</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
