<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Đơn hàng của tôi</h1>

        <c:choose>
            <c:when test="${empty orders}">
                <div class="empty-state">
                    <h2>Bạn chưa có đơn hàng nào</h2>
                    <p style="margin-top:16px;">
                        <a class="btn btn-primary" href="${pageContext.request.contextPath}/products">Đặt món ngay</a>
                    </p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-scroll">
                    <table class="data-table" style="margin-top:20px;">
                    <thead>
                        <tr>
                            <th>Mã đơn</th>
                            <th>Ngày đặt</th>
                            <th>Tòa nhà</th>
                            <th>Tổng tiền</th>
                            <th>Trạng thái</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="o" items="${orders}">
                            <tr>
                                <td><c:out value="${o.orderCode}" /></td>
                                <td><c:out value="${o.createdAtDisplay}" /></td>
                                <td><c:out value="${o.buildingName}" /></td>
                                <td><fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true" />₫</td>
                                <td><span class="badge badge-${fn:toLowerCase(o.orderStatus)}"><c:out value="${o.orderStatus.displayName}" /></span></td>
                                <td><a href="${pageContext.request.contextPath}/orders/detail?id=${o.orderId}">Xem chi tiết</a></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
