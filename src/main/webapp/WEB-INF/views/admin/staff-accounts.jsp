<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
            <h1>Tài khoản nhân viên</h1>
            <c:if test="${sessionScope.user.permissions['roles.manage']}">
                <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/admin/roles">Quản lý vai trò &amp; phân quyền</a>
            </c:if>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-error"><c:out value="${error}" /></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/admin/staff/save"
              style="margin:20px 0; display:flex; gap:10px; align-items:end; flex-wrap:wrap;">
            <div class="form-group" style="margin-bottom:0;">
                <label for="fullName">Họ tên</label>
                <input type="text" id="fullName" name="fullName" required>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="username">Tên đăng nhập</label>
                <input type="text" id="username" name="username" required>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" required>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="phone">Điện thoại</label>
                <input type="tel" id="phone" name="phone">
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="password">Mật khẩu</label>
                <input type="password" id="password" name="password" minlength="6" required>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label for="roleId">Vai trò</label>
                <select id="roleId" name="roleId" required>
                    <c:forEach var="r" items="${assignableRoles}">
                        <option value="${r.roleId}"><c:out value="${r.displayName}" /></option>
                    </c:forEach>
                </select>
            </div>
            <button type="submit" class="btn btn-primary">Tạo tài khoản</button>
        </form>

        <table class="data-table">
            <thead>
                <tr><th>Họ tên</th><th>Tên đăng nhập</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th></th></tr>
            </thead>
            <tbody>
                <c:forEach var="u" items="${staff}">
                    <tr>
                        <td><c:out value="${u.fullName}" /></td>
                        <td><c:out value="${u.username}" /></td>
                        <td><c:out value="${u.email}" /></td>
                        <td><c:out value="${u.role.displayName}" /></td>
                        <td>
                            <c:choose>
                                <c:when test="${u.status == 'ACTIVE'}"><span class="badge badge-completed">Đang hoạt động</span></c:when>
                                <c:otherwise><span class="badge badge-cancelled">Đã khóa</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/admin/staff/toggle">
                                <input type="hidden" name="userId" value="${u.userId}">
                                <input type="hidden" name="status" value="${u.status == 'ACTIVE' ? 'DISABLED' : 'ACTIVE'}">
                                <button type="submit" class="btn btn-sm ${u.status == 'ACTIVE' ? 'btn-danger' : 'btn-secondary'}">
                                    ${u.status == 'ACTIVE' ? 'Khóa' : 'Mở khóa'}
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
