<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
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
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">

    <!-- Favicon: EAUT crest -->
    <link rel="icon" type="image/jpeg" href="${pageContext.request.contextPath}/assets/images/brand/eaut-logo.jpg">

    <!-- Installable webapp (Add to Home Screen) -->
    <link rel="manifest" href="${pageContext.request.contextPath}/manifest.json">
    <meta name="theme-color" content="#1E4FA3">
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/assets/icons/apple-touch-icon.png">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="default">
    <meta name="apple-mobile-web-app-title" content="Căng tin EAUT">
    <script src="${pageContext.request.contextPath}/assets/js/pwa.js" defer></script>

    <title><c:out value="${pageTitle}" /> - Căng tin EAUT</title>
</head>
<body>
