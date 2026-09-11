<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<main class="page-content">
    <div class="container">
        <h1>Ví của tôi</h1>

        <c:if test="${not empty error}">
            <div class="alert alert-error"><c:out value="${error}" /></div>
        </c:if>

        <div class="stat-grid" style="margin-top:20px;">
            <div class="stat-tile">
                <div class="stat-label">Số dư ví EAUT Pay</div>
                <div class="stat-value">💳 <fmt:formatNumber value="${customer.walletBalance}" type="number" groupingUsed="true" />đ</div>
            </div>
            <div class="stat-tile">
                <div class="stat-label">Điểm tích luỹ</div>
                <div class="stat-value">🎁 ${customer.loyaltyPoints}</div>
            </div>
            <c:if test="${customer.eautStudent}">
                <div class="stat-tile">
                    <div class="stat-label">Trạng thái</div>
                    <div class="stat-value" style="font-size:1.1rem;"><span class="smart-id-badge">🪪 EAUT Smart ID</span></div>
                </div>
            </c:if>
        </div>

        <c:if test="${not empty newRequest}">
            <div class="card" style="padding:24px; margin-bottom:24px; text-align:center;">
                <h2 style="margin-bottom:12px;">Quét mã để nạp <fmt:formatNumber value="${newRequest.amount}" type="number" groupingUsed="true" />đ</h2>
                <img src="${qrImageUrl}" alt="Mã VietQR nạp ví" style="width:100%; max-width:240px; border-radius:8px;">
                <p style="margin-top:12px;">Nội dung chuyển khoản: <strong><c:out value="${newRequest.transferNote}" /></strong></p>
                <p style="color:var(--color-danger); font-size:0.85rem; margin-top:4px;">
                    Giữ nguyên nội dung chuyển khoản. Ví chỉ được cộng tiền sau khi Admin/nhân viên kiểm tra và xác nhận đã nhận được — không phải tự động.
                </p>
            </div>
        </c:if>

        <div class="card" style="padding:20px; margin-bottom:24px;">
            <h2 style="font-size:1rem; margin-bottom:12px;">Nạp tiền vào ví qua VietQR</h2>
            <form method="post" action="${ctx}/wallet/topup-request" style="display:flex; gap:10px; align-items:end; flex-wrap:wrap;">
                <div class="form-group" style="margin-bottom:0;">
                    <label for="amount">Số tiền (tối thiểu 10.000đ)</label>
                    <input type="number" id="amount" name="amount" min="10000" step="1000" required style="width:220px;">
                </div>
                <button type="submit" class="btn btn-primary">Tạo mã QR nạp tiền</button>
            </form>
        </div>

        <c:if test="${not empty topupRequests}">
            <h2 style="margin-bottom:12px;">Yêu cầu nạp tiền</h2>
            <table class="data-table" style="margin-bottom:24px;">
                <thead><tr><th>Thời gian</th><th>Số tiền</th><th>Nội dung CK</th><th>Trạng thái</th></tr></thead>
                <tbody>
                    <c:forEach var="r" items="${topupRequests}">
                        <tr>
                            <td><c:out value="${r.createdAtDisplay}" /></td>
                            <td><fmt:formatNumber value="${r.amount}" type="number" groupingUsed="true" />đ</td>
                            <td><c:out value="${r.transferNote}" /></td>
                            <td>
                                <span class="badge ${r.status == 'CONFIRMED' ? 'badge-completed' : r.status == 'REJECTED' ? 'badge-rejected' : 'badge-pending'}">
                                    <c:out value="${r.status.displayName}" />
                                </span>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:if>

        <div style="display:grid; grid-template-columns:1fr 1fr; gap:24px;">
            <div>
                <h2 style="margin-bottom:12px;">Lịch sử ví EAUT Pay</h2>
                <c:choose>
                    <c:when test="${empty walletTransactions}"><div class="empty-state"><h2>Chưa có giao dịch</h2></div></c:when>
                    <c:otherwise>
                        <table class="data-table">
                            <thead><tr><th>Thời gian</th><th>Loại</th><th>Số tiền</th></tr></thead>
                            <tbody>
                                <c:forEach var="tx" items="${walletTransactions}">
                                    <tr>
                                        <td><c:out value="${tx.createdAtDisplay}" /></td>
                                        <td><c:out value="${tx.type.displayName}" /></td>
                                        <td style="color:${tx.amount > 0 ? 'var(--color-success)' : 'var(--color-danger)'};">
                                            <fmt:formatNumber value="${tx.amount}" type="number" groupingUsed="true" />đ
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </div>
            <div>
                <h2 style="margin-bottom:12px;">Lịch sử điểm tích luỹ</h2>
                <c:choose>
                    <c:when test="${empty loyaltyTransactions}"><div class="empty-state"><h2>Chưa có giao dịch</h2></div></c:when>
                    <c:otherwise>
                        <table class="data-table">
                            <thead><tr><th>Thời gian</th><th>Loại</th><th>Điểm</th></tr></thead>
                            <tbody>
                                <c:forEach var="tx" items="${loyaltyTransactions}">
                                    <tr>
                                        <td><c:out value="${tx.createdAtDisplay}" /></td>
                                        <td><c:out value="${tx.type.displayName}" /></td>
                                        <td style="color:${tx.points > 0 ? 'var(--color-success)' : 'var(--color-danger)'};">
                                            ${tx.points > 0 ? '+' : ''}${tx.points}
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
