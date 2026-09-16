<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <div class="page-head">
            <h1>Nhập hàng</h1>
            <div class="page-head-actions">
                <a class="btn btn-secondary" href="${ctx}/admin/suppliers">Nhà cung cấp</a>
                <a class="btn btn-primary" href="${ctx}/admin/stock-imports/form">+ Tạo phiếu nhập</a>
            </div>
        </div>

        <c:if test="${not empty sessionScope.actionMessage}">
            <div class="alert alert-success"><c:out value="${sessionScope.actionMessage}" /></div>
            <c:remove var="actionMessage" scope="session" />
        </c:if>
        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <%-- Tab trạng thái. Là link chứ không phải nút JS, nên mỗi tab là một URL chia sẻ được và
             nút Back của trình duyệt hoạt động đúng. --%>
        <nav class="status-tabs" aria-label="Lọc theo trạng thái">
            <a class="status-tab ${empty qStatus ? 'is-active' : ''}" href="${ctx}/admin/stock-imports">
                Tất cả <span class="status-tab-count">${totalImports}</span>
            </a>
            <c:forEach var="s" items="${statuses}">
                <a class="status-tab ${qStatus == s ? 'is-active' : ''}"
                   href="${ctx}/admin/stock-imports?status=${s}" title="${s.hint}">
                    ${s.displayName} <span class="status-tab-count">${statusCounts[s]}</span>
                </a>
            </c:forEach>
        </nav>

        <form class="toolbar" method="get" action="${ctx}/admin/stock-imports">
            <c:if test="${not empty qStatus}"><input type="hidden" name="status" value="${fn:escapeXml(qStatus)}"></c:if>
            <div class="toolbar-field" style="flex:1 1 240px;">
                <label for="f-q">Tìm kiếm</label>
                <input type="search" id="f-q" name="q" value="<c:out value='${qText}'/>"
                       placeholder="Mã phiếu, nhà cung cấp, ghi chú">
            </div>
            <div class="toolbar-field">
                <label for="f-supplier">Nhà cung cấp</label>
                <select id="f-supplier" name="supplier">
                    <option value="">Tất cả</option>
                    <c:forEach var="sup" items="${suppliers}">
                        <option value="${sup.supplierId}" ${qSupplier == sup.supplierId ? 'selected' : ''}>
                            <c:out value="${sup.name}" />
                        </option>
                    </c:forEach>
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
                    <a class="btn btn-secondary btn-sm" href="${ctx}/admin/stock-imports">Xóa lọc</a>
                </c:if>
            </div>
        </form>

        <div class="catalog-toolbar">
            <span class="catalog-count"><strong>${totalImports}</strong> phiếu nhập</span>
        </div>

        <c:choose>
            <c:when test="${empty imports}">
                <div class="empty-state">
                    <h2>Chưa có phiếu nhập nào</h2>
                    <c:choose>
                        <c:when test="${filter.active}">
                            <p>Không có phiếu nào khớp bộ lọc. Thử nới rộng khoảng ngày hoặc bỏ bớt điều kiện.</p>
                        </c:when>
                        <c:otherwise>
                            <p>Lập phiếu khi đặt hàng nhà cung cấp. Kho chỉ cộng sau khi bạn kiểm hàng thực tế.</p>
                            <a class="btn btn-primary" href="${ctx}/admin/stock-imports/form">Tạo phiếu đầu tiên</a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-scroll">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Mã phiếu</th><th>Nhà cung cấp</th><th>Số lượng</th>
                                <th>Tổng tiền</th><th>Công nợ</th><th>Trạng thái</th>
                                <th>Ngày tạo</th><th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="imp" items="${imports}">
                                <tr>
                                    <td>
                                        <strong><c:out value="${imp.code}" /></strong>
                                        <div class="hint">${imp.lineCount} món · <c:out value="${imp.adminName}" /></div>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty imp.supplierName}"><c:out value="${imp.supplierName}" /></c:when>
                                            <c:otherwise><span class="hint">Không ghi</span></c:otherwise>
                                        </c:choose>
                                        <c:if test="${not empty imp.expectedDate}">
                                            <div class="hint">Hẹn giao <c:out value="${imp.expectedDateDisplay}" /></div>
                                        </c:if>
                                    </td>
                                    <td>
                                        <%-- Hai con số cạnh nhau vì chính khoảng chênh giữa chúng là
                                             thứ trang này sinh ra để nhìn thấy. --%>
                                        <strong>${imp.receivedQuantity}</strong> / ${imp.orderedQuantity}
                                        <c:if test="${imp.pendingQuantity > 0 and imp.status.open}">
                                            <div class="hint">Còn ${imp.pendingQuantity} đang về</div>
                                        </c:if>
                                    </td>
                                    <td><fmt:formatNumber value="${imp.total}" type="number" groupingUsed="true" />₫</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${imp.fullyPaid}"><span class="hint">Đã trả đủ</span></c:when>
                                            <c:otherwise>
                                                <fmt:formatNumber value="${imp.debt}" type="number" groupingUsed="true" />₫
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <span class="badge ${imp.status.badgeClass}">${imp.status.displayName}</span>
                                    </td>
                                    <td><c:out value="${imp.importedAtDisplay}" /></td>
                                    <td class="table-actions">
                                        <a class="btn btn-sm btn-secondary" href="${ctx}/admin/stock-imports/detail?id=${imp.importId}">
                                            <c:choose>
                                                <c:when test="${imp.status.open}">Kiểm hàng</c:when>
                                                <c:otherwise>Chi tiết</c:otherwise>
                                            </c:choose>
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>

                <c:if test="${totalPages > 1}">
                    <c:set var="filterQs">
                        <c:if test="${not empty qText}">&amp;q=${fn:escapeXml(qText)}</c:if>
                        <c:if test="${not empty qStatus}">&amp;status=${fn:escapeXml(qStatus)}</c:if>
                        <c:if test="${not empty qSupplier}">&amp;supplier=${fn:escapeXml(qSupplier)}</c:if>
                        <c:if test="${not empty qFrom}">&amp;from=${fn:escapeXml(qFrom)}</c:if>
                        <c:if test="${not empty qTo}">&amp;to=${fn:escapeXml(qTo)}</c:if>
                    </c:set>
                    <nav class="pagination" aria-label="Phân trang phiếu nhập">
                        <c:choose>
                            <c:when test="${page > 1}">
                                <a class="pagination-link" href="${ctx}/admin/stock-imports?page=${page - 1}${filterQs}">&#8249; Trước</a>
                            </c:when>
                            <c:otherwise><span class="pagination-link is-disabled">&#8249; Trước</span></c:otherwise>
                        </c:choose>
                        <span class="pagination-link is-current" aria-current="page">Trang ${page} / ${totalPages}</span>
                        <c:choose>
                            <c:when test="${page < totalPages}">
                                <a class="pagination-link" href="${ctx}/admin/stock-imports?page=${page + 1}${filterQs}">Sau &#8250;</a>
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
