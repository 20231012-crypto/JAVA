<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content" data-page="catalog">
    <div class="container">
        <div class="hero-banner-carousel" data-autoplay="5000">
            <div class="hero-banner hero-slide is-active">
                <div>
                    <span class="hero-banner-eyebrow">CĂNG TIN EAUT</span>
                    <h1>Đặt đồ ăn nhanh chóng — giao tận tòa nhà</h1>
                    <p>Chọn món, thanh toán, và tiếp tục học — canteen mang đồ ăn đến tận nơi bạn học.</p>
                    <div class="hero-info-pills">
                        <span class="hero-info-pill"><strong>⏱ ${estimatedWaitMinutes} phút</strong>Thời gian chờ ước tính</span>
                        <span class="hero-info-pill"><strong>🏢 4 tòa nhà</strong>Giao tận nơi</span>
                    </div>
                </div>
            </div>
            <div class="hero-banner hero-slide">
                <div>
                    <span class="hero-banner-eyebrow">VÍ EAUT PAY</span>
                    <h1>Thanh toán nhanh, tích điểm mỗi đơn</h1>
                    <p>Nạp ví một lần, đặt món cả tuần không cần quét QR mỗi lần — mỗi đơn hoàn thành còn được cộng điểm đổi giảm giá.</p>
                    <div class="hero-info-pills">
                        <span class="hero-info-pill"><strong>💳 EAUT Pay</strong>Số dư nội bộ</span>
                        <span class="hero-info-pill"><strong>⭐ Tích điểm</strong>Đổi giảm giá</span>
                    </div>
                </div>
            </div>
            <div class="hero-banner hero-slide">
                <div>
                    <c:choose>
                        <c:when test="${shopAcceptingOrders == false}">
                            <span class="hero-banner-eyebrow">TẠM NGƯNG NHẬN ĐƠN</span>
                            <h1>Căng tin đang tạm ngưng nhận đơn</h1>
                            <p>Bạn vẫn xem được thực đơn — quay lại sau ít phút để đặt hàng nhé.</p>
                        </c:when>
                        <c:otherwise>
                            <span class="hero-banner-eyebrow">ĐANG MỞ NHẬN ĐƠN</span>
                            <h1>Sinh viên @eaut.edu.vn được giảm giá tự động</h1>
                            <p>Đăng nhập bằng email trường để tự động áp dụng ưu đãi EAUT Smart ID khi thanh toán.</p>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
            <div class="hero-carousel-dots"></div>
        </div>

        <c:if test="${shopAcceptingOrders == false}">
            <div class="alert alert-error">🚫 Căng tin đang tạm ngưng nhận đơn — bạn vẫn xem được thực đơn nhưng chưa đặt hàng được lúc này.</div>
        </c:if>

        <h1 class="section-title">Thực đơn căng tin</h1>

        <div class="filter-bar">
            <a class="filter-chip ${empty selectedCategory ? 'active' : ''}" href="${pageContext.request.contextPath}/products">Tất cả</a>
            <c:forEach var="cat" items="${categories}">
                <a class="filter-chip ${selectedCategory == cat.categoryId ? 'active' : ''}"
                   href="${pageContext.request.contextPath}/products?category=${cat.categoryId}">
                    <c:out value="${cat.name}" />
                </a>
            </c:forEach>
        </div>

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
                        <a class="card product-card reveal-on-scroll" style="transition-delay:${(loop.index % 6) * 40}ms"
                           href="${pageContext.request.contextPath}/products/detail?id=${p.productId}">
                            <div class="product-card-media">
                                <c:choose>
                                    <c:when test="${not empty p.imageFilename}">
                                        <img src="${pageContext.request.contextPath}/images/${p.imageFilename}" alt="${p.name}" loading="lazy">
                                    </c:when>
                                    <c:otherwise>
                                        <div class="product-image-placeholder"><c:out value="${p.name}" /></div>
                                    </c:otherwise>
                                </c:choose>
                                <c:if test="${not empty sessionScope.user and sessionScope.user.role.customerDefault and p.shelfQuantity > 0 and shopAcceptingOrders != false}">
                                    <button type="button" class="quick-add-btn" data-quick-add data-product-id="${p.productId}" data-product-name="${p.name}"
                                            aria-label="Thêm ${p.name} vào giỏ hàng">+ Giỏ hàng</button>
                                </c:if>
                            </div>
                            <div class="card-body">
                                <div class="product-name"><c:out value="${p.name}" /></div>
                                <div class="product-price">
                                    <fmt:formatNumber value="${p.price}" type="number" groupingUsed="true" />₫
                                </div>
                                <c:if test="${p.shelfQuantity <= 0}">
                                    <div class="out-of-stock">Tạm hết hàng</div>
                                </c:if>
                            </div>
                        </a>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
