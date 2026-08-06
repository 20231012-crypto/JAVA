<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Danh mục sản phẩm</h1>

        <form method="post" action="${pageContext.request.contextPath}/admin/categories/save" style="margin:20px 0; display:flex; gap:10px; align-items:end;">
            <div class="form-group" style="flex:1; margin-bottom:0;">
                <label for="name">Tên danh mục mới</label>
                <input type="text" id="name" name="name" required>
            </div>
            <button type="submit" class="btn btn-primary">Thêm danh mục</button>
        </form>

        <table class="data-table">
            <thead>
                <tr><th>Tên danh mục</th><th>Trạng thái</th><th></th></tr>
            </thead>
            <tbody>
                <c:forEach var="cat" items="${categories}">
                    <tr>
                        <td><c:out value="${cat.name}" /></td>
                        <td>
                            <c:choose>
                                <c:when test="${cat.active}"><span class="badge badge-completed">Đang hoạt động</span></c:when>
                                <c:otherwise><span class="badge badge-cancelled">Đã ẩn</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/admin/categories/toggle">
                                <input type="hidden" name="categoryId" value="${cat.categoryId}">
                                <input type="hidden" name="active" value="${!cat.active}">
                                <button type="submit" class="btn btn-sm ${cat.active ? 'btn-danger' : 'btn-secondary'}">
                                    ${cat.active ? 'Ẩn' : 'Hiện lại'}
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
