<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Chấm công nhân viên</h1>

        <form class="catalog-toolbar" method="get" action="${ctx}/admin/attendance">
            <span class="catalog-sort">
                <label for="from">Từ ngày</label>
                <input type="date" id="from" name="from" value="${from}">
                <label for="to">đến</label>
                <input type="date" id="to" name="to" value="${to}">
            </span>
            <span class="catalog-sort">
                <label for="staffId">Nhân viên</label>
                <select id="staffId" name="staffId">
                    <option value="">Tất cả</option>
                    <c:forEach var="s" items="${staffList}">
                        <option value="${s.userId}" ${selectedStaffId == s.userId ? 'selected' : ''}>
                            <c:out value="${s.fullName}" />
                        </option>
                    </c:forEach>
                </select>
                <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
            </span>
        </form>

        <c:choose>
            <c:when test="${empty records}">
                <div class="empty-state">
                    <h2>Không có ca làm nào</h2>
                    <p>Chưa có nhân viên nào chấm công trong khoảng thời gian này.</p>
                </div>
            </c:when>
            <c:otherwise>
                <h2 style="font-size:1rem;">Tổng giờ làm theo nhân viên</h2>
                <div class="table-scroll">
                    <table class="data-table">
                    <thead><tr><th>Nhân viên</th><th>Tổng giờ</th></tr></thead>
                    <tbody>
                        <c:forEach var="entry" items="${totalsByStaff}">
                            <tr>
                                <td><c:out value="${entry.key}" /></td>
                                <td><strong><c:out value="${entry.value}" /></strong></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                </div>

                <h2 style="font-size:1rem; margin-top:24px;">Chi tiết từng ca</h2>
                <div class="table-scroll">
                    <table class="data-table">
                    <thead>
                        <tr><th>Ngày</th><th>Nhân viên</th><th>Vai trò</th><th>Vào làm</th><th>Tan làm</th><th>Số giờ</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="r" items="${records}">
                            <tr>
                                <td><c:out value="${r.dateDisplay}" /></td>
                                <td><c:out value="${r.fullName}" /></td>
                                <td><c:out value="${r.roleDisplayName}" /></td>
                                <td><c:out value="${r.checkInDisplay}" /></td>
                                <td><c:out value="${r.checkOutDisplay}" /></td>
                                <td>
                                    <c:out value="${r.durationDisplay}" />
                                    <c:if test="${r.open}"><span class="badge badge-pending">đang trong ca</span></c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                </div>
                <p class="hint" style="margin-top:8px;">
                    Ca chưa tan làm được tính tới thời điểm hiện tại.
                </p>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
