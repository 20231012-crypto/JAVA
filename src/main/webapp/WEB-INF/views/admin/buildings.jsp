<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Tòa nhà & phí ship</h1>

        <form method="post" action="${pageContext.request.contextPath}/admin/buildings/save"
              style="margin:20px 0; display:flex; gap:10px; align-items:end; flex-wrap:wrap;">
            <div class="form-group" style="margin-bottom:0;">
                <label for="name">Tên tòa nhà</label>
                <input type="text" id="name" name="name" required>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="description">Mô tả</label>
                <input type="text" id="description" name="description">
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="shippingFee">Phí ship (₫)</label>
                <input type="number" id="shippingFee" name="shippingFee" min="0" step="1000" required>
            </div>
            <button type="submit" class="btn btn-primary">Thêm tòa nhà</button>
        </form>

        <table class="data-table">
            <thead>
                <tr><th>Tên</th><th>Mô tả</th><th>Phí ship</th><th>Trạng thái</th><th></th></tr>
            </thead>
            <tbody>
                <c:forEach var="b" items="${buildings}">
                    <tr>
                        <td><c:out value="${b.name}" /></td>
                        <td><c:out value="${b.description}" /></td>
                        <td><fmt:formatNumber value="${b.shippingFee}" type="number" groupingUsed="true" />₫</td>
                        <td>
                            <c:choose>
                                <c:when test="${b.active}"><span class="badge badge-completed">Đang hoạt động</span></c:when>
                                <c:otherwise><span class="badge badge-cancelled">Đã ẩn</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/admin/buildings/toggle">
                                <input type="hidden" name="buildingId" value="${b.buildingId}">
                                <input type="hidden" name="active" value="${!b.active}">
                                <button type="submit" class="btn btn-sm ${b.active ? 'btn-danger' : 'btn-secondary'}">
                                    ${b.active ? 'Ẩn' : 'Hiện lại'}
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
