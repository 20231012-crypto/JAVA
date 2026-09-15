<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Chuyển hàng từ kho lên kệ</h1>

        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/store/transfers/save"
              style="margin:20px 0; display:flex; gap:10px; align-items:end; flex-wrap:wrap;">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <div class="form-group" style="margin-bottom:0;">
                <label for="productId">Sản phẩm</label>
                <select id="productId" name="productId" required>
                    <c:forEach var="p" items="${products}">
                        <option value="${p.productId}">
                            <c:out value="${p.name}" /> (kho: ${p.warehouseQuantity}, kệ: ${p.shelfQuantity})
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="quantity">Số lượng chuyển</label>
                <input type="number" id="quantity" name="quantity" min="1" required>
            </div>
            <button type="submit" class="btn btn-primary">Chuyển lên kệ</button>
        </form>

        <table class="data-table">
            <thead>
                <tr><th>Sản phẩm</th><th>Tồn kho</th><th>Tồn kệ</th></tr>
            </thead>
            <tbody>
                <c:forEach var="p" items="${products}">
                    <tr>
                        <td><c:out value="${p.name}" /></td>
                        <td>${p.warehouseQuantity}</td>
                        <td>${p.shelfQuantity}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <h2 style="margin:24px 0 12px;">Lịch sử chuyển hàng gần đây</h2>
        <table class="data-table">
            <thead>
                <tr><th>Thời gian</th><th>Sản phẩm</th><th>Số lượng</th><th>Người thực hiện</th></tr>
            </thead>
            <tbody>
                <c:forEach var="t" items="${transfers}">
                    <tr>
                        <td><c:out value="${t.transferredAtDisplay}" /></td>
                        <td><c:out value="${t.productName}" /></td>
                        <td>${t.quantity}</td>
                        <td><c:out value="${t.storeStaffName}" /></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
