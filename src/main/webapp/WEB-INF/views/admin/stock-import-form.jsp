<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="editing" value="${not empty stockImport}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <div class="page-head">
            <h1>
                <c:choose>
                    <c:when test="${editing}">Sửa phiếu <c:out value="${stockImport.code}" /></c:when>
                    <c:otherwise>Tạo phiếu nhập hàng</c:otherwise>
                </c:choose>
            </h1>
            <div class="page-head-actions">
                <a class="btn btn-secondary" href="${ctx}/admin/stock-imports">Quay lại danh sách</a>
            </div>
        </div>

        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <p class="hint" style="margin-bottom:18px;">
            Lập phiếu chỉ ghi nhận việc đặt hàng — <strong>kho chưa cộng</strong>. Số lượng ở đây là
            số <strong>đặt</strong>; số thực nhận sẽ điền ở bước Kiểm hàng khi hàng về.
        </p>

        <form method="post" action="${ctx}/admin/stock-imports/save" id="import-form">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <c:if test="${editing}">
                <input type="hidden" name="importId" value="${stockImport.importId}">
            </c:if>

            <div class="toolbar">
                <div class="toolbar-field">
                    <label for="supplierId">Nhà cung cấp</label>
                    <select id="supplierId" name="supplierId">
                        <option value="">— Không ghi —</option>
                        <c:forEach var="sup" items="${suppliers}">
                            <option value="${sup.supplierId}"
                                ${editing and stockImport.supplierId == sup.supplierId ? 'selected' : ''}>
                                <c:out value="${sup.name}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="toolbar-field">
                    <label for="expectedDate">Ngày hẹn giao</label>
                    <input type="date" id="expectedDate" name="expectedDate"
                           value="${editing ? stockImport.expectedDateInput : ''}">
                </div>
                <div class="toolbar-field" style="flex:1 1 240px;">
                    <label for="note">Ghi chú</label>
                    <input type="text" id="note" name="note" placeholder="Số hóa đơn, người giao..."
                           value="<c:out value='${editing ? stockImport.note : ""}'/>">
                </div>
            </div>

            <div class="table-scroll">
                <table class="data-table" id="line-table">
                    <thead>
                        <tr>
                            <th style="min-width:220px;">Sản phẩm</th>
                            <th style="width:120px;">SL đặt</th>
                            <th style="width:160px;">Đơn giá nhập (₫)</th>
                            <th style="width:60px;"></th>
                        </tr>
                    </thead>
                    <tbody id="line-body">
                        <%-- Các dòng đã có khi sửa, rồi ba dòng trống. Dòng trống là cách form mời
                             thêm hàng mà không cần JavaScript; server bỏ qua dòng để trống. --%>
                        <c:forEach var="item" items="${items}" varStatus="st">
                            <tr class="line-row">
                                <td>
                                    <label class="visually-hidden" for="p-${st.index}">Sản phẩm dòng ${st.index + 1}</label>
                                    <select id="p-${st.index}" name="productId">
                                        <option value="">— Bỏ trống —</option>
                                        <c:forEach var="p" items="${products}">
                                            <option value="${p.productId}" ${p.productId == item.productId ? 'selected' : ''}>
                                                <c:out value="${p.name}" />
                                            </option>
                                        </c:forEach>
                                    </select>
                                </td>
                                <td>
                                    <label class="visually-hidden" for="q-${st.index}">Số lượng dòng ${st.index + 1}</label>
                                    <input type="number" id="q-${st.index}" name="quantity" min="1" step="1"
                                           class="input-tiny" value="${item.quantity}">
                                </td>
                                <td>
                                    <label class="visually-hidden" for="c-${st.index}">Đơn giá dòng ${st.index + 1}</label>
                                    <input type="number" id="c-${st.index}" name="unitCost" min="0" step="500"
                                           class="input-mid" value="<fmt:formatNumber value='${item.unitCost}' type='number' groupingUsed='false'/>">
                                </td>
                                <td class="table-actions">
                                    <button type="button" class="btn btn-sm btn-secondary js-remove-line"
                                            aria-label="Xóa dòng ${st.index + 1}">×</button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:forEach begin="1" end="3" var="blank">
                            <tr class="line-row">
                                <td>
                                    <label class="visually-hidden" for="pn-${blank}">Sản phẩm dòng trống ${blank}</label>
                                    <select id="pn-${blank}" name="productId">
                                        <option value="">— Bỏ trống —</option>
                                        <c:forEach var="p" items="${products}">
                                            <option value="${p.productId}"><c:out value="${p.name}" /></option>
                                        </c:forEach>
                                    </select>
                                </td>
                                <td>
                                    <label class="visually-hidden" for="qn-${blank}">Số lượng dòng trống ${blank}</label>
                                    <input type="number" id="qn-${blank}" name="quantity" min="1" step="1" class="input-tiny">
                                </td>
                                <td>
                                    <label class="visually-hidden" for="cn-${blank}">Đơn giá dòng trống ${blank}</label>
                                    <input type="number" id="cn-${blank}" name="unitCost" min="0" step="500" class="input-mid">
                                </td>
                                <td class="table-actions">
                                    <button type="button" class="btn btn-sm btn-secondary js-remove-line"
                                            aria-label="Xóa dòng trống ${blank}">×</button>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>

            <p style="margin:10px 0 20px;">
                <button type="button" class="btn btn-secondary btn-sm" id="add-line">+ Thêm dòng</button>
                <span class="hint">Dòng để trống sẽ được bỏ qua.</span>
            </p>

            <div class="toolbar">
                <div class="toolbar-field">
                    <label for="discountAmount">Chiết khấu (₫)</label>
                    <input type="number" id="discountAmount" name="discountAmount" min="0" step="1000"
                           class="input-mid"
                           value="<fmt:formatNumber value='${editing ? stockImport.discountAmount : 0}' type='number' groupingUsed='false'/>">
                </div>
                <div class="toolbar-field">
                    <label for="otherCost">Chi phí nhập (₫)</label>
                    <input type="number" id="otherCost" name="otherCost" min="0" step="1000"
                           class="input-mid"
                           value="<fmt:formatNumber value='${editing ? stockImport.otherCost : 0}' type='number' groupingUsed='false'/>">
                </div>
                <div class="toolbar-field">
                    <label for="paidAmount">Đã trả NCC (₫)</label>
                    <input type="number" id="paidAmount" name="paidAmount" min="0" step="1000"
                           class="input-mid"
                           value="<fmt:formatNumber value='${editing ? stockImport.paidAmount : 0}' type='number' groupingUsed='false'/>">
                </div>
            </div>

            <div class="page-head-actions" style="margin-top:18px;">
                <button type="submit" class="btn btn-primary">
                    <c:choose>
                        <c:when test="${editing}">Lưu thay đổi</c:when>
                        <c:otherwise>Tạo phiếu</c:otherwise>
                    </c:choose>
                </button>
                <a class="btn btn-secondary" href="${ctx}/admin/stock-imports">Hủy</a>
            </div>
        </form>
    </div>
</main>

<script>
/* Thêm/bớt dòng. Form đã chạy được khi không có JavaScript nhờ ba dòng trống sẵn; phần này chỉ
   bỏ giới hạn ba dòng đó. Dòng mới nhân bản từ dòng cuối để không phải dựng lại danh sách sản
   phẩm bằng chuỗi — danh sách ấy có thể dài hàng trăm món. */
(function () {
    var body = document.getElementById('line-body');
    var addBtn = document.getElementById('add-line');
    if (!body || !addBtn) { return; }

    addBtn.addEventListener('click', function () {
        var rows = body.querySelectorAll('.line-row');
        if (!rows.length) { return; }
        var clone = rows[rows.length - 1].cloneNode(true);
        clone.querySelectorAll('select, input').forEach(function (field) {
            field.value = '';
            // id trùng nhau sẽ làm <label for> trỏ nhầm ô; bỏ hẳn id và label ẩn đi kèm.
            field.removeAttribute('id');
        });
        clone.querySelectorAll('label').forEach(function (label) { label.remove(); });
        body.appendChild(clone);
        var firstField = clone.querySelector('select');
        if (firstField) { firstField.focus(); }
    });

    body.addEventListener('click', function (event) {
        var btn = event.target.closest('.js-remove-line');
        if (!btn) { return; }
        var rows = body.querySelectorAll('.line-row');
        // Luôn chừa lại một dòng: xóa hết sẽ để người dùng đối diện một bảng trống không có cách
        // nào thêm lại ngoài việc tải lại trang.
        if (rows.length <= 1) {
            btn.closest('.line-row').querySelectorAll('select, input').forEach(function (f) { f.value = ''; });
            return;
        }
        btn.closest('.line-row').remove();
    });
})();
</script>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
