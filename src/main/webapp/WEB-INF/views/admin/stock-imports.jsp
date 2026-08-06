<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Nhập hàng vào kho</h1>

        <form method="post" action="${pageContext.request.contextPath}/admin/stock-imports/save"
              style="margin:20px 0; display:flex; gap:10px; align-items:end; flex-wrap:wrap;">
            <div class="form-group" style="margin-bottom:0;">
                <label for="productId">Sản phẩm</label>
                <select id="productId" name="productId" required>
                    <c:forEach var="p" items="${products}">
                        <option value="${p.productId}"><c:out value="${p.name}" /></option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="quantity">Số lượng</label>
                <input type="number" id="quantity" name="quantity" min="1" required>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="unitCost">Đơn giá nhập (₫)</label>
                <input type="number" id="unitCost" name="unitCost" min="0" step="1000" required>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="supplierName">Nhà cung cấp</label>
                <input type="text" id="supplierName" name="supplierName">
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="note">Ghi chú</label>
                <input type="text" id="note" name="note">
            </div>
            <button type="submit" class="btn btn-primary">Ghi nhận nhập hàng</button>
        </form>

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
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
