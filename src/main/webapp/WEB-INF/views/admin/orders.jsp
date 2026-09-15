<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Quản lý đơn hàng</h1>

        <c:if test="${not empty sessionScope.actionMessage}">
            <div class="alert alert-success"><c:out value="${sessionScope.actionMessage}" /></div>
            <c:remove var="actionMessage" scope="session" />
        </c:if>
        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <%-- GET, so a filtered view is a shareable URL and the browser's Back button works.
             Every control is a plain form field: the whole filter works with JavaScript off. --%>
        <form class="toolbar" method="get" action="${ctx}/admin/orders">
            <div class="toolbar-field">
                <label for="f-q">Tìm kiếm</label>
                <input type="search" id="f-q" name="q" value="<c:out value='${qText}'/>"
                       placeholder="Mã đơn, tên, SĐT, MSSV">
            </div>
            <div class="toolbar-field">
                <label for="f-status">Trạng thái</label>
                <select id="f-status" name="status">
                    <option value="">Tất cả</option>
                    <c:forEach var="s" items="${statuses}">
                        <option value="${s}" ${qStatus == s ? 'selected' : ''}>${s.displayName}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="toolbar-field">
                <label for="f-payment">Thanh toán</label>
                <select id="f-payment" name="payment">
                    <option value="">Tất cả</option>
                    <c:forEach var="p" items="${paymentMethods}">
                        <option value="${p}" ${qPayment == p ? 'selected' : ''}>${p.displayName}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="toolbar-field">
                <label for="f-channel">Kênh</label>
                <select id="f-channel" name="channel">
                    <option value="">Tất cả</option>
                    <option value="ONLINE" ${qChannel == 'ONLINE' ? 'selected' : ''}>Đặt online</option>
                    <option value="COUNTER" ${qChannel == 'COUNTER' ? 'selected' : ''}>Bán tại quầy</option>
                </select>
            </div>
            <div class="toolbar-field">
                <label for="f-from">Từ ngày</label>
                <input type="date" id="f-from" name="from" value="<c:out value='${qFrom}'/>">
            </div>
            <div class="toolbar-field">
                <label for="f-to">Đến ngày</label>
                <input type="date" id="f-to" name="to" value="<c:out value='${qTo}'/>">
            </div>
            <div class="toolbar-actions">
                <button type="submit" class="btn btn-primary btn-sm">Lọc</button>
                <c:if test="${filter.active}">
                    <a class="btn btn-secondary btn-sm" href="${ctx}/admin/orders">Xóa lọc</a>
                </c:if>
            </div>
        </form>

        <div class="catalog-toolbar">
            <span class="catalog-count"><strong>${totalOrders}</strong> đơn hàng</span>
        </div>

        <c:choose>
            <c:when test="${empty orders}">
                <div class="empty-state">
                    <h2>Không có đơn hàng nào</h2>
                    <c:if test="${filter.active}">
                        <p>Không có đơn nào khớp bộ lọc hiện tại. Thử nới rộng khoảng ngày hoặc bỏ bớt điều kiện.</p>
                    </c:if>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-scroll">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Mã đơn</th><th>Khách hàng</th><th>Nơi nhận</th>
                                <th>Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th>
                                <th>Thời gian</th><th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="o" items="${orders}">
                                <tr>
                                    <td>
                                        <strong><c:out value="${o.orderCode}" /></strong>
                                        <c:if test="${o.channel == 'COUNTER'}"><div class="hint">Bán tại quầy</div></c:if>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty o.customerName}">
                                                <c:out value="${o.customerName}" />
                                                <%-- The separator belongs to the pair, not to the
                                                     phone: a customer with no MSSV was rendering
                                                     as a stray leading "·". --%>
                                                <div class="hint">
                                                    <c:out value="${o.customerStudentId}" />
                                                    <c:if test="${not empty o.customerStudentId and not empty o.customerPhone}"> · </c:if>
                                                    <c:out value="${o.customerPhone}" />
                                                </div>
                                            </c:when>
                                            <c:otherwise><span class="hint">Khách vãng lai</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty o.buildingName}"><c:out value="${o.buildingName}" /></c:when>
                                            <c:otherwise><span class="hint">Tại quầy</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true" />₫</td>
                                    <td>
                                        ${o.paymentMethod.displayName}
                                        <div class="hint">
                                            <c:choose>
                                                <c:when test="${o.paymentStatus == 'PAID'}">Đã thanh toán</c:when>
                                                <c:otherwise>Chưa thanh toán</c:otherwise>
                                            </c:choose>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge badge-${fn:toLowerCase(o.orderStatus)}">${o.orderStatus.displayName}</span>
                                        <c:if test="${o.refunded}"><div class="hint">Đã hoàn tiền</div></c:if>
                                    </td>
                                    <td><c:out value="${o.createdAtDisplay}" /></td>
                                    <td class="table-actions">
                                        <a class="btn btn-sm btn-secondary" href="${ctx}/admin/orders/detail?id=${o.orderId}">Chi tiết</a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>

                <c:if test="${totalPages > 1}">
                    <%-- Carries every active filter into the pager so page 2 shows page 2 OF THE
                         FILTERED SET, not page 2 of everything. --%>
                    <c:set var="filterQs">
                        <c:if test="${not empty qText}">&amp;q=${fn:escapeXml(qText)}</c:if>
                        <c:if test="${not empty qStatus}">&amp;status=${fn:escapeXml(qStatus)}</c:if>
                        <c:if test="${not empty qPayment}">&amp;payment=${fn:escapeXml(qPayment)}</c:if>
                        <c:if test="${not empty qChannel}">&amp;channel=${fn:escapeXml(qChannel)}</c:if>
                        <c:if test="${not empty qFrom}">&amp;from=${fn:escapeXml(qFrom)}</c:if>
                        <c:if test="${not empty qTo}">&amp;to=${fn:escapeXml(qTo)}</c:if>
                    </c:set>
                    <nav class="pagination" aria-label="Phân trang đơn hàng">
                        <c:choose>
                            <c:when test="${page > 1}">
                                <a class="pagination-link" href="${ctx}/admin/orders?page=${page - 1}${filterQs}">&#8249; Trước</a>
                            </c:when>
                            <c:otherwise><span class="pagination-link is-disabled">&#8249; Trước</span></c:otherwise>
                        </c:choose>
                        <span class="pagination-link is-current" aria-current="page">Trang ${page} / ${totalPages}</span>
                        <c:choose>
                            <c:when test="${page < totalPages}">
                                <a class="pagination-link" href="${ctx}/admin/orders?page=${page + 1}${filterQs}">Sau &#8250;</a>
                            </c:when>
                            <c:otherwise><span class="pagination-link is-disabled">Sau &#8250;</span></c:otherwise>
                        </c:choose>
                    </nav>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
