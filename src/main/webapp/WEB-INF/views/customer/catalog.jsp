<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
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
        <div class="hero-carousel ${not empty headBanners ? 'has-controls' : ''}" data-hero-carousel>
            <div class="hero-carousel-viewport">
                <div class="hero-banner hero-carousel-slide is-active">
                    <span class="hero-banner-eyebrow">CĂNG TIN EAUT</span>
                    <h1>Đặt đồ ăn nhanh chóng — giao tận tòa nhà</h1>
                    <p>Chọn món, thanh toán bằng Ví EAUT Pay hoặc VietQR, và tiếp tục học.</p>
                    <div class="hero-info-pills">
                        <span class="hero-info-pill"><strong><svg class="icon" aria-hidden="true"><use href="#i-clock"/></svg> ${estimatedWaitMinutes} phút</strong>Chờ ước tính</span>
                        <span class="hero-info-pill"><strong><svg class="icon" aria-hidden="true"><use href="#i-building"/></svg> 4 tòa nhà</strong>Giao tận nơi</span>
                        <span class="hero-info-pill"><strong><svg class="icon" aria-hidden="true"><use href="#i-wallet"/></svg> EAUT Pay</strong>Tích điểm mỗi đơn</span>
                        <c:choose>
                            <c:when test="${shopAcceptingOrders == false}">
                                <span class="hero-info-pill"><strong><svg class="icon" aria-hidden="true"><use href="#i-ban"/></svg> Tạm ngưng</strong>Quay lại sau ít phút</span>
                            </c:when>
                            <c:otherwise>
                                <span class="hero-info-pill"><strong><svg class="icon" aria-hidden="true"><use href="#i-check-circle"/></svg> Đang mở</strong>Nhận đơn bình thường</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <c:forEach var="b" items="${headBanners}">
                    <div class="hero-carousel-slide hero-carousel-slide-banner">
                        <%-- In the carousel, carousel.js drives playback (it plays on arrival and advances
                             when the clip ends), so the slide must not autoplay or loop on its own. --%>
                        <c:set var="bannerInCarousel" value="true" scope="request" />
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
            <div class="alert alert-error"><svg class="icon" aria-hidden="true"><use href="#i-ban"/></svg> Căng tin đang tạm ngưng nhận đơn — bạn vẫn xem được thực đơn nhưng chưa đặt hàng được lúc này.</div>
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
                <a class="active-filter-clear" href="${ctx}/products"><svg class="icon" aria-hidden="true"><use href="#i-x"/></svg> Xem tất cả</a>
            </div>
        </c:if>

        <%-- Discovery strips only show on the plain menu; CatalogServlet leaves them unset when a
             search or category filter is active so results are not buried under them. --%>
        <c:if test="${not empty promoProducts}">
            <section class="catalog-section">
                <h2 class="catalog-section-title">Khuyến mãi</h2>
                <div class="card-grid">
                    <c:forEach var="p" items="${promoProducts}" varStatus="loop">
                        <c:set var="cardProduct" value="${p}" scope="request" />
                        <c:set var="cardDelay" value="${(loop.index % 5) * 40}" scope="request" />
                        <jsp:include page="/WEB-INF/views/customer/_product-card.jsp" />
                    </c:forEach>
                </div>
            </section>
        </c:if>

        <c:if test="${not empty hotProducts}">
            <section class="catalog-section">
                <h2 class="catalog-section-title">Hot hit tuần</h2>
                <p class="catalog-section-note">Món được đặt nhiều nhất ${hotWindowDays} ngày qua.</p>
                <div class="card-grid">
                    <c:forEach var="p" items="${hotProducts}" varStatus="loop">
                        <c:set var="cardProduct" value="${p}" scope="request" />
                        <c:set var="cardDelay" value="${(loop.index % 5) * 40}" scope="request" />
                        <jsp:include page="/WEB-INF/views/customer/_product-card.jsp" />
                    </c:forEach>
                </div>
            </section>
        </c:if>

        <c:if test="${not empty favoriteProducts}">
            <section class="catalog-section">
                <h2 class="catalog-section-title">Món yêu thích của bạn</h2>
                <div class="card-grid">
                    <c:forEach var="p" items="${favoriteProducts}" varStatus="loop">
                        <c:set var="cardProduct" value="${p}" scope="request" />
                        <c:set var="cardDelay" value="${(loop.index % 5) * 40}" scope="request" />
                        <jsp:include page="/WEB-INF/views/customer/_product-card.jsp" />
                    </c:forEach>
                </div>
            </section>
        </c:if>

        <div class="catalog-layout ${not empty leftBanners ? 'has-left' : ''} ${not empty rightBanners ? 'has-right' : ''}">
            <c:if test="${not empty leftBanners}">
                <aside class="catalog-sidebar">
                    <c:forEach var="b" items="${leftBanners}">
                        <c:set var="bannerInCarousel" value="false" scope="request" />
                        <c:set var="bannerRef" value="${b}" scope="request" />
                        <jsp:include page="/WEB-INF/views/customer/_cms-banner.jsp" />
                    </c:forEach>
                </aside>
            </c:if>

            <div class="catalog-main">
                <div class="catalog-toolbar">
                    <span class="catalog-count">
                        <c:choose>
                            <c:when test="${not empty searchQuery}">
                                Tìm thấy <strong>${totalItems}</strong> món cho &ldquo;<c:out value="${searchQuery}" />&rdquo;
                            </c:when>
                            <c:otherwise><strong>${totalItems}</strong> món</c:otherwise>
                        </c:choose>
                    </span>
                    <form class="catalog-sort" method="get" action="${ctx}/products">
                        <c:if test="${not empty searchQuery}"><input type="hidden" name="q" value="<c:out value='${searchQuery}'/>"></c:if>
                        <c:if test="${not empty selectedCategory}"><input type="hidden" name="category" value="${selectedCategory}"></c:if>
                        <label for="sort">Sắp xếp</label>
                        <select id="sort" name="sort" onchange="this.form.submit()">
                            <option value="" ${empty selectedSort ? 'selected' : ''}>Tên A-Z</option>
                            <option value="gia-tang" ${selectedSort == 'gia-tang' ? 'selected' : ''}>Giá thấp đến cao</option>
                            <option value="gia-giam" ${selectedSort == 'gia-giam' ? 'selected' : ''}>Giá cao đến thấp</option>
                            <option value="ban-chay" ${selectedSort == 'ban-chay' ? 'selected' : ''}>Bán chạy nhất</option>
                            <option value="moi" ${selectedSort == 'moi' ? 'selected' : ''}>Món mới nhất</option>
                        </select>
                        <noscript><button type="submit" class="btn btn-secondary btn-sm">Áp dụng</button></noscript>
                    </form>
                </div>

                <c:choose>
                    <c:when test="${empty products}">
                        <div class="empty-state">
                            <c:choose>
                                <c:when test="${not empty searchQuery}">
                                    <h2>Không tìm thấy món nào</h2>
                                    <p>Không có món nào khớp với &ldquo;<c:out value="${searchQuery}" />&rdquo;. Thử từ khóa khác xem sao.</p>
                                    <p style="margin-top:12px;"><a class="btn btn-primary" href="${ctx}/products">Xem toàn bộ thực đơn</a></p>
                                </c:when>
                                <c:otherwise>
                                    <h2>Chưa có món nào</h2>
                                    <p>Vui lòng quay lại sau.</p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="card-grid">
                            <c:forEach var="p" items="${products}" varStatus="loop">
                                <c:set var="cardProduct" value="${p}" scope="request" />
                                <c:set var="cardDelay" value="${(loop.index % 5) * 40}" scope="request" />
                                <jsp:include page="/WEB-INF/views/customer/_product-card.jsp" />
                            </c:forEach>
                        </div>

                        <c:if test="${totalPages > 1}">
                            <%-- Server-rendered links so paging works without JS and every page is a
                                 real, shareable URL. Each link carries the active filters along. --%>
                            <c:set var="filterQs"><c:if test="${not empty searchQuery}">&amp;q=${fn:escapeXml(searchQuery)}</c:if><c:if test="${not empty selectedCategory}">&amp;category=${selectedCategory}</c:if><c:if test="${not empty selectedSort}">&amp;sort=${fn:escapeXml(selectedSort)}</c:if></c:set>
                            <nav class="pagination" aria-label="Phân trang thực đơn">
                                <c:choose>
                                    <c:when test="${currentPage > 1}">
                                        <a class="pagination-link" href="${ctx}/products?page=${currentPage - 1}${filterQs}" rel="prev">&#8249; Trước</a>
                                    </c:when>
                                    <c:otherwise><span class="pagination-link is-disabled">&#8249; Trước</span></c:otherwise>
                                </c:choose>

                                <c:forEach begin="1" end="${totalPages}" var="i">
                                    <c:choose>
                                        <c:when test="${i == currentPage}">
                                            <span class="pagination-link is-current" aria-current="page">${i}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="pagination-link" href="${ctx}/products?page=${i}${filterQs}">${i}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>

                                <c:choose>
                                    <c:when test="${currentPage < totalPages}">
                                        <a class="pagination-link" href="${ctx}/products?page=${currentPage + 1}${filterQs}" rel="next">Sau &#8250;</a>
                                    </c:when>
                                    <c:otherwise><span class="pagination-link is-disabled">Sau &#8250;</span></c:otherwise>
                                </c:choose>
                            </nav>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </div>

            <c:if test="${not empty rightBanners}">
                <aside class="catalog-sidebar">
                    <c:forEach var="b" items="${rightBanners}">
                        <c:set var="bannerInCarousel" value="false" scope="request" />
                        <c:set var="bannerRef" value="${b}" scope="request" />
                        <jsp:include page="/WEB-INF/views/customer/_cms-banner.jsp" />
                    </c:forEach>
                </aside>
            </c:if>
        </div>

        <c:if test="${not empty footerBanners}">
            <div class="cms-banner-row">
                <c:forEach var="b" items="${footerBanners}">
                    <c:set var="bannerInCarousel" value="false" scope="request" />
                    <c:set var="bannerRef" value="${b}" scope="request" />
                    <jsp:include page="/WEB-INF/views/customer/_cms-banner.jsp" />
                </c:forEach>
            </div>
        </c:if>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
