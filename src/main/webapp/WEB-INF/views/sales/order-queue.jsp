<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Đơn hàng</h1>

        <div class="filter-bar">
            <a class="filter-chip ${selectedStatus == 'PENDING' ? 'active' : ''}" href="?status=PENDING">Chờ xác nhận</a>
            <a class="filter-chip ${selectedStatus == 'CONFIRMED' ? 'active' : ''}" href="?status=CONFIRMED">Đã xác nhận</a>
            <a class="filter-chip ${selectedStatus == 'SHIPPING' ? 'active' : ''}" href="?status=SHIPPING">Đang giao</a>
            <a class="filter-chip ${selectedStatus == 'COMPLETED' ? 'active' : ''}" href="?status=COMPLETED">Hoàn thành</a>
            <a class="filter-chip ${selectedStatus == 'REJECTED' ? 'active' : ''}" href="?status=REJECTED">Bị từ chối</a>
            <a class="filter-chip ${selectedStatus == 'CANCELLED' ? 'active' : ''}" href="?status=CANCELLED">Đã hủy</a>
            <a class="filter-chip ${selectedStatus == 'ALL' ? 'active' : ''}" href="?status=ALL">Tất cả</a>
        </div>

        <c:choose>
            <c:when test="${empty orders}">
                <div class="empty-state"><h2>Không có đơn hàng nào</h2></div>
            </c:when>
            <c:otherwise>
                <table class="data-table">
                    <thead>
                        <tr><th>Mã đơn</th><th>Kênh</th><th>Khách/Tòa nhà</th><th>Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th><th></th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="o" items="${orders}">
                            <tr>
                                <td>#${o.orderId}</td>
                                <td>${o.channel == 'ONLINE' ? 'Web' : 'Tại quầy'}</td>
                                <td><c:out value="${o.buildingName}" /></td>
                                <td><fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true" />₫</td>
                                <td><c:out value="${o.paymentStatus.displayName}" /></td>
                                <td><span class="badge badge-${fn:toLowerCase(o.orderStatus)}"><c:out value="${o.orderStatus.displayName}" /></span></td>
                                <td><a href="${pageContext.request.contextPath}/sales/orders/detail?id=${o.orderId}">Xem</a></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
