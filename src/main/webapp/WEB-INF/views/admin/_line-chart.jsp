<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- A single-series line chart, drawn as one polyline in a 0-100 viewBox.

     The points arrive already scaled — AdminDashboardServlet does the arithmetic, because EL
     cannot without turning this file into a calculator. Here we only print coordinates.

     Parameters:
       seriesAttr  name of the request attribute holding the List<ChartPoint>
       chartId     unique id, so two charts on one page do not share gradient/aria ids
       unitLabel   what the x axis counts, for the table fallback's first column

     One series, so no legend: the heading above already names what is plotted. The y axis starts
     at zero (enforced in the servlet's scaling) because this is revenue, and a truncated baseline
     turns an ordinary quiet hour into a cliff. --%>
<c:set var="series" value="${requestScope[param.seriesAttr]}" />
<c:set var="chartId" value="${param.chartId}" />

<c:choose>
    <c:when test="${empty series}">
        <p class="hint">Chưa có dữ liệu để vẽ.</p>
    </c:when>
    <c:otherwise>
        <c:set var="pointList"><c:forEach var="p" items="${series}">${p.x},${p.y} </c:forEach></c:set>

        <%-- viewBox height 100 to match the 0-100 scale the servlet produces. With
             preserveAspectRatio="none" the box is stretched to whatever the CSS height is, so the
             viewBox aspect does not have to match the rendered one — but the COORDINATES do, and
             a 46-high box with 0-100 coordinates sent the line straight out of the card. --%>
        <svg class="line-chart" viewBox="0 0 100 100" preserveAspectRatio="none"
             role="img" aria-labelledby="${chartId}-title">
            <title id="${chartId}-title">Biểu đồ đường doanh thu. Số liệu chi tiết có trong bảng bên dưới.</title>

            <%-- Recessive gridlines at quarters. They orient the eye without competing with the
                 data, which is why they are hairlines in the border colour rather than solid. --%>
            <g class="line-chart-grid" aria-hidden="true">
                <line x1="0" y1="0" x2="100" y2="0" />
                <line x1="0" y1="25" x2="100" y2="25" />
                <line x1="0" y1="50" x2="100" y2="50" />
                <line x1="0" y1="75" x2="100" y2="75" />
                <line x1="0" y1="100" x2="100" y2="100" />
            </g>

            <%-- vector-effect keeps the 2px stroke 2px: preserveAspectRatio="none" stretches the
                 viewBox horizontally, which would otherwise scale the line thickness with it. --%>
            <polyline class="line-chart-line" points="${pointList}" />
        </svg>

        <div class="line-chart-axis" aria-hidden="true">
            <c:forEach var="p" items="${series}" varStatus="loop">
                <%-- Every label would collide on a narrow card; every third is enough to read the
                     shape, and the table below carries the exact figures. --%>
                <span class="${loop.index % 3 == 0 ? '' : 'is-muted'}">
                    <c:if test="${loop.index % 3 == 0}"><c:out value="${p.label}" /></c:if>
                </span>
            </c:forEach>
        </div>

        <details class="chart-table-toggle">
            <summary>Xem dạng bảng</summary>
            <table class="data-table">
                <thead><tr><th><c:out value="${param.unitLabel}" /></th><th>Doanh thu</th></tr></thead>
                <tbody>
                    <c:forEach var="p" items="${series}">
                        <tr>
                            <td><c:out value="${p.label}" /></td>
                            <td><fmt:formatNumber value="${p.value}" type="number" groupingUsed="true" />₫</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </details>
    </c:otherwise>
</c:choose>
