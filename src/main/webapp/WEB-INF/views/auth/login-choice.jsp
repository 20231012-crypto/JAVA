<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<main class="page-content">
    <h1 style="text-align:center; margin-top:8px;">Đăng nhập Căng tin EAUT</h1>
    <p style="text-align:center; color:var(--color-text-muted); margin-top:8px;">Chọn loại tài khoản để tiếp tục</p>

    <div class="login-choice-grid">
        <a class="login-choice-card" href="${ctx}/login/customer${not empty param.redirect ? '?redirect=' : ''}${param.redirect}">
            <div class="login-choice-icon"><svg class="icon" aria-hidden="true"><use href="#i-graduation-cap"/></svg></div>
            <h2>Khách hàng</h2>
            <p>Sinh viên / giảng viên — đăng nhập bằng tài khoản Google để đặt món, theo dõi đơn hàng.</p>
        </a>
        <a class="login-choice-card" href="${ctx}/login/staff${not empty param.redirect ? '?redirect=' : ''}${param.redirect}">
            <div class="login-choice-icon"><svg class="icon" aria-hidden="true"><use href="#i-users"/></svg>‍<svg class="icon" aria-hidden="true"><use href="#i-briefcase"/></svg></div>
            <h2>Nhân viên</h2>
            <p>Quản lý, nhân viên bán hàng, nhân viên cửa hàng — đăng nhập bằng tài khoản được cấp.</p>
        </a>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
