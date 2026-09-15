<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<main class="page-content">
    <div class="container">
        <h1>Vai trò &amp; phân quyền</h1>
        <p class="hint" style="margin:8px 0 20px;">
            Tạo thêm vai trò (đối tượng sử dụng) tuỳ ý và chọn những chức năng vai trò đó được phép dùng —
            không còn giới hạn ở 4 vai trò cố định. Khách hàng tự đăng ký qua Google nên không xuất hiện ở đây.
        </p>

        <c:if test="${not empty error}">
            <div class="alert alert-error"><c:out value="${error}" /></div>
        </c:if>

        <div class="card" style="padding:20px; margin-bottom:24px;">
            <h2 style="font-size:1rem; margin-bottom:12px;">Danh sách vai trò</h2>
            <div style="display:flex; flex-wrap:wrap; gap:10px; margin-bottom:16px;">
                <c:forEach var="r" items="${allRoles}">
                    <div class="badge ${r.customerDefault ? 'badge-confirmed' : 'badge-pending'}" style="display:flex; align-items:center; gap:8px; padding:6px 12px;">
                        <c:out value="${r.displayName}" /> · ${userCountByRole[r.roleId]} người
                        <c:if test="${!r.system}">
                            <form method="post" action="${ctx}/admin/roles/delete" style="display:inline;"
                                  onsubmit="return confirm('Xoá vai trò &quot;${r.displayName}&quot;?');">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="roleId" value="${r.roleId}">
                                <button type="submit" style="border:none; background:none; color:var(--color-danger); cursor:pointer; font-weight:700;"><svg class="icon" aria-hidden="true"><use href="#i-x"/></svg></button>
                            </form>
                        </c:if>
                    </div>
                </c:forEach>
            </div>

            <form method="post" action="${ctx}/admin/roles/save" style="display:flex; gap:10px; align-items:end; flex-wrap:wrap;">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <div class="form-group" style="margin-bottom:0;">
                    <label for="displayName">Tên vai trò mới</label>
                    <input type="text" id="displayName" name="displayName" placeholder="Ví dụ: Kế toán" required>
                </div>
                <button type="submit" class="btn btn-secondary">+ Thêm vai trò</button>
            </form>
        </div>

        <form method="post" action="${ctx}/admin/roles/permissions">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <div style="overflow-x:auto;">
                <table class="data-table" style="min-width:600px;">
                    <thead>
                        <tr>
                            <th>Chức năng</th>
                            <c:forEach var="r" items="${editableRoles}">
                                <th style="text-align:center;"><c:out value="${r.displayName}" /></th>
                            </c:forEach>
                        </tr>
                    </thead>
                    <tbody>
                        <c:set var="lastGroup" value="" />
                        <c:forEach var="p" items="${permissions}">
                            <c:if test="${p.groupName != lastGroup}">
                                <tr style="background:var(--color-neutral-soft);">
                                    <td colspan="${1 + fn:length(editableRoles)}" style="font-weight:700; color:var(--color-text-muted);">
                                        <c:out value="${p.groupName}" />
                                    </td>
                                </tr>
                                <c:set var="lastGroup" value="${p.groupName}" />
                            </c:if>
                            <tr>
                                <td><c:out value="${p.displayName}" /></td>
                                <c:forEach var="r" items="${editableRoles}">
                                    <td style="text-align:center;">
                                        <input type="checkbox" style="width:18px; height:18px;"
                                               name="perm_${r.roleId}_${p.permissionKey}"
                                               ${permissionsByRole[r.roleId][p.permissionKey] ? 'checked' : ''}>
                                    </td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <button type="submit" class="btn btn-primary" style="margin-top:16px;">Lưu phân quyền</button>
        </form>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
