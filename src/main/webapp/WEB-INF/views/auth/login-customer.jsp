<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="auth-container">
        <div class="auth-image">
            <h2>Chào mừng trở lại</h2>
            <p>Đăng nhập để đặt món tại căng tin EAUT.</p>
            <p class="smart-id-badge"><svg class="icon" aria-hidden="true"><use href="#i-id-card"/></svg> EAUT Smart ID: tự động giảm giá cho email @eaut.edu.vn</p>
        </div>
        <div class="auth-form">
            <h1 style="margin-bottom:8px;">Đăng nhập khách hàng</h1>
            <p class="hint" style="margin-bottom:24px;">
                <a href="${pageContext.request.contextPath}/login"><svg class="icon" aria-hidden="true"><use href="#i-arrow-left"/></svg> Không phải khách hàng? Chọn lại loại tài khoản</a>
            </p>

            <c:if test="${not empty error}">
                <div class="alert alert-error"><c:out value="${error}" /></div>
            </c:if>

            <p class="hint" style="margin-bottom:16px;">Đăng nhập hoặc đăng ký bằng tài khoản Google của bạn — không cần mật khẩu riêng.</p>

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
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
