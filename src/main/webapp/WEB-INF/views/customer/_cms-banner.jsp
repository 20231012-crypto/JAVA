<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Renders one admin-configured banner (${bannerRef}, set in request scope by the including
     page before jsp:include — jsp:param only carries strings, not the Banner object itself).

     The media can be an image or a short video clip; both are served by /banner-image from the
     same column. The optional link wraps the whole banner, opened and closed by a pair of c:if
     blocks so the media/title/subtitle markup below only has to be written once. --%>
<c:set var="mediaUrl" value="${pageContext.request.contextPath}/banner-image?id=${bannerRef.bannerId}" />
<div class="cms-banner">
    <c:if test="${not empty bannerRef.linkUrl}">
        <a href="${bannerRef.linkUrl}" class="cms-banner-link">
    </c:if>

    <c:if test="${bannerRef.hasImage}">
        <c:choose>
            <c:when test="${bannerRef.video}">
                <%-- muted + playsinline are what browsers require before they will start a video
                     without a click.

                     Inside the carousel, carousel.js owns playback: it starts the clip when the
                     slide arrives and moves on when the clip ends, so autoplay/loop here would
                     fight it. Everywhere else (the side rails and the footer row) nothing was
                     driving these at all, which is why an uploaded video just sat on its first
                     frame — those autoplay and loop on their own, like any decorative banner.
                     interactions.js pauses them for anyone who asked for reduced motion. --%>
                <video class="cms-banner-media" src="${mediaUrl}" muted playsinline preload="metadata"
                       <%-- preload stays "metadata" even with autoplay: the browser fetches what it
                            needs to start playing anyway, and "auto" would eagerly pull the whole
                            clip on every page load for a decorative rail the reader may never
                            scroll to — two of those is a lot of mobile data. --%>
                       <c:if test="${not bannerInCarousel}">autoplay loop</c:if>
                       <c:if test="${not empty bannerRef.title}">aria-label="<c:out value='${bannerRef.title}'/>"</c:if>></video>
            </c:when>
            <c:otherwise>
                <img src="${mediaUrl}" alt="<c:out value='${bannerRef.title}'/>" class="cms-banner-media">
            </c:otherwise>
        </c:choose>
    </c:if>
    <c:if test="${not empty bannerRef.title}"><div class="cms-banner-title"><c:out value="${bannerRef.title}" /></div></c:if>
    <c:if test="${not empty bannerRef.subtitle}"><div class="cms-banner-subtitle"><c:out value="${bannerRef.subtitle}" /></div></c:if>

    <c:if test="${not empty bannerRef.linkUrl}">
        </a>
    </c:if>
</div>
