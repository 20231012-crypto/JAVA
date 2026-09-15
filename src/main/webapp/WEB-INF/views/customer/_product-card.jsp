<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- One product card. The including page sets ${cardProduct} (and optionally ${cardDelay}, the
     stagger in ms) in request scope first — jsp:param only carries strings, not the Product.

     Extracted because the catalog page now renders this in four places (main grid, promotions,
     trending, favourites); it was already duplicated into product-detail and quick-view, and a
     fifth and sixth copy would have guaranteed they drift apart. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<div class="card product-card reveal-on-scroll" style="transition-delay:${empty cardDelay ? 0 : cardDelay}ms">
    <a class="product-card-media" href="${ctx}/products/detail?id=${cardProduct.productId}">
        <c:if test="${cardProduct.onPromo}">
            <span class="discount-badge">-${cardProduct.discountPercent}%</span>
        </c:if>
        <c:choose>
            <c:when test="${not empty cardProduct.imageFilename}">
                <img src="${ctx}/images/${cardProduct.imageFilename}" alt="${cardProduct.name}" loading="lazy">
            </c:when>
            <c:otherwise>
                <div class="product-image-placeholder"><c:out value="${cardProduct.name}" /></div>
            </c:otherwise>
        </c:choose>
        <div class="card-hover-actions">
            <c:if test="${not empty sessionScope.user and sessionScope.user.role.customerDefault}">
                <button type="button" class="icon-btn favorite-btn ${cardProduct.favoritedByCurrentUser ? 'is-favorited' : ''}"
                        data-favorite-toggle data-product-id="${cardProduct.productId}" aria-label="Yêu thích ${cardProduct.name}">
                    <svg class="icon" aria-hidden="true"><use href="#i-heart"/></svg>
                </button>
            </c:if>
            <button type="button" class="icon-btn quickview-btn" data-quick-view data-product-id="${cardProduct.productId}"
                    aria-label="Xem nhanh ${cardProduct.name}"><svg class="icon" aria-hidden="true"><use href="#i-eye"/></svg></button>
        </div>
    </a>
    <a class="card-body" href="${ctx}/products/detail?id=${cardProduct.productId}">
        <div class="product-name"><c:out value="${cardProduct.name}" /></div>
        <c:if test="${cardProduct.showSoldProgress}">
            <div class="sold-progress">
                <div class="sold-progress-bar"><div class="sold-progress-fill" style="width:${cardProduct.soldProgressPercent}%"></div></div>
                <div class="sold-progress-label">
                    <span>Đã bán: ${cardProduct.soldQuantity}/${cardProduct.promoTargetQuantity}</span>
                    <span>${cardProduct.soldProgressPercent}%</span>
                </div>
            </div>
        </c:if>
        <div class="product-price">
            <c:if test="${cardProduct.onPromo}">
                <span class="price-original"><fmt:formatNumber value="${cardProduct.originalPrice}" type="number" groupingUsed="true" />₫</span>
            </c:if>
            <span class="price-current"><fmt:formatNumber value="${cardProduct.price}" type="number" groupingUsed="true" />₫</span>
        </div>
        <c:if test="${cardProduct.shelfQuantity <= 0}">
            <div class="out-of-stock">Tạm hết hàng</div>
        </c:if>
    </a>
    <c:if test="${not empty sessionScope.user and sessionScope.user.role.customerDefault
                  and cardProduct.shelfQuantity > 0 and shopAcceptingOrders != false}">
        <button type="button" class="quick-add-btn quick-add-btn-block" data-quick-add
                data-product-id="${cardProduct.productId}" data-product-name="${cardProduct.name}"
                aria-label="Thêm ${cardProduct.name} vào giỏ hàng">Thêm vào giỏ</button>
    </c:if>
</div>
