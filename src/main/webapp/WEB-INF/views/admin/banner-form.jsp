<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container" style="max-width:600px;">
        <h1>${empty banner ? 'Thêm banner' : 'Sửa banner'}</h1>

        <c:if test="${not empty error}">
            <div class="alert alert-error"><c:out value="${error}" /></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/admin/banners/save" enctype="multipart/form-data">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <c:if test="${not empty banner}">
                <input type="hidden" name="bannerId" value="${banner.bannerId}">
            </c:if>

            <div class="form-group">
                <label for="position">Vị trí hiển thị</label>
                <select id="position" name="position" required>
                    <option value="HEAD" ${banner.position == 'HEAD' ? 'selected' : ''}>Đầu trang (trên cùng, toàn chiều rộng)</option>
                    <option value="LEFT" ${banner.position == 'LEFT' ? 'selected' : ''}>Bên trái menu</option>
                    <option value="RIGHT" ${banner.position == 'RIGHT' ? 'selected' : ''}>Bên phải menu</option>
                    <option value="FOOTER" ${banner.position == 'FOOTER' ? 'selected' : ''}>Cuối trang (trước footer)</option>
                </select>
                <span class="hint">Trang thực đơn tự thêm cột trái/phải hoặc dải banner đầu/cuối trang tuỳ vị trí nào đang có banner bật.</span>
            </div>
            <div class="form-group">
                <label for="title">Tiêu đề</label>
                <input type="text" id="title" name="title" value="${banner.title}" placeholder="Sản phẩm nổi bật tuần này">
            </div>
            <div class="form-group">
                <label for="subtitle">Mô tả ngắn</label>
                <textarea id="subtitle" name="subtitle" rows="2">${banner.subtitle}</textarea>
            </div>
            <div class="form-group">
                <label for="linkUrl">Liên kết khi bấm vào (tuỳ chọn)</label>
                <input type="text" id="linkUrl" name="linkUrl" value="${banner.linkUrl}" placeholder="/products?category=4">
            </div>
            <div class="form-group">
                <label for="sortOrder">Thứ tự hiển thị (số nhỏ hơn hiện trước)</label>
                <input type="number" id="sortOrder" name="sortOrder" value="${empty banner.sortOrder ? 0 : banner.sortOrder}">
            </div>

        <%-- Scheduling. Both optional: leaving them empty keeps the banner always-on, which is how
             every existing banner behaves. The window is AND-ed with the on/off switch, so the
             switch still takes a banner down immediately regardless of dates. --%>
        <div class="form-group">
            <label for="startAt">Bắt đầu hiển thị</label>
            <input type="datetime-local" id="startAt" name="startAt"
                   value="<c:out value='${banner.startAtInput}'/>">
            <span class="hint">Để trống = hiển thị ngay.</span>
        </div>
        <div class="form-group">
            <label for="endAt">Tự động ẩn lúc</label>
            <input type="datetime-local" id="endAt" name="endAt"
                   value="<c:out value='${banner.endAtInput}'/>">
            <span class="hint">Để trống = không tự ẩn. Dùng cho banner sự kiện như 20/11.</span>
        </div>
            <div class="form-group">
                <label for="image">Ảnh hoặc video banner</label>
                <c:if test="${not empty banner and banner.hasImage}">
                    <c:choose>
                        <c:when test="${banner.video}">
                            <video src="${pageContext.request.contextPath}/banner-image?id=${banner.bannerId}"
                                   controls muted playsinline preload="metadata"
                                   style="width:260px; border-radius:8px; display:block; margin-bottom:8px;"></video>
                        </c:when>
                        <c:otherwise>
                            <img src="${pageContext.request.contextPath}/banner-image?id=${banner.bannerId}" alt="${banner.title}"
                                 style="width:200px; border-radius:8px; display:block; margin-bottom:8px;">
                        </c:otherwise>
                    </c:choose>
                </c:if>
                <input type="file" id="image" name="image" accept=".jpg,.jpeg,.png,.webp,.mp4,.webm">
                <span class="hint">Ảnh: JPG, PNG, WEBP. Video: MP4, WEBM. Tối đa 20MB — video nên là clip ngắn
                    (khoảng 10–15 giây) vì được lưu thẳng trong database để không mất khi deploy lại.
                    Video chạy không tiếng và tự chuyển sang banner kế tiếp khi hết.</span>
            </div>

            <button type="submit" class="btn btn-primary">Lưu banner</button>
            <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admin/banners">Hủy</a>
        </form>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
