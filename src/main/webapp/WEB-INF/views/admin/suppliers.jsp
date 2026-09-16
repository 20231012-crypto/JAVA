<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <div class="page-head">
            <h1>Nhà cung cấp</h1>
            <div class="page-head-actions">
                <a class="btn btn-secondary" href="${ctx}/admin/stock-imports">Phiếu nhập hàng</a>
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

        <form method="post" action="${ctx}/admin/suppliers/save" class="toolbar">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <div class="toolbar-field" style="flex:1 1 200px;">
                <label for="name">Tên nhà cung cấp *</label>
                <input type="text" id="name" name="name" required maxlength="150">
            </div>
            <div class="toolbar-field">
                <label for="phone">Điện thoại</label>
                <input type="tel" id="phone" name="phone" maxlength="20">
            </div>
            <div class="toolbar-field">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" maxlength="150">
            </div>
            <div class="toolbar-field" style="flex:1 1 200px;">
                <label for="address">Địa chỉ</label>
                <input type="text" id="address" name="address" maxlength="255">
            </div>
            <div class="toolbar-actions">
                <button type="submit" class="btn btn-primary btn-sm">Thêm</button>
            </div>
        </form>

        <c:choose>
            <c:when test="${empty suppliers}">
                <div class="empty-state">
                    <h2>Chưa có nhà cung cấp nào</h2>
                    <p>Thêm nhà cung cấp ở trên để chọn được khi lập phiếu nhập, và để theo dõi công nợ theo từng bên.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-scroll">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Tên</th><th>Liên hệ</th><th>Số phiếu</th>
                                <th>Tổng đã nhập</th><th>Còn nợ</th><th>Trạng thái</th><th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="sup" items="${suppliers}">
                                <tr>
                                    <td>
                                        <strong><c:out value="${sup.name}" /></strong>
                                        <c:if test="${not empty sup.address}">
                                            <div class="hint"><c:out value="${sup.address}" /></div>
                                        </c:if>
                                    </td>
                                    <td>
                                        <c:out value="${sup.phone}" />
                                        <c:if test="${not empty sup.email}">
                                            <div class="hint"><c:out value="${sup.email}" /></div>
                                        </c:if>
                                    </td>
                                    <td>${sup.importCount}</td>
                                    <td><fmt:formatNumber value="${sup.totalValue}" type="number" groupingUsed="true" />₫</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${sup.totalDebt > 0}">
                                                <strong><fmt:formatNumber value="${sup.totalDebt}" type="number" groupingUsed="true" />₫</strong>
                                            </c:when>
                                            <c:otherwise><span class="hint">Không nợ</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${sup.active}"><span class="badge badge-success">Đang hợp tác</span></c:when>
                                            <c:otherwise><span class="badge badge-muted">Đã ngừng</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="table-actions">
                                        <%-- Không xóa cứng: phiếu nhập cũ trỏ tới hàng này và tên NCC
                                             trên chứng từ cũ vẫn phải đọc được. --%>
                                        <form method="post" action="${ctx}/admin/suppliers/toggle" style="display:inline;">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <input type="hidden" name="supplierId" value="${sup.supplierId}">
                                            <button type="submit" class="btn btn-sm btn-secondary">
                                                ${sup.active ? 'Ngừng' : 'Bật lại'}
                                            </button>
                                        </form>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <p class="hint" style="margin-top:12px;">
                    Công nợ chỉ tính trên phiếu đã nhận hàng. Phiếu còn ở trạng thái "Chưa nhập" là
                    dự định mua, chưa phải khoản nợ.
                </p>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
