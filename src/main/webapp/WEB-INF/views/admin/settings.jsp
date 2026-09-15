<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Cài đặt hệ thống</h1>
        <p class="hint">
            Các giá trị này được đọc trực tiếp khi khách đặt hàng — sửa xong là có hiệu lực ngay,
            không cần triển khai lại.
        </p>

        <c:if test="${not empty sessionScope.actionMessage}">
            <div class="alert alert-success"><c:out value="${sessionScope.actionMessage}" /></div>
            <c:remove var="actionMessage" scope="session" />
        </c:if>
        <c:if test="${not empty sessionScope.actionError}">
            <div class="alert alert-error"><c:out value="${sessionScope.actionError}" /></div>
            <c:remove var="actionError" scope="session" />
        </c:if>

        <%-- The whole page is one form: these settings interact (opening/closing time, the two
             loyalty rates), so they are saved together and rejected together. --%>
        <form method="post" action="${ctx}/admin/settings/save">
            <input type="hidden" name="csrfToken" value="${csrfToken}">

            <c:forEach var="group" items="${settingGroups}">
                <section class="settings-group">
                    <h2><c:out value="${group.key}" /></h2>
                    <div class="settings-grid">
                        <c:forEach var="s" items="${group.value}">
                            <div class="form-group">
                                <c:choose>
                                    <c:when test="${s.booleanType}">
                                        <%-- has_* marks the field as present: an unchecked box
                                             sends nothing, so without it the servlet cannot tell
                                             "turned off" from "not on this form". --%>
                                        <input type="hidden" name="has_${s.settingKey}" value="1">
                                        <label class="switch-row" for="set-${s.settingKey}">
                                            <input type="checkbox" class="switch-input" id="set-${s.settingKey}"
                                                   name="${s.settingKey}" value="true"
                                                   ${s.truthy ? 'checked' : ''}>
                                            <span class="switch-track" aria-hidden="true"><span class="switch-thumb"></span></span>
                                            <span class="switch-label"><c:out value="${s.displayName}" /></span>
                                        </label>
                                    </c:when>
                                    <c:otherwise>
                                        <label for="set-${s.settingKey}"><c:out value="${s.displayName}" /></label>
                                        <input type="${s.inputType}" id="set-${s.settingKey}"
                                               name="${s.settingKey}"
                                               value="<c:out value='${s.settingValue}'/>"
                                               <c:if test="${s.inputType == 'number'}">min="0" step="${s.inputStep}"</c:if>>
                                    </c:otherwise>
                                </c:choose>
                                <c:if test="${not empty s.hint}">
                                    <span class="hint"><c:out value="${s.hint}" /></span>
                                </c:if>
                                <c:if test="${not empty s.updatedByName}">
                                    <span class="hint">Sửa lần cuối bởi <c:out value="${s.updatedByName}" />
                                        lúc <c:out value="${s.updatedAtDisplay}" /></span>
                                </c:if>
                            </div>
                        </c:forEach>
                    </div>
                </section>
            </c:forEach>

            <button type="submit" class="btn btn-primary">Lưu cài đặt</button>
        </form>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
