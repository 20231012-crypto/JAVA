<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Hiệu ứng trang chủ</h1>
        <p class="hint">
            Áp dụng cho trang mua hàng của sinh viên. Có hiệu lực ngay ở lần tải trang sau,
            không cần triển khai lại.
        </p>

        <c:if test="${not empty sessionScope.actionMessage}">
            <div class="alert alert-success"><c:out value="${sessionScope.actionMessage}" /></div>
            <c:remove var="actionMessage" scope="session" />
        </c:if>

        <form method="post" action="${ctx}/admin/effects/save">
            <input type="hidden" name="csrfToken" value="${csrfToken}">

            <fieldset class="effect-choices">
                <legend>Chọn hiệu ứng</legend>
                <c:forEach var="opt" items="${['NONE','SNOW','FIREWORKS','LEAVES']}">
                    <label class="effect-choice ${effectsMode == opt ? 'is-selected' : ''}">
                        <input type="radio" name="mode" value="${opt}" ${effectsMode == opt ? 'checked' : ''}>
                        <span class="effect-choice-icon" aria-hidden="true">
                            <c:choose>
                                <c:when test="${opt == 'NONE'}">—</c:when>
                                <c:when test="${opt == 'SNOW'}">❄</c:when>
                                <c:when test="${opt == 'FIREWORKS'}">✦</c:when>
                                <c:otherwise>🍂</c:otherwise>
                            </c:choose>
                        </span>
                        <span class="effect-choice-name">
                            <c:choose>
                                <c:when test="${opt == 'NONE'}">Tắt</c:when>
                                <c:when test="${opt == 'SNOW'}">Tuyết rơi</c:when>
                                <c:when test="${opt == 'FIREWORKS'}">Pháo hoa</c:when>
                                <c:otherwise>Lá rơi</c:otherwise>
                            </c:choose>
                        </span>
                        <span class="hint">
                            <c:choose>
                                <c:when test="${opt == 'NONE'}">Giao diện sạch, không hiệu ứng</c:when>
                                <c:when test="${opt == 'SNOW'}">Giáng sinh, cuối năm</c:when>
                                <c:when test="${opt == 'FIREWORKS'}">Tết, lễ hội trường</c:when>
                                <c:otherwise>Mùa thu, khai giảng</c:otherwise>
                            </c:choose>
                        </span>
                    </label>
                </c:forEach>
            </fieldset>

            <div class="form-group" style="max-width:320px;">
                <label for="intensity">Mức độ</label>
                <select id="intensity" name="intensity">
                    <option value="1" ${effectsIntensity == 1 ? 'selected' : ''}>1 — Nhẹ</option>
                    <option value="2" ${effectsIntensity == 2 ? 'selected' : ''}>2 — Vừa</option>
                    <option value="3" ${effectsIntensity == 3 ? 'selected' : ''}>3 — Mạnh</option>
                </select>
                <span class="hint">
                    Mức càng cao càng nhiều hạt. Hiệu ứng tự tắt với người đã chọn giảm chuyển động
                    trong hệ điều hành, và tự dừng khi tab bị ẩn.
                </span>
            </div>

            <button type="submit" class="btn btn-primary">Lưu</button>
        </form>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
