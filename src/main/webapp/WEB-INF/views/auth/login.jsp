<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="auth-container">
        <div class="auth-image">
            <h2>Chào mừng trở lại</h2>
            <p>Đăng nhập để đặt món tại căng tin EAUT.</p>
        </div>
        <div class="auth-form">
            <h1 style="margin-bottom:24px;">Đăng nhập</h1>

            <c:if test="${not empty error}">
                <div class="alert alert-error"><c:out value="${error}" /></div>
            </c:if>

            <div class="auth-section">
                <p class="auth-section-title">Khách hàng</p>
                <p class="hint" style="margin-bottom:16px;">Đăng nhập hoặc đăng ký bằng tài khoản Google của bạn.</p>

                <script src="https://accounts.google.com/gsi/client" async defer></script>
                <div id="g_id_onload"
                     data-client_id="${googleClientId}"
                     data-login_uri="${pageContext.request.contextPath}/auth/google"
                     data-ux_mode="redirect"
                     data-auto_prompt="false">
                </div>
                <div class="g_id_signin"
                     data-type="standard"
                     data-size="large"
                     data-theme="outline"
                     data-text="continue_with"
                     data-shape="rectangular"
                     data-locale="vi">
                </div>
            </div>

            <div class="auth-divider"><span>hoặc</span></div>

            <div class="auth-section">
                <p class="auth-section-title">Nhân viên</p>
                <p class="hint" style="margin-bottom:16px;">Dành cho Quản lý, Nhân viên bán hàng, Nhân viên kho.</p>

                <form method="post" action="${pageContext.request.contextPath}/login">
                    <c:if test="${not empty param.redirect}">
                        <input type="hidden" name="redirect" value="${param.redirect}">
                    </c:if>
                    <div class="form-group">
                        <label for="username">Tên đăng nhập</label>
                        <input type="text" id="username" name="username" value="${username}" required>
                    </div>
                    <div class="form-group">
                        <label for="password">Mật khẩu</label>
                        <input type="password" id="password" name="password" required>
                    </div>
                    <button type="submit" class="btn btn-primary" style="width:100%;">Đăng nhập</button>
                </form>
            </div>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
