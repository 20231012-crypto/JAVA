<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <div class="page-head">
            <h1>
                Phiếu <c:out value="${stockImport.code}" />
                <span class="badge ${stockImport.status.badgeClass}">${stockImport.status.displayName}</span>
            </h1>
            <div class="page-head-actions">
                <c:if test="${stockImport.status.editable}">
                    <a class="btn btn-secondary" href="${ctx}/admin/stock-imports/form?id=${stockImport.importId}">Sửa phiếu</a>
                </c:if>
                <a class="btn btn-secondary" href="${ctx}/admin/stock-imports">Quay lại danh sách</a>
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

        <p class="hint">${stockImport.status.hint}</p>

        <%-- ===== Thông tin phiếu ===== --%>
        <div class="table-scroll" style="margin-top:16px;">
            <table class="data-table">
                <tbody>
                    <tr>
                        <th style="width:180px;">Nhà cung cấp</th>
                        <td>
                            <c:choose>
                                <c:when test="${not empty stockImport.supplierName}"><c:out value="${stockImport.supplierName}" /></c:when>
                                <c:otherwise><span class="hint">Không ghi</span></c:otherwise>
                            </c:choose>
                        </td>
                        <th style="width:180px;">Người lập</th>
                        <td><c:out value="${stockImport.adminName}" /> · <c:out value="${stockImport.importedAtDisplay}" /></td>
                    </tr>
                    <tr>
                        <th>Ngày hẹn giao</th>
                        <td>
                            <c:choose>
                                <c:when test="${not empty stockImport.expectedDate}"><c:out value="${stockImport.expectedDateDisplay}" /></c:when>
                                <c:otherwise><span class="hint">Không hẹn</span></c:otherwise>
                            </c:choose>
                        </td>
                        <th>Người nhận hàng</th>
                        <td>
                            <c:choose>
                                <c:when test="${not empty stockImport.receivedByName}">
                                    <c:out value="${stockImport.receivedByName}" /> · <c:out value="${stockImport.receivedAtDisplay}" />
                                </c:when>
                                <c:otherwise><span class="hint">Chưa nhận hàng</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <c:if test="${not empty stockImport.note}">
                        <tr>
                            <th>Ghi chú</th>
                            <td colspan="3"><c:out value="${stockImport.note}" /></td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>

        <%-- ===== Kiểm hàng =====
             Cả bảng nằm trong một form. Ô "SL thực nhận" mặc định bằng số đặt, vì giao đủ là
             trường hợp thường gặp nhất — người kiểm chỉ phải sửa những dòng bị thiếu. --%>
        <h2 style="margin-top:28px;">
            <c:choose>
                <c:when test="${receiving}">Kiểm hàng</c:when>
                <c:otherwise>Chi tiết hàng hóa</c:otherwise>
            </c:choose>
        </h2>

        <form method="post" action="${ctx}/admin/stock-imports/receive">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="importId" value="${stockImport.importId}">

            <div class="table-scroll">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Sản phẩm</th>
                            <th style="width:90px;">SL đặt</th>
                            <th style="width:100px;">Đã nhận</th>
                            <c:if test="${receiving}"><th style="width:130px;">SL thực nhận</th></c:if>
                            <th style="width:130px;">Đơn giá</th>
                            <th style="width:140px;">Thành tiền</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="item" items="${items}">
                            <tr>
                                <td>
                                    <c:out value="${item.productName}" />
                                    <div class="hint">Tồn kho hiện tại: ${item.currentStock}</div>
                                </td>
                                <td>${item.quantity}</td>
                                <td>
                                    ${item.receivedQuantity}
                                    <c:if test="${item.pendingQuantity > 0 and stockImport.status.open}">
                                        <div class="hint">Thiếu ${item.pendingQuantity}</div>
                                    </c:if>
                                </td>
                                <c:if test="${receiving}">
                                    <td>
                                        <label class="visually-hidden" for="rcv-${item.importItemId}">
                                            Số thực nhận của <c:out value="${item.productName}" />
                                        </label>
                                        <input type="number" id="rcv-${item.importItemId}"
                                               name="received_${item.importItemId}"
                                               class="input-tiny"
                                               min="${item.receivedQuantity}" max="${item.quantity}" step="1"
                                               value="${item.quantity}">
                                    </td>
                                </c:if>
                                <td><fmt:formatNumber value="${item.unitCost}" type="number" groupingUsed="true" />₫</td>
                                <td><fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true" />₫</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                    <tfoot>
                        <tr>
                            <th colspan="${receiving ? 5 : 4}" style="text-align:right;">Tiền hàng</th>
                            <td><fmt:formatNumber value="${stockImport.subtotal}" type="number" groupingUsed="true" />₫</td>
                        </tr>
                        <c:if test="${stockImport.discountAmount > 0}">
                            <tr>
                                <th colspan="${receiving ? 5 : 4}" style="text-align:right;">Chiết khấu</th>
                                <td>−<fmt:formatNumber value="${stockImport.discountAmount}" type="number" groupingUsed="true" />₫</td>
                            </tr>
                        </c:if>
                        <c:if test="${stockImport.otherCost > 0}">
                            <tr>
                                <th colspan="${receiving ? 5 : 4}" style="text-align:right;">Chi phí nhập</th>
                                <td>+<fmt:formatNumber value="${stockImport.otherCost}" type="number" groupingUsed="true" />₫</td>
                            </tr>
                        </c:if>
                        <tr>
                            <th colspan="${receiving ? 5 : 4}" style="text-align:right;">Tổng phải trả</th>
                            <td><strong><fmt:formatNumber value="${stockImport.total}" type="number" groupingUsed="true" />₫</strong></td>
                        </tr>
                        <tr>
                            <th colspan="${receiving ? 5 : 4}" style="text-align:right;">Đã trả</th>
                            <td><fmt:formatNumber value="${stockImport.paidAmount}" type="number" groupingUsed="true" />₫</td>
                        </tr>
                        <tr>
                            <th colspan="${receiving ? 5 : 4}" style="text-align:right;">Còn nợ</th>
                            <td>
                                <c:choose>
                                    <c:when test="${stockImport.fullyPaid}"><span class="hint">Đã trả đủ</span></c:when>
                                    <c:otherwise>
                                        <strong><fmt:formatNumber value="${stockImport.debt}" type="number" groupingUsed="true" />₫</strong>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </tfoot>
                </table>
            </div>

            <c:if test="${receiving}">
                <div class="alert alert-info" style="margin-top:16px;">
                    Số thực nhận đang để bằng số đặt. Chỉ sửa những dòng nhà cung cấp giao thiếu,
                    rồi bấm <strong>Xác nhận nhập kho</strong> — kho chỉ cộng đúng số bạn để ở đây.
                </div>
                <div class="page-head-actions" style="margin-top:14px;">
                    <button type="submit" class="btn btn-primary">Xác nhận nhập kho</button>
                </div>
            </c:if>
        </form>

        <%-- ===== Các thao tác khác, mỗi cái một form riêng =====
             Không lồng form vào nhau (HTML cấm), và mỗi nút gửi đúng dữ liệu của nó. --%>
        <div class="page-head-actions" style="margin-top:20px; align-items:flex-start;">
            <c:if test="${stockImport.status == 'PARTIAL'}">
                <form method="post" action="${ctx}/admin/stock-imports/finish"
                      onsubmit="return confirm('Kết thúc phiếu ${stockImport.code}? Phần hàng còn thiếu sẽ không còn tính là đang về.');">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="importId" value="${stockImport.importId}">
                    <button type="submit" class="btn btn-secondary">Kết thúc phiếu</button>
                </form>
            </c:if>
            <c:if test="${stockImport.status == 'DRAFT'}">
                <form method="post" action="${ctx}/admin/stock-imports/cancel"
                      onsubmit="return confirm('Hủy phiếu ${stockImport.code}?');">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="importId" value="${stockImport.importId}">
                    <button type="submit" class="btn btn-secondary">Hủy phiếu</button>
                </form>
            </c:if>
            <c:if test="${stockImport.status.deletable}">
                <form method="post" action="${ctx}/admin/stock-imports/delete"
                      onsubmit="return confirm('Xóa hẳn phiếu ${stockImport.code}? Không khôi phục được.');">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="importId" value="${stockImport.importId}">
                    <button type="submit" class="btn btn-danger">Xóa phiếu</button>
                </form>
            </c:if>
        </div>

        <%-- ===== Thanh toán nhà cung cấp ===== --%>
        <c:if test="${stockImport.status != 'CANCELLED'}">
            <h2 style="margin-top:28px;">Thanh toán nhà cung cấp</h2>
            <form method="post" action="${ctx}/admin/stock-imports/pay" class="toolbar">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="importId" value="${stockImport.importId}">
                <div class="toolbar-field">
                    <label for="paidAmount">Tổng đã trả (₫)</label>
                    <input type="number" id="paidAmount" name="paidAmount" min="0" step="1000" class="input-mid"
                           value="<fmt:formatNumber value='${stockImport.paidAmount}' type='number' groupingUsed='false'/>">
                </div>
                <div class="toolbar-actions">
                    <button type="submit" class="btn btn-primary btn-sm">Cập nhật</button>
                </div>
            </form>
            <p class="hint">
                Đây là <em>tổng</em> số tiền đã trả cho phiếu này, không phải số trả thêm lần này.
            </p>
        </c:if>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
