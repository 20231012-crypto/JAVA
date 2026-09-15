<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content" data-page="catalog">
    <div class="container">
        <%-- Rotating hero. Slide 1 is the live status panel (estimated wait + whether the canteen
             is accepting orders right now), so the carousel is never empty and that information
             never disappears; every further slide is an admin-configured HEAD banner from
             /admin/banners, so the promotional content is not hardcoded. Arrows and dots only
             render when there is more than one slide. --%>
        <div class="hero-carousel" data-hero-carousel>
            <div class="hero-carousel-viewport">
                <div class="hero-banner hero-carousel-slide is-active">
                    <span class="hero-banner-eyebrow">CĂNG TIN EAUT</span>
                    <h1>Đặt đồ ăn nhanh chóng — giao tận tòa nhà</h1>
                    <p>Chọn món, thanh toán bằng Ví EAUT Pay hoặc VietQR, và tiếp tục học.</p>
                    <div class="hero-info-pills">
                        <span class="hero-info-pill"><strong>⏱ ${estimatedWaitMinutes} phút</strong>Chờ ước tính</span>
                        <span class="hero-info-pill"><strong>🏢 4 tòa nhà</strong>Giao tận nơi</span>
                        <span class="hero-info-pill"><strong>💳 EAUT Pay</strong>Tích điểm mỗi đơn</span>
                        <c:choose>
                            <c:when test="${shopAcceptingOrders == false}">
                                <span class="hero-info-pill"><strong>🚫 Tạm ngưng</strong>Quay lại sau ít phút</span>
                            </c:when>
                            <c:otherwise>
                                <span class="hero-info-pill"><strong>✅ Đang mở</strong>Nhận đơn bình thường</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <c:forEach var="b" items="${headBanners}">
                    <div class="hero-carousel-slide hero-carousel-slide-banner">
                        <c:set var="bannerRef" value="${b}" scope="request" />
                        <jsp:include page="/WEB-INF/views/customer/_cms-banner.jsp" />
                    </div>
                </c:forEach>
            </div>

            <c:if test="${not empty headBanners}">
                <button type="button" class="hero-carousel-nav hero-carousel-prev" data-hero-prev
                        aria-label="Banner trước">&#8249;</button>
                <button type="button" class="hero-carousel-nav hero-carousel-next" data-hero-next
                        aria-label="Banner sau">&#8250;</button>
                <div class="hero-carousel-dots" data-hero-dots>
                    <button type="button" class="hero-carousel-dot is-active" data-hero-dot="0"
                            aria-label="Chuyển tới banner 1"></button>
                    <c:forEach var="b" items="${headBanners}" varStatus="bLoop">
                        <button type="button" class="hero-carousel-dot" data-hero-dot="${bLoop.index + 1}"
                                aria-label="Chuyển tới banner ${bLoop.index + 2}"></button>
                    </c:forEach>
                </div>
            </c:if>
        </div>

        <c:if test="${shopAcceptingOrders == false}">
            <div class="alert alert-error">🚫 Căng tin đang tạm ngưng nhận đơn — bạn vẫn xem được thực đơn nhưng chưa đặt hàng được lúc này.</div>
        </c:if>

        <%-- The in-page category chips were removed (the nav's "Danh mục sản phẩm" mega-menu covers
             the same job), but a filtered view still needs to say what it is filtered to and offer
             a way back out. --%>
        <c:if test="${not empty selectedCategory}">
            <div class="active-filter">
                <span class="active-filter-label">Đang xem:</span>
                <c:forEach var="cat" items="${categories}">
                    <c:if test="${cat.categoryId == selectedCategory}">
                        <strong><c:out value="${cat.name}" /></strong>
                    </c:if>
                </c:forEach>
                <a class="active-filter-clear" href="${ctx}/products">✕ Xem tất cả</a>
            </div>
        </c:if>

        <div class="catalog-layout ${not empty leftBanners ? 'has-left' : ''} ${not empty rightBanners ? 'has-right' : ''}">
            <c:if test="${not empty leftBanners}">
                <aside class="catalog-sidebar">
                    <c:forEach var="b" items="${leftBanners}">
                        <c:set var="bannerRef" value="${b}" scope="request" />
                        <jsp:include page="/WEB-INF/views/customer/_cms-banner.jsp" />
                    </c:forEach>
                </aside>
            </c:if>

            <div class="catalog-main">
                <c:choose>
                    <c:when test="${empty products}">
                        <div class="empty-state">
                            <h2>Chưa có món nào</h2>
                            <p>Vui lòng quay lại sau.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="card-grid">
                            <c:forEach var="p" items="${products}" varStatus="loop">
                                <div class="card product-card reveal-on-scroll" style="transition-delay:${(loop.index % 6) * 40}ms">
                                    <a class="product-card-media" href="${pageContext.request.contextPath}/products/detail?id=${p.productId}">
                                        <c:if test="${p.onPromo}">
                                            <span class="discount-badge">-${p.discountPercent}%</span>
                                        </c:if>
                                        <c:choose>
                                            <c:when test="${not empty p.imageFilename}">
                                                <img src="${pageContext.request.contextPath}/images/${p.imageFilename}" alt="${p.name}" loading="lazy">
                                            </c:when>
                                            <c:otherwise>
                                                <div class="product-image-placeholder"><c:out value="${p.name}" /></div>
                                            </c:otherwise>
                                        </c:choose>
                                        <div class="card-hover-actions">
                                            <c:if test="${not empty sessionScope.user and sessionScope.user.role.customerDefault}">
                                                <button type="button" class="icon-btn favorite-btn ${p.favoritedByCurrentUser ? 'is-favorited' : ''}"
                                                        data-favorite-toggle data-product-id="${p.productId}" aria-label="Yêu thích ${p.name}">
                                                    <c:choose>
                                                        <c:when test="${p.favoritedByCurrentUser}">♥</c:when>
                                                        <c:otherwise>♡</c:otherwise>
                                                    </c:choose>
                                                </button>
                                            </c:if>
                                            <button type="button" class="icon-btn quickview-btn" data-quick-view data-product-id="${p.productId}" aria-label="Xem nhanh ${p.name}">👁</button>
                                        </div>
                                    </a>
                                    <a class="card-body" href="${pageContext.request.contextPath}/products/detail?id=${p.productId}">
                                        <div class="product-name"><c:out value="${p.name}" /></div>
                                        <c:if test="${p.showSoldProgress}">
                                            <div class="sold-progress">
                                                <div class="sold-progress-bar"><div class="sold-progress-fill" style="width:${p.soldProgressPercent}%"></div></div>
                                                <div class="sold-progress-label">
                                                    <span>Đã bán: ${p.soldQuantity}/${p.promoTargetQuantity}</span>
                                                    <span>${p.soldProgressPercent}%</span>
                                                </div>
                                            </div>
                                        </c:if>
                                        <div class="product-price">
                                            <c:if test="${p.onPromo}">
                                                <span class="price-original"><fmt:formatNumber value="${p.originalPrice}" type="number" groupingUsed="true" />₫</span>
                                            </c:if>
                                            <span class="price-current"><fmt:formatNumber value="${p.price}" type="number" groupingUsed="true" />₫</span>
                                        </div>
                                        <c:if test="${p.shelfQuantity <= 0}">
                                            <div class="out-of-stock">Tạm hết hàng</div>
                                        </c:if>
                                    </a>
                                    <c:if test="${not empty sessionScope.user and sessionScope.user.role.customerDefault and p.shelfQuantity > 0 and shopAcceptingOrders != false}">
                                        <button type="button" class="quick-add-btn quick-add-btn-block" data-quick-add data-product-id="${p.productId}" data-product-name="${p.name}"
                                                aria-label="Thêm ${p.name} vào giỏ hàng">Thêm vào giỏ</button>
                                    </c:if>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <c:if test="${not empty rightBanners}">
                <aside class="catalog-sidebar">
                    <c:forEach var="b" items="${rightBanners}">
                        <c:set var="bannerRef" value="${b}" scope="request" />
                        <jsp:include page="/WEB-INF/views/customer/_cms-banner.jsp" />
                    </c:forEach>
                </aside>
            </c:if>
        </div>

        <c:if test="${not empty footerBanners}">
            <div class="cms-banner-row">
                <c:forEach var="b" items="${footerBanners}">
                    <c:set var="bannerRef" value="${b}" scope="request" />
                    <jsp:include page="/WEB-INF/views/customer/_cms-banner.jsp" />
                </c:forEach>
            </div>
        </c:if>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
