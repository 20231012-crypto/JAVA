<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Tồn kho &amp; sổ kho</h1>

        <c:if test="${not empty sessionScope.actionMessage}">
            <div class="alert alert-success"><c:out value="${sessionScope.actionMessage}" /></div>
            <c:remove var="actionMessage" scope="session" />
        </c:if>
        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <c:if test="${not empty lowStock}">
            <div class="alert alert-error">
                <strong>${fn:length(lowStock)} món sắp hết</strong> —
                <c:forEach var="p" items="${lowStock}" varStatus="loop">
                    <c:out value="${p.name}" /> (còn ${p.shelfQuantity}/${p.lowStockThreshold})<c:if test="${not loop.last}">, </c:if>
                </c:forEach>
            </div>
        </c:if>

        <h2>Điều chỉnh tồn kho</h2>
        <%-- Only the two manual reasons are offered. A sale or a transfer is recorded by the
             operation that performed it, so listing them here would let someone write a movement
             for something that never happened. --%>
        <form class="toolbar" method="post" action="${ctx}/admin/inventory/adjust">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <div class="toolbar-field">
                <label for="adj-product">Món</label>
                <select id="adj-product" name="productId" required>
                    <c:forEach var="p" items="${products}">
                        <option value="${p.productId}">
                            <c:out value="${p.name}" /> — kệ ${p.shelfQuantity}, kho ${p.warehouseQuantity}
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="toolbar-field">
                <label for="adj-location">Nơi</label>
                <select id="adj-location" name="location">
                    <option value="SHELF">Kệ bán</option>
                    <option value="WAREHOUSE">Kho</option>
                </select>
            </div>
            <div class="toolbar-field">
                <label for="adj-reason">Loại</label>
                <select id="adj-reason" name="reason">
                    <option value="WRITE_OFF">Hủy hàng (hỏng, hết hạn)</option>
                    <option value="STOCK_TAKE">Kiểm kê (nhập số đếm thực tế)</option>
                </select>
            </div>
            <div class="toolbar-field">
                <label for="adj-amount">Số lượng</label>
                <input type="number" id="adj-amount" name="amount" min="0" step="1" required>
            </div>
            <div class="toolbar-field">
                <label for="adj-note">Ghi chú</label>
                <input type="text" id="adj-note" name="note" maxlength="200" placeholder="Không bắt buộc">
            </div>
            <div class="toolbar-actions">
                <button type="submit" class="btn btn-primary btn-sm">Ghi nhận</button>
            </div>
        </form>
        <p class="hint">
            Với <strong>Hủy hàng</strong>, số lượng là số bị bỏ đi. Với <strong>Kiểm kê</strong>,
            số lượng là số đếm được thực tế — hệ thống tự tính phần chênh lệch.
        </p>

        <h2>Tồn kho theo món</h2>
        <div class="table-scroll">
            <table class="data-table">
                <thead>
                    <tr><th>Món</th><th>Tồn kho</th><th>Đang về</th><th>Tồn kệ</th><th>Ngưỡng cảnh báo</th><th>Trạng thái</th></tr>
                </thead>
                <tbody>
                    <c:forEach var="p" items="${products}">
                        <tr>
                            <td><c:out value="${p.name}" /></td>
                            <td>${p.warehouseQuantity}</td>
                            <td>
                                <%-- Số đã đặt nhà cung cấp mà chưa nhận. Có nó thì "sắp hết" mới đọc
                                     được đúng: hết hàng nhưng chiều nay có 50 thùng về là chuyện khác
                                     hẳn với hết hàng và chưa đặt gì. --%>
                                <c:choose>
                                    <c:when test="${incoming[p.productId] > 0}">
                                        <a href="${ctx}/admin/stock-imports?status=DRAFT">+${incoming[p.productId]}</a>
                                    </c:when>
                                    <c:otherwise><span class="hint">—</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                ${p.shelfQuantity}
                                <c:if test="${p.active and p.lowStock}">
                                    <span class="badge badge-rejected">Sắp hết</span>
                                </c:if>
                            </td>
                            <td>
                                <%-- One row, one form: a per-row save keeps a mistyped threshold on
                                     one dish from blocking every other change on the page. --%>
                                <form method="post" action="${ctx}/admin/inventory/threshold" class="inline-form">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="productId" value="${p.productId}">
                                    <label class="visually-hidden" for="th-${p.productId}">Ngưỡng cảnh báo cho <c:out value="${p.name}" /></label>
                                    <input type="number" id="th-${p.productId}" name="threshold"
                                           value="${p.lowStockThreshold}" min="0" step="1" class="input-tiny">
                                    <button type="submit" class="btn btn-sm btn-secondary">Lưu</button>
                                </form>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${not p.active}"><span class="badge badge-cancelled">Đã ẩn</span></c:when>
                                    <c:when test="${not p.available}"><span class="badge badge-pending">Hết hàng</span></c:when>
                                    <c:otherwise><span class="badge badge-completed">Đang bán</span></c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>

        <h2>Sổ kho</h2>
        <form class="toolbar" method="get" action="${ctx}/admin/inventory">
            <div class="toolbar-field">
                <label for="led-product">Món</label>
                <select id="led-product" name="productId">
                    <option value="">Tất cả</option>
                    <c:forEach var="p" items="${products}">
                        <option value="${p.productId}" ${qProductId == p.productId ? 'selected' : ''}><c:out value="${p.name}" /></option>
                    </c:forEach>
                </select>
            </div>
            <div class="toolbar-field">
                <label for="led-reason">Loại</label>
                <select id="led-reason" name="reason">
                    <option value="">Tất cả</option>
                    <c:forEach var="r" items="${reasons}">
                        <option value="${r}" ${qReason == r ? 'selected' : ''}>${r.displayName}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="toolbar-actions">
                <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
            </div>
        </form>

        <c:choose>
            <c:when test="${empty movements}">
                <div class="empty-state">
                    <h2>Chưa có chuyển động kho nào</h2>
                    <p>Mọi lần nhập hàng, chuyển kệ, bán ra hoặc điều chỉnh sẽ được ghi lại ở đây.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-scroll">
                    <table class="data-table">
                        <thead>
                            <tr><th>Thời gian</th><th>Món</th><th>Nơi</th><th>Thay đổi</th><th>Lý do</th><th>Ghi chú</th><th>Người thực hiện</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="m" items="${movements}">
                                <tr>
                                    <td><c:out value="${m.createdAtDisplay}" /></td>
                                    <td><c:out value="${m.productName}" /></td>
                                    <td>${m.location.displayName}</td>
                                    <td class="${m.increase ? 'delta-up' : 'delta-down'}">${m.deltaDisplay}</td>
                                    <td>${m.reason.displayName}</td>
                                    <td>
                                        <c:out value="${m.note}" />
                                        <c:if test="${not empty m.refOrderCode}">
                                            <div class="hint">Đơn <c:out value="${m.refOrderCode}" /></div>
                                        </c:if>
                                    </td>
                                    <td><c:out value="${m.createdByName}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>

                <c:if test="${totalPages > 1}">
                    <c:set var="qs">
                        <c:if test="${not empty qProductId}">&amp;productId=${fn:escapeXml(qProductId)}</c:if>
                        <c:if test="${not empty qReason}">&amp;reason=${fn:escapeXml(qReason)}</c:if>
                    </c:set>
                    <nav class="pagination" aria-label="Phân trang sổ kho">
                        <c:choose>
                            <c:when test="${page > 1}"><a class="pagination-link" href="${ctx}/admin/inventory?page=${page - 1}${qs}">&#8249; Trước</a></c:when>
                            <c:otherwise><span class="pagination-link is-disabled">&#8249; Trước</span></c:otherwise>
                        </c:choose>
                        <span class="pagination-link is-current" aria-current="page">Trang ${page} / ${totalPages}</span>
                        <c:choose>
                            <c:when test="${page < totalPages}"><a class="pagination-link" href="${ctx}/admin/inventory?page=${page + 1}${qs}">Sau &#8250;</a></c:when>
                            <c:otherwise><span class="pagination-link is-disabled">Sau &#8250;</span></c:otherwise>
                        </c:choose>
                    </nav>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
