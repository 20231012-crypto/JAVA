<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <div style="display:flex; justify-content:space-between; align-items:center;">
            <h1>Banner trang chủ</h1>
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/admin/banners/form">Thêm banner</a>
        </div>
        <p class="hint">Chọn vị trí (đầu trang / cuối trang / trái / xen kẽ menu) — trang thực đơn tự căn chỉnh bố cục theo những vị trí đang có banner đang bật.</p>

        <c:choose>
            <c:when test="${empty banners}">
                <div class="empty-state">
                    <h2>Chưa có banner nào</h2>
                    <p>Thêm banner để hiển thị trên trang thực đơn.</p>
                </div>
            </c:when>
            <c:otherwise>
                <table class="data-table" style="margin-top:20px;">
                    <thead>
                        <tr><th>Vị trí</th><th>Tiêu đề</th><th>Ảnh</th><th>Thứ tự</th><th>Trạng thái</th><th></th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="b" items="${banners}">
                            <tr>
                                <td>
                                    <c:choose>
                                        <c:when test="${b.position == 'HEAD'}">Đầu trang</c:when>
                                        <c:when test="${b.position == 'FOOTER'}">Cuối trang</c:when>
                                        <c:when test="${b.position == 'LEFT'}">Bên trái</c:when>
                                        <c:when test="${b.position == 'RIGHT'}">Bên phải</c:when>
                                    </c:choose>
                                </td>
                                <td><c:out value="${b.title}" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${b.hasImage and b.video}">
                                            <video src="${pageContext.request.contextPath}/banner-image?id=${b.bannerId}"
                                                   muted playsinline preload="metadata"
                                                   style="width:80px; border-radius:6px; display:block;"></video>
                                            <span class="hint">Video</span>
                                        </c:when>
                                        <c:when test="${b.hasImage}">
                                            <img src="${pageContext.request.contextPath}/banner-image?id=${b.bannerId}" alt="${b.title}" style="width:80px; border-radius:6px;">
                                        </c:when>
                                        <c:otherwise><span class="hint">Chưa có ảnh/video</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${b.sortOrder}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${b.active}"><span class="badge badge-completed">Đang hiện</span></c:when>
                                        <c:otherwise><span class="badge badge-cancelled">Đã ẩn</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="table-actions">
                                    <a class="btn btn-sm btn-secondary" href="${pageContext.request.contextPath}/admin/banners/form?id=${b.bannerId}">Sửa</a>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/banners/toggle" style="display:inline;">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="bannerId" value="${b.bannerId}">
                                        <input type="hidden" name="active" value="${!b.active}">
                                        <button type="submit" class="btn btn-sm ${b.active ? 'btn-danger' : 'btn-secondary'}">${b.active ? 'Ẩn' : 'Hiện lại'}</button>
                                    </form>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/banners/delete" style="display:inline;"
                                          onsubmit="return confirm('Xoá hẳn banner này?');">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="bannerId" value="${b.bannerId}">
                                        <button type="submit" class="btn btn-sm btn-danger">Xoá</button>
                                    </form>
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
