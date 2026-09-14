<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Renders one admin-configured banner (${bannerRef}, set in request scope by the including
     page before jsp:include — jsp:param only carries strings, not the Banner object itself). --%>
<div class="cms-banner">
    <c:choose>
        <c:when test="${not empty bannerRef.linkUrl}">
            <a href="${bannerRef.linkUrl}" class="cms-banner-link">
                <c:if test="${bannerRef.hasImage}">
                    <img src="${pageContext.request.contextPath}/banner-image?id=${bannerRef.bannerId}" alt="${bannerRef.title}" class="cms-banner-image">
                </c:if>
                <c:if test="${not empty bannerRef.title}"><div class="cms-banner-title"><c:out value="${bannerRef.title}" /></div></c:if>
                <c:if test="${not empty bannerRef.subtitle}"><div class="cms-banner-subtitle"><c:out value="${bannerRef.subtitle}" /></div></c:if>
            </a>
        </c:when>
        <c:otherwise>
            <c:if test="${bannerRef.hasImage}">
                <img src="${pageContext.request.contextPath}/banner-image?id=${bannerRef.bannerId}" alt="${bannerRef.title}" class="cms-banner-image">
            </c:if>
            <c:if test="${not empty bannerRef.title}"><div class="cms-banner-title"><c:out value="${bannerRef.title}" /></div></c:if>
            <c:if test="${not empty bannerRef.subtitle}"><div class="cms-banner-subtitle"><c:out value="${bannerRef.subtitle}" /></div></c:if>
        </c:otherwise>
    </c:choose>
</div>
