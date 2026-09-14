<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <div class="detail-layout">
            <c:choose>
                <c:when test="${not empty product.imageFilename}">
                    <img src="${pageContext.request.contextPath}/images/${product.imageFilename}" alt="${product.name}"
                         style="width:100%; border-radius:16px;">
                </c:when>
                <c:otherwise>
                    <div class="product-image-placeholder" style="height:280px; border-radius:16px;">
                        <c:out value="${product.name}" />
                    </div>
                </c:otherwise>
            </c:choose>

            <div>
                <span class="badge badge-confirmed"><c:out value="${product.categoryName}" /></span>
                <h1 style="margin:12px 0 8px;"><c:out value="${product.name}" /></h1>
                <p style="color:var(--color-text-muted); margin-bottom:16px;"><c:out value="${product.description}" /></p>
                <c:if test="${product.showSoldProgress}">
                    <div class="sold-progress" style="max-width:280px;">
                        <div class="sold-progress-bar"><div class="sold-progress-fill" style="width:${product.soldProgressPercent}%"></div></div>
                        <div class="sold-progress-label">
                            <span>Đã bán: ${product.soldQuantity}/${product.promoTargetQuantity}</span>
                            <span>${product.soldProgressPercent}%</span>
                        </div>
                    </div>
                </c:if>
                <div class="product-price" style="margin-bottom:16px;">
                    <c:if test="${product.onPromo}">
                        <span class="price-original" style="font-size:1.1rem;"><fmt:formatNumber value="${product.originalPrice}" type="number" groupingUsed="true" />₫</span>
                    </c:if>
                    <span class="price-current" style="font-size:1.5rem;"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true" />₫</span>
                    <c:if test="${not empty product.unit}"> / <c:out value="${product.unit}" /></c:if>
                </div>

                <c:choose>
                    <c:when test="${product.shelfQuantity > 0}">
                        <form method="post" action="${pageContext.request.contextPath}/cart/add">
                            <input type="hidden" name="productId" value="${product.productId}">
                            <div class="qty-form" style="margin-bottom:16px;">
                                <label for="quantity">Số lượng</label>
                                <input type="number" id="quantity" name="quantity" value="1" min="1" max="${product.shelfQuantity}">
                            </div>
                            <button type="submit" class="btn btn-primary">Thêm vào giỏ hàng</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <p class="out-of-stock">Sản phẩm tạm hết hàng.</p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
