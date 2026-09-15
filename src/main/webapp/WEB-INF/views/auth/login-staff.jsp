<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="auth-container">
        <div class="auth-image">
            <h2>Khu vực nhân viên</h2>
            <p>Dành cho Quản lý, Nhân viên bán hàng, Nhân viên cửa hàng — và bất kỳ vai trò nào khác được Admin tạo trong Quản lý vai trò &amp; phân quyền.</p>
        </div>
        <div class="auth-form">
            <h1 style="margin-bottom:8px;">Đăng nhập nhân viên</h1>
            <p class="hint" style="margin-bottom:24px;">
                <a href="${pageContext.request.contextPath}/login"><svg class="icon" aria-hidden="true"><use href="#i-arrow-left"/></svg> Không phải nhân viên? Chọn lại loại tài khoản</a>
            </p>

            <c:if test="${not empty error}">
                <div class="alert alert-error"><c:out value="${error}" /></div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/login">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <c:if test="${not empty param.redirect}">
                    <input type="hidden" name="redirect" value="${param.redirect}">
                </c:if>
                <div class="form-group">
                    <label for="username">Tên đăng nhập</label>
                    <input type="text" id="username" name="username" value="${username}" required autofocus>
                </div>
                <div class="form-group">
                    <label for="password">Mật khẩu</label>
                    <input type="password" id="password" name="password" required>
                </div>
                <button type="submit" class="btn btn-primary" style="width:100%;">Đăng nhập</button>
            </form>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
