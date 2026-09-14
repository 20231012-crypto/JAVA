<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<main class="page-content">
    <div class="container">
        <h1>Tổng quan quản lý</h1>

        <div class="stat-grid" style="margin-top:20px;">
            <div class="stat-tile">
                <div class="stat-label">Doanh thu hôm nay</div>
                <div class="stat-value"><fmt:formatNumber value="${revenueToday}" type="number" groupingUsed="true" />đ</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Chờ duyệt</div>
                <div class="stat-value">${pendingCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Đã duyệt, chờ lấy hàng</div>
                <div class="stat-value">${confirmedCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Đang giao</div>
                <div class="stat-value">${shippingCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Sắp hết hàng trên kệ (&lt; 5)</div>
                <div class="stat-value">${lowStockCount}</div>
            </div>
        </div>

        <div class="card" style="padding:20px; margin-bottom:24px;">
            <h2 style="font-size:1rem;">Lưu lượng đơn hôm nay theo giờ</h2>
            <p class="hint" style="margin-bottom:16px;">Giờ cao điểm: <strong><c:out value="${peakHourLabel}" /></strong></p>
            <div style="display:flex; align-items:flex-end; gap:6px; height:140px; border-bottom:1px solid var(--color-border);">
                <c:forEach var="bar" items="${hourlyChart}">
                    <div style="flex:1; display:flex; align-items:flex-end; justify-content:center; height:100%;"
                         title="${bar.label}: ${bar.count} đơn">
                        <div style="width:100%; max-width:28px; background:var(--color-accent);
                                    border-radius:4px 4px 0 0; height:${bar.heightPercent}%;
                                    min-height:${bar.count > 0 ? 3 : 0}px;"></div>
                    </div>
                </c:forEach>
            </div>
            <div style="display:flex; gap:6px; margin-top:6px;">
                <c:forEach var="bar" items="${hourlyChart}">
                    <div style="flex:1; text-align:center; font-size:0.7rem; color:var(--color-text-muted);">${bar.hour}</div>
                </c:forEach>
            </div>
        </div>

        <div class="card" style="padding:20px; margin-bottom:24px;">
            <h2 style="font-size:1rem;">Món được yêu thích nhiều nhất</h2>
            <c:choose>
                <c:when test="${empty mostFavorited}">
                    <p class="hint">Chưa có món nào được khách yêu thích.</p>
                </c:when>
                <c:otherwise>
                    <table class="cart-table" style="margin-top:12px;">
                        <thead>
                            <tr><th>Món</th><th>Lượt yêu thích</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="f" items="${mostFavorited}">
                                <tr>
                                    <td><c:out value="${f.productName}" /></td>
                                    <td>♥ ${f.favoriteCount}</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="stat-grid">
            <div class="stat-tile">
                <div class="stat-label">Sản phẩm</div>
                <div class="stat-value">${productCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Danh mục</div>
                <div class="stat-value">${categoryCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Tòa nhà</div>
                <div class="stat-value">${buildingCount}</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Nhân viên</div>
                <div class="stat-value">${staffCount}</div>
            </div>
        </div>

        <div class="filter-bar">
            <a class="btn btn-secondary" href="${ctx}/admin/products">Quản lý sản phẩm</a>
            <a class="btn btn-secondary" href="${ctx}/admin/categories">Quản lý danh mục</a>
            <a class="btn btn-secondary" href="${ctx}/admin/buildings">Tòa nhà & phí ship</a>
            <a class="btn btn-secondary" href="${ctx}/admin/stock-imports">Nhập hàng</a>
            <a class="btn btn-secondary" href="${ctx}/admin/staff">Tài khoản nhân viên</a>
            <c:if test="${sessionScope.user.permissions['roles.manage']}">
                <a class="btn btn-secondary" href="${ctx}/admin/roles">Vai trò &amp; phân quyền</a>
            </c:if>
            <c:if test="${sessionScope.user.permissions['wallet.topup']}">
                <a class="btn btn-secondary" href="${ctx}/admin/wallet">Nạp ví EAUT Pay</a>
            </c:if>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
