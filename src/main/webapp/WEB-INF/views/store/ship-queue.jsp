<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Đơn cần giao</h1>

        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <c:choose>
            <c:when test="${empty orders}">
                <div class="empty-state"><h2>Không có đơn nào cần xử lý</h2></div>
            </c:when>
            <c:otherwise>
                <table class="data-table" style="margin-top:20px;">
                    <thead>
                        <tr><th>Mã đơn</th><th>Tòa nhà</th><th>Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th><th></th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="o" items="${orders}">
                            <tr>
                                <td>#${o.orderId}</td>
                                <td><c:out value="${o.buildingName}" /></td>
                                <td><fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true" />₫</td>
                                <td><c:out value="${o.paymentStatus.displayName}" /></td>
                                <td><span class="badge badge-${fn:toLowerCase(o.orderStatus)}"><c:out value="${o.orderStatus.displayName}" /></span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${o.orderStatus == 'CONFIRMED'}">
                                            <form method="post" action="${pageContext.request.contextPath}/store/orders/pick">
                                                <input type="hidden" name="orderId" value="${o.orderId}">
                                                <button type="submit" class="btn btn-sm btn-primary">Lấy hàng & giao</button>
                                            </form>
                                        </c:when>
                                        <c:when test="${o.orderStatus == 'SHIPPING'}">
                                            <form method="post" action="${pageContext.request.contextPath}/store/orders/complete">
                                                <input type="hidden" name="orderId" value="${o.orderId}">
                                                <button type="submit" class="btn btn-sm btn-secondary">Đã giao xong</button>
                                            </form>
                                        </c:when>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
