<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Danh mục sản phẩm</h1>
        <p class="hint">Danh mục không chọn "Nhóm cấp trên" là nhóm lớn (cột trong menu Danh mục ở đầu trang). Danh mục có chọn nhóm cấp trên là danh mục con — sản phẩm chỉ gán được vào danh mục con.</p>

        <form method="post" action="${pageContext.request.contextPath}/admin/categories/save" style="margin:20px 0; display:flex; gap:10px; align-items:end; flex-wrap:wrap;">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <div class="form-group" style="flex:1; min-width:200px; margin-bottom:0;">
                <label for="name">Tên danh mục mới</label>
                <input type="text" id="name" name="name" required>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="parentCategoryId">Nhóm cấp trên</label>
                <select id="parentCategoryId" name="parentCategoryId">
                    <option value="">— Là nhóm lớn (không có cấp trên) —</option>
                    <c:forEach var="cat" items="${categories}">
                        <c:if test="${cat.topLevel}">
                            <option value="${cat.categoryId}"><c:out value="${cat.name}" /></option>
                        </c:if>
                    </c:forEach>
                </select>
            </div>
            <button type="submit" class="btn btn-primary">Thêm danh mục</button>
        </form>

        <div class="table-scroll">
            <table class="data-table">
            <thead>
                <tr><th>Tên danh mục</th><th>Nhóm cấp trên</th><th>Trạng thái</th><th></th></tr>
            </thead>
            <tbody>
                <c:forEach var="cat" items="${categories}">
                    <tr>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/admin/categories/save" style="display:flex; gap:6px; align-items:center;">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="categoryId" value="${cat.categoryId}">
                                <input type="text" name="name" value="${cat.name}" style="max-width:220px;">
                                <select name="parentCategoryId">
                                    <option value="">— Là nhóm lớn —</option>
                                    <c:forEach var="p" items="${categories}">
                                        <c:if test="${p.topLevel and p.categoryId != cat.categoryId}">
                                            <option value="${p.categoryId}" ${cat.parentCategoryId == p.categoryId ? 'selected' : ''}><c:out value="${p.name}" /></option>
                                        </c:if>
                                    </c:forEach>
                                </select>
                                <button type="submit" class="btn btn-sm btn-secondary">Lưu</button>
                            </form>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${cat.topLevel}"><span class="hint">(nhóm lớn)</span></c:when>
                                <c:otherwise>
                                    <c:forEach var="p" items="${categories}">
                                        <c:if test="${p.categoryId == cat.parentCategoryId}"><c:out value="${p.name}" /></c:if>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${cat.active}"><span class="badge badge-completed">Đang hoạt động</span></c:when>
                                <c:otherwise><span class="badge badge-cancelled">Đã ẩn</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/admin/categories/toggle">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
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
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
