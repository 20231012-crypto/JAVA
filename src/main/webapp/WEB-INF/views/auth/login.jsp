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

            <form method="post" action="${pageContext.request.contextPath}/login">
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

            <p style="margin-top:20px; text-align:center; color:var(--color-text-muted);">
                Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register">Đăng ký ngay</a>
            </p>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
