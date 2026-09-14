<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Bare fragment (no header/nav/footer) — fetched once by JS and injected into the mega-menu flyout. --%>
<div class="mega-menu-columns">
    <c:forEach var="group" items="${categoryGroups}">
        <div class="mega-menu-column">
            <h3><c:out value="${group.parent.name}" /></h3>
            <ul>
                <c:forEach var="child" items="${group.children}">
                    <li><a href="${pageContext.request.contextPath}/products?category=${child.categoryId}"><c:out value="${child.name}" /></a></li>
                </c:forEach>
            </ul>
        </div>
    </c:forEach>
</div>
