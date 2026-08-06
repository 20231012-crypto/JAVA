<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Không có quyền truy cập" scope="request" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container empty-state">
        <h2>Bạn không có quyền truy cập trang này</h2>
        <p>Tài khoản của bạn không có quyền thực hiện thao tác này. Vui lòng quay lại trang chủ.</p>
        <p style="margin-top:16px;"><a class="btn btn-primary" href="${pageContext.request.contextPath}/products">Về trang chủ</a></p>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
