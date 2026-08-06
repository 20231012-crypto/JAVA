<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="auth-container">
        <div class="auth-image">
            <h2>Tham gia căng tin EAUT</h2>
            <p>Tạo tài khoản để đặt món và theo dõi đơn hàng.</p>
        </div>
        <div class="auth-form">
            <h1 style="margin-bottom:24px;">Đăng ký</h1>

            <c:if test="${not empty error}">
                <div class="alert alert-error"><c:out value="${error}" /></div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/register">
                <div class="form-group">
                    <label for="fullName">Họ và tên</label>
                    <input type="text" id="fullName" name="fullName" value="${fullName}" required autofocus>
                </div>
                <div class="form-group">
                    <label for="username">Tên đăng nhập</label>
                    <input type="text" id="username" name="username" value="${username}" required minlength="3" maxlength="50">
                </div>
                <div class="form-group">
                    <label for="email">Email</label>
                    <input type="email" id="email" name="email" value="${email}" required>
                </div>
                <div class="form-group">
                    <label for="phone">Số điện thoại</label>
                    <input type="tel" id="phone" name="phone" value="${phone}">
                </div>
                <div class="form-group">
                    <label for="password">Mật khẩu</label>
                    <input type="password" id="password" name="password" required minlength="6">
                    <span class="hint">Tối thiểu 6 ký tự.</span>
                </div>
                <div class="form-group">
                    <label for="confirmPassword">Xác nhận mật khẩu</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6">
                </div>
                <button type="submit" class="btn btn-primary" style="width:100%;">Tạo tài khoản</button>
            </form>

            <p style="margin-top:20px; text-align:center; color:var(--color-text-muted);">
                Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập</a>
            </p>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
