<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Nhập hàng vào kho</h1>

        <c:if test="${not empty sessionScope.actionMessage}">
            <div class="alert alert-success"><c:out value="${sessionScope.actionMessage}" /></div>
            <c:remove var="actionMessage" scope="session" />
        </c:if>
        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <%-- One receipt, several lines. The van that arrives with eight products is one delivery
             from one supplier, and recording it as eight separate receipts loses that. Five blank
             rows are offered up front; empty ones are skipped server-side, so there is nothing to
             add or remove and the form needs no JavaScript at all. --%>
        <form method="post" action="${pageContext.request.contextPath}/admin/stock-imports/save">
            <input type="hidden" name="csrfToken" value="${csrfToken}">

            <div class="toolbar">
                <div class="toolbar-field">
                    <label for="supplierName">Nhà cung cấp</label>
                    <input type="text" id="supplierName" name="supplierName" placeholder="Không bắt buộc">
                </div>
                <div class="toolbar-field" style="flex:1 1 240px;">
                    <label for="note">Ghi chú phiếu</label>
                    <input type="text" id="note" name="note" placeholder="Số hóa đơn, người giao...">
                </div>
            </div>

            <div class="table-scroll">
                <table class="data-table">
                    <thead>
                        <tr><th>Sản phẩm</th><th>Số lượng</th><th>Đơn giá nhập (₫)</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach begin="1" end="5" var="row">
                            <tr>
                                <td>
                                    <label class="visually-hidden" for="imp-product-${row}">Sản phẩm dòng ${row}</label>
                                    <select id="imp-product-${row}" name="productId">
                                        <option value="">— Bỏ trống —</option>
                                        <c:forEach var="p" items="${products}">
                                            <option value="${p.productId}"><c:out value="${p.name}" /></option>
                                        </c:forEach>
                                    </select>
                                </td>
                                <td>
                                    <label class="visually-hidden" for="imp-qty-${row}">Số lượng dòng ${row}</label>
                                    <input type="number" id="imp-qty-${row}" name="quantity"
                                           min="0" step="1" class="input-tiny">
                                </td>
                                <td>
                                    <label class="visually-hidden" for="imp-cost-${row}">Đơn giá dòng ${row}</label>
                                    <input type="number" id="imp-cost-${row}" name="unitCost"
                                           min="0" step="1000" class="input-mid">
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>

            <button type="submit" class="btn btn-primary" style="margin-top:14px;">Ghi nhận phiếu nhập</button>
        </form>

        <div class="table-scroll">
            <table class="data-table">
            <thead>
                <tr><th>Mã phiếu</th><th>Thời gian</th><th>Người nhập</th><th>Nhà cung cấp</th><th>Ghi chú</th></tr>
            </thead>
            <tbody>
                <c:forEach var="imp" items="${imports}">
                    <tr>
                        <td>#${imp.importId}</td>
                        <td><c:out value="${imp.importedAtDisplay}" /></td>
                        <td><c:out value="${imp.adminName}" /></td>
                        <td><c:out value="${imp.supplierName}" /></td>
                        <td><c:out value="${imp.note}" /></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
