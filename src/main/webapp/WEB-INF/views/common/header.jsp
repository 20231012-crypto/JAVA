<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" scope="request" />
<c:if test="${empty pageTitle}">
    <c:set var="pageTitle" value="Căng tin EAUT" scope="request" />
</c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@600;700;800&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">

    <!-- Favicon: EAUT crest -->
    <link rel="icon" type="image/jpeg" href="${pageContext.request.contextPath}/assets/images/brand/eaut-logo.jpg">

    <!-- Installable webapp (Add to Home Screen) -->
    <link rel="manifest" href="${pageContext.request.contextPath}/manifest.json">
    <meta name="theme-color" content="#0B3C88">
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/assets/icons/apple-touch-icon.png">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="default">
    <meta name="apple-mobile-web-app-title" content="Căng tin EAUT">
    <script src="${pageContext.request.contextPath}/assets/js/pwa.js" defer></script>

    <title><c:out value="${pageTitle}" /> - Căng tin EAUT</title>
</head>
<%-- The URL the browser actually asked for. Inside a forwarded JSP, servletPath is the view file
     (/WEB-INF/views/...), not the route, so the original has to come from the forward attribute;
     the fallback covers a JSP reached without a forward. Set once here in request scope so nav.jsp
     and admin-sidebar.jsp can both use it. --%>
<c:set var="requestPath" scope="request"
       value="${empty requestScope['jakarta.servlet.forward.servlet_path']
                ? pageContext.request.servletPath
                : requestScope['jakarta.servlet.forward.servlet_path']}" />
<%-- Admin screens get the fixed left rail, so the body has to keep clear of it. Derived from the
     path rather than set by each page, so a new /admin/* screen picks it up automatically. --%>
<body data-context-path="${pageContext.request.contextPath}"
      data-csrf="${csrfToken}"
      class="${fn:startsWith(requestPath, '/admin') ? 'has-admin-rail' : ''}">
