<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Thực đơn căng tin</h1>

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
                    <c:forEach var="p" items="${products}">
                        <a class="card product-card" href="${pageContext.request.contextPath}/products/detail?id=${p.productId}">
                            <c:choose>
                                <c:when test="${not empty p.imageFilename}">
                                    <img src="${pageContext.request.contextPath}/images/${p.imageFilename}" alt="${p.name}">
                                </c:when>
                                <c:otherwise>
                                    <div class="product-image-placeholder"><c:out value="${p.name}" /></div>
                                </c:otherwise>
                            </c:choose>
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
