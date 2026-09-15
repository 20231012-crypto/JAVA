<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Bare fragment (no header/nav/footer) — fetched via JS and injected into the quick-view modal. --%>
<div class="quick-view-layout">
    <div class="quick-view-media">
        <c:if test="${product.onPromo}">
            <span class="discount-badge">-${product.discountPercent}%</span>
        </c:if>
        <c:choose>
            <c:when test="${not empty product.imageFilename}">
                <img src="${pageContext.request.contextPath}/images/${product.imageFilename}" alt="${product.name}">
            </c:when>
            <c:otherwise>
                <div class="product-image-placeholder" style="height:100%;"><c:out value="${product.name}" /></div>
            </c:otherwise>
        </c:choose>
    </div>
    <div class="quick-view-info">
        <span class="badge badge-confirmed"><c:out value="${product.categoryName}" /></span>
        <h2 style="margin:10px 0 6px;"><c:out value="${product.name}" /></h2>
        <p style="color:var(--color-text-muted); font-size:0.9rem; margin-bottom:12px;"><c:out value="${product.description}" /></p>

        <c:if test="${product.showSoldProgress}">
            <div class="sold-progress" style="max-width:260px;">
                <div class="sold-progress-bar"><div class="sold-progress-fill" style="width:${product.soldProgressPercent}%"></div></div>
                <div class="sold-progress-label">
                    <span>Đã bán: ${product.soldQuantity}/${product.promoTargetQuantity}</span>
                    <span>${product.soldProgressPercent}%</span>
                </div>
            </div>
        </c:if>

        <div class="product-price" style="margin-bottom:16px;">
            <c:if test="${product.onPromo}">
                <span class="price-original" style="font-size:1rem;"><fmt:formatNumber value="${product.originalPrice}" type="number" groupingUsed="true" />₫</span>
            </c:if>
            <span class="price-current" style="font-size:1.3rem;"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true" />₫</span>
            <c:if test="${not empty product.unit}"> / <c:out value="${product.unit}" /></c:if>
        </div>

        <div style="display:flex; gap:10px; align-items:center;">
            <c:choose>
                <c:when test="${product.sellable}">
                    <button type="button" class="btn btn-primary quick-add-btn" data-quick-add
                            data-product-id="${product.productId}" data-product-name="${product.name}">Thêm vào giỏ</button>
                </c:when>
                <c:otherwise>
                    <p class="out-of-stock">Sản phẩm tạm hết hàng.</p>
                </c:otherwise>
            </c:choose>
            <c:if test="${not empty sessionScope.user and sessionScope.user.role.customerDefault}">
                <button type="button" class="icon-btn favorite-btn ${product.favoritedByCurrentUser ? 'is-favorited' : ''}"
                        data-favorite-toggle data-product-id="${product.productId}" aria-label="Yêu thích">
                    <svg class="icon" aria-hidden="true"><use href="#i-heart"/></svg>
                </button>
            </c:if>
            <a class="btn btn-secondary" href="${pageContext.request.contextPath}/products/detail?id=${product.productId}">Xem đầy đủ</a>
        </div>
    </div>
</div>
