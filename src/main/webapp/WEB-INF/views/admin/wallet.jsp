<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<main class="page-content">
    <div class="container">
        <h1>Nạp ví EAUT Pay</h1>
        <p class="hint" style="margin:8px 0 20px;">Tìm khách hàng theo tên đăng nhập hoặc email để nạp tiền vào ví.</p>

        <c:if test="${not empty error}">
            <div class="alert alert-error"><c:out value="${error}" /></div>
        </c:if>
        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <c:if test="${not empty pendingRequests}">
            <h2 style="margin-bottom:12px;">Yêu cầu nạp tiền chờ xác nhận (${fn:length(pendingRequests)})</h2>
            <div class="table-scroll">
                <table class="data-table" style="margin-bottom:24px;">
                <thead><tr><th>Thời gian</th><th>Khách hàng</th><th>Số tiền</th><th>Nội dung CK</th><th></th></tr></thead>
                <tbody>
                    <c:forEach var="r" items="${pendingRequests}">
                        <tr>
                            <td><c:out value="${r.createdAtDisplay}" /></td>
                            <td><c:out value="${r.customerName}" /></td>
                            <td><fmt:formatNumber value="${r.amount}" type="number" groupingUsed="true" />đ</td>
                            <td><strong><c:out value="${r.transferNote}" /></strong></td>
                            <td class="table-actions">
                                <form method="post" action="${ctx}/admin/wallet/topup-requests/confirm">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="requestId" value="${r.requestId}">
                                    <button type="submit" class="btn btn-sm btn-primary">Đã nhận tiền, cộng ví</button>
                                </form>
                                <form method="post" action="${ctx}/admin/wallet/topup-requests/reject">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="requestId" value="${r.requestId}">
                                    <button type="submit" class="btn btn-sm btn-danger">Từ chối</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            </div>
        </c:if>

        <form method="get" action="${ctx}/admin/wallet" style="display:flex; gap:10px; margin-bottom:24px;">
            <input type="text" name="q" placeholder="Tên đăng nhập hoặc email khách hàng" value="${query}" style="flex:1; padding:12px; border:1px solid var(--color-border); border-radius:var(--radius-input);">
            <button type="submit" class="btn btn-primary">Tìm</button>
        </form>

        <c:if test="${notFound}">
            <div class="empty-state"><h2>Không tìm thấy khách hàng phù hợp</h2></div>
        </c:if>

        <c:if test="${not empty customer}">
            <div class="card" style="padding:20px; margin-bottom:24px;">
                <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
                    <div>
                        <h2 style="font-size:1.1rem;"><c:out value="${customer.fullName}" /> <span class="hint">(@<c:out value="${customer.username}" />)</span></h2>
                        <p class="hint"><c:out value="${customer.email}" /></p>
                        <c:if test="${customer.eautStudent}"><span class="smart-id-badge" style="margin-top:6px;"><svg class="icon" aria-hidden="true"><use href="#i-id-card"/></svg> EAUT Smart ID</span></c:if>
                    </div>
                    <div class="wallet-badge" style="font-size:1.2rem; padding:8px 18px;">
                        <svg class="icon" aria-hidden="true"><use href="#i-wallet"/></svg> <fmt:formatNumber value="${customer.walletBalance}" type="number" groupingUsed="true" />đ
                    </div>
                </div>

                <form method="post" action="${ctx}/admin/wallet/topup" style="display:flex; gap:10px; align-items:end; margin-top:20px; flex-wrap:wrap;">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="userId" value="${customer.userId}">
                    <div class="form-group" style="margin-bottom:0;">
                        <label for="amount">Số tiền nạp (đ)</label>
                        <input type="number" id="amount" name="amount" min="1000" step="1000" required style="width:200px;">
                    </div>
                    <button type="submit" class="btn btn-primary">Nạp tiền</button>
                </form>
            </div>

            <h2 style="margin-bottom:12px;">Lịch sử giao dịch</h2>
            <c:choose>
                <c:when test="${empty transactions}">
                    <div class="empty-state"><h2>Chưa có giao dịch nào</h2></div>
                </c:when>
                <c:otherwise>
                    <div class="table-scroll">
                        <table class="data-table">
                        <thead><tr><th>Thời gian</th><th>Loại</th><th>Số tiền</th><th>Ghi chú</th></tr></thead>
                        <tbody>
                            <c:forEach var="tx" items="${transactions}">
                                <tr>
                                    <td><c:out value="${tx.createdAtDisplay}" /></td>
                                    <td><c:out value="${tx.type.displayName}" /></td>
                                    <td style="color:${tx.amount > 0 ? 'var(--color-success)' : 'var(--color-danger)'};">
                                        <fmt:formatNumber value="${tx.amount}" type="number" groupingUsed="true" />đ
                                    </td>
                                    <td><c:out value="${tx.note}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:if>

        <c:if test="${not empty candidates}">
            <h2>Có nhiều kết quả khớp</h2>
            <p class="hint">Chọn đúng sinh viên để xem ví.</p>
            <div class="table-scroll">
                <table class="data-table">
                <thead><tr><th>Họ tên</th><th>MSSV</th><th>Liên hệ</th><th></th></tr></thead>
                <tbody>
                    <c:forEach var="row" items="${candidates}">
                        <tr>
                            <td><c:out value="${row.user.fullName}" /></td>
                            <td><c:out value="${row.user.studentId}" /></td>
                            <td>
                                <c:out value="${row.user.email}" />
                                <c:if test="${not empty row.user.phone}"><div class="hint"><c:out value="${row.user.phone}" /></div></c:if>
                            </td>
                            <td class="table-actions">
                                <a class="btn btn-sm btn-secondary"
                                   href="${ctx}/admin/wallet?q=${fn:escapeXml(row.user.username)}">Xem ví</a>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            </div>
        </c:if>

        <h2>Đối soát số dư</h2>
        <%-- Nothing in the database forces users.wallet_balance to equal the sum of that user's
             wallet_transactions — no constraint, no trigger. So this table is the only way to find
             out, and an empty one is the check passing. --%>
        <c:choose>
            <c:when test="${empty balanceDrift}">
                <div class="alert alert-success">
                    Số dư của mọi ví đều khớp với sổ giao dịch.
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-error">
                    <strong>${fn:length(balanceDrift)} ví lệch so với sổ giao dịch.</strong>
                    Có nghĩa là đã có luồng nào đó cộng/trừ tiền mà không ghi sổ — cần kiểm tra trước khi tin số dư.
                </div>
                <div class="table-scroll">
                    <table class="data-table">
                        <thead><tr><th>Sinh viên</th><th>Số dư đang lưu</th><th>Tổng theo sổ</th><th>Lệch</th></tr></thead>
                        <tbody>
                            <c:forEach var="d" items="${balanceDrift}">
                                <tr>
                                    <td>
                                        <c:out value="${d.fullName}" />
                                        <div class="hint"><c:out value="${d.username}" /> <c:out value="${d.studentId}" /></div>
                                    </td>
                                    <td><fmt:formatNumber value="${d.storedBalance}" type="number" groupingUsed="true" />₫</td>
                                    <td><fmt:formatNumber value="${d.ledgerTotal}" type="number" groupingUsed="true" />₫</td>
                                    <td class="${d.difference > 0 ? 'delta-up' : 'delta-down'}">
                                        <fmt:formatNumber value="${d.difference}" type="number" groupingUsed="true" />₫
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
