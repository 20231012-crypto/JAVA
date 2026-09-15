<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Chấm công</h1>

        <c:if test="${not empty attendanceMessage}">
            <div class="alert alert-success"><c:out value="${attendanceMessage}" /></div>
        </c:if>

        <div class="clock-panel">
            <c:choose>
                <c:when test="${not empty openShift}">
                    <div>
                        <div class="clock-state">Đang trong ca làm việc</div>
                        <div class="hint">
                            Vào làm lúc <strong><c:out value="${openShift.checkInDisplay}" /></strong>
                            ngày <c:out value="${openShift.dateDisplay}" />
                            · đã làm <strong><c:out value="${openShift.durationDisplay}" /></strong>
                        </div>
                    </div>
                    <form method="post" action="${ctx}/attendance/clock">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="action" value="out">
                        <button type="submit" class="btn btn-secondary">Tan làm</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <div>
                        <div class="clock-state">Chưa vào ca</div>
                        <div class="hint">Bấm "Vào làm" để bắt đầu tính giờ công.</div>
                    </div>
                    <form method="post" action="${ctx}/attendance/clock">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="action" value="in">
                        <button type="submit" class="btn btn-primary">Vào làm</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </div>

        <h2 style="font-size:1rem; margin-top:24px;">Lịch sử ca làm của tôi</h2>
        <c:choose>
            <c:when test="${empty myShifts}">
                <div class="empty-state">
                    <h2>Chưa có ca làm nào</h2>
                    <p>Lần chấm công đầu tiên của bạn sẽ hiện ở đây.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-scroll">
                    <table class="data-table">
                    <thead>
                        <tr><th>Ngày</th><th>Vào làm</th><th>Tan làm</th><th>Số giờ</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="s" items="${myShifts}">
                            <tr>
                                <td><c:out value="${s.dateDisplay}" /></td>
                                <td><c:out value="${s.checkInDisplay}" /></td>
                                <td><c:out value="${s.checkOutDisplay}" /></td>
                                <td>
                                    <c:out value="${s.durationDisplay}" />
                                    <c:if test="${s.open}"><span class="badge badge-pending">đang tính</span></c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
