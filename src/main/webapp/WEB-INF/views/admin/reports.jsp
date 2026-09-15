<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Thống kê doanh thu</h1>

        <%-- One filter row above everything it scopes, rather than a control inside each card. --%>
        <form class="catalog-toolbar" method="get" action="${ctx}/admin/reports">
            <span class="catalog-count">Doanh thu tính từ đơn đã <strong>hoàn thành</strong>.</span>
            <span class="catalog-sort">
                <label for="days">Khoảng thời gian</label>
                <select id="days" name="days" onchange="this.form.submit()">
                    <option value="7" ${days == 7 ? 'selected' : ''}>7 ngày</option>
                    <option value="14" ${days == 14 ? 'selected' : ''}>14 ngày</option>
                    <option value="30" ${days == 30 ? 'selected' : ''}>30 ngày</option>
                    <option value="90" ${days == 90 ? 'selected' : ''}>90 ngày</option>
                </select>
                <noscript><button type="submit" class="btn btn-secondary btn-sm">Áp dụng</button></noscript>
            </span>
        </form>

        <div class="stat-grid">
            <div class="stat-tile">
                <div class="stat-label">Doanh thu hôm nay</div>
                <div class="stat-value"><fmt:formatNumber value="${revenueToday}" type="number" groupingUsed="true" />₫</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Doanh thu tháng này</div>
                <div class="stat-value"><fmt:formatNumber value="${revenueThisMonth}" type="number" groupingUsed="true" />₫</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Doanh thu ${days} ngày</div>
                <div class="stat-value"><fmt:formatNumber value="${windowRevenue}" type="number" groupingUsed="true" />₫</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Đơn hoàn thành ${days} ngày</div>
                <div class="stat-value">${windowOrders}</div>
            </div>
        </div>

        <h2 style="font-size:1rem; margin-top:8px;">Doanh thu theo ngày</h2>
        <%-- A single series, so every bar is the same colour: shading bars by their own height
             would encode the value twice and say nothing the bar length does not already say.
             Days with no trade are real zero-height bars, not gaps. --%>
        <div class="bar-chart" role="img"
             aria-label="Biểu đồ cột doanh thu ${days} ngày gần nhất. Số liệu chi tiết có trong bảng bên dưới.">
            <c:forEach var="point" items="${series}">
                <div class="bar-chart-col"
                     title="<c:out value='${point.dayDisplay}'/>: <fmt:formatNumber value='${point.revenue}' type='number' groupingUsed='true'/>₫ · ${point.orderCount} đơn">
                    <div class="bar-chart-track">
                        <div class="bar-chart-fill"
                             style="height:${point.revenue.signum() == 0 ? 0 : (point.revenue.doubleValue() / peakRevenue.doubleValue()) * 100}%"></div>
                    </div>
                    <div class="bar-chart-label"><c:out value="${point.dayDisplay}" /></div>
                </div>
            </c:forEach>
        </div>

        <%-- The table is the accessible twin of the chart: every value the bars encode is
             readable here without hovering, and it is what a screen reader gets. --%>
        <details class="chart-table-toggle">
            <summary>Xem số liệu dạng bảng</summary>
            <table class="data-table">
                <thead><tr><th>Ngày</th><th>Doanh thu</th><th>Số đơn hoàn thành</th></tr></thead>
                <tbody>
                    <c:forEach var="point" items="${series}">
                        <tr>
                            <td><c:out value="${point.dayDisplay}" /></td>
                            <td><fmt:formatNumber value="${point.revenue}" type="number" groupingUsed="true" />₫</td>
                            <td>${point.orderCount}</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </details>

        <h2 style="font-size:1rem; margin-top:24px;">Đơn hàng theo trạng thái</h2>
        <table class="data-table">
            <thead><tr><th>Trạng thái</th><th>Số đơn</th></tr></thead>
            <tbody>
                <c:forEach var="entry" items="${statusCounts}">
                    <tr>
                        <td><span class="badge badge-${fn:toLowerCase(entry.key)}"><c:out value="${entry.key}" /></span></td>
                        <td>${entry.value}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
