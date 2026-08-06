<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<% exception.printStackTrace(); %>
<c:set var="pageTitle" value="Đã có lỗi xảy ra" scope="request" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container empty-state">
        <h2>Đã có lỗi xảy ra</h2>
        <p>Hệ thống gặp sự cố. Vui lòng thử lại sau hoặc quay về trang chủ.</p>
        <p style="margin-top:16px;"><a class="btn btn-primary" href="${pageContext.request.contextPath}/products">Về trang chủ</a></p>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
