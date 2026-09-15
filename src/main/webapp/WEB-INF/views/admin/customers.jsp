<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container">
        <h1>Quản lý khách hàng</h1>

        <div class="catalog-toolbar">
            <span class="catalog-count"><strong>${totalCustomers}</strong> khách hàng</span>
            <form class="catalog-sort" method="get" action="${ctx}/admin/customers">
                <input type="search" name="q" value="<c:out value='${searchQuery}'/>"
                       placeholder="Tìm tên, tài khoản, email, SĐT..." aria-label="Tìm khách hàng"
                       style="padding:6px 10px; border:1px solid var(--color-border); border-radius:var(--radius-btn); min-width:220px;">
                <button type="submit" class="btn btn-secondary btn-sm">Tìm</button>
                <c:if test="${not empty searchQuery}">
                    <a class="btn btn-secondary btn-sm" href="${ctx}/admin/customers">Xóa lọc</a>
                </c:if>
            </form>
        </div>

        <c:choose>
            <c:when test="${empty customers}">
                <div class="empty-state">
                    <h2>Không có khách hàng nào</h2>
                    <c:if test="${not empty searchQuery}">
                        <p>Không tìm thấy khách hàng khớp với &ldquo;<c:out value="${searchQuery}" />&rdquo;.</p>
                    </c:if>
                </div>
            </c:when>
            <c:otherwise>
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Họ tên</th><th>Liên hệ</th><th>Số đơn</th><th>Tổng chi</th>
                            <th>Đơn gần nhất</th><th>Trạng thái</th><th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${customers}">
                            <tr>
                                <td>
                                    <c:out value="${row.user.fullName}" />
                                    <div class="hint"><c:out value="${row.user.username}" /></div>
                                </td>
                                <td>
                                    <c:out value="${row.user.email}" />
                                    <div class="hint">
                                        <c:choose>
                                            <c:when test="${not empty row.user.phone}"><c:out value="${row.user.phone}" /></c:when>
                                            <c:otherwise>Chưa có SĐT</c:otherwise>
                                        </c:choose>
                                    </div>
                                </td>
                                <td>${row.orderCount}</td>
                                <td><fmt:formatNumber value="${row.totalSpent}" type="number" groupingUsed="true" />₫</td>
                                <td><c:out value="${row.lastOrderAtDisplay}" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${row.user.status == 'ACTIVE'}">
                                            <span class="badge badge-completed">Đang hoạt động</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-cancelled">Đã khóa</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="table-actions">
                                    <form method="post" action="${ctx}/admin/customers/toggle">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="userId" value="${row.user.userId}">
                                        <input type="hidden" name="q" value="<c:out value='${searchQuery}'/>">
                                        <c:choose>
                                            <c:when test="${row.user.status == 'ACTIVE'}">
                                                <input type="hidden" name="status" value="DISABLED">
                                                <button type="submit" class="btn btn-sm btn-secondary">Khóa</button>
                                            </c:when>
                                            <c:otherwise>
                                                <input type="hidden" name="status" value="ACTIVE">
                                                <button type="submit" class="btn btn-sm btn-primary">Mở khóa</button>
                                            </c:otherwise>
                                        </c:choose>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>

                <c:if test="${totalPages > 1}">
                    <c:set var="filterQs"><c:if test="${not empty searchQuery}">&amp;q=${fn:escapeXml(searchQuery)}</c:if></c:set>
                    <nav class="pagination" aria-label="Phân trang khách hàng">
                        <c:choose>
                            <c:when test="${currentPage > 1}">
                                <a class="pagination-link" href="${ctx}/admin/customers?page=${currentPage - 1}${filterQs}">&#8249; Trước</a>
                            </c:when>
                            <c:otherwise><span class="pagination-link is-disabled">&#8249; Trước</span></c:otherwise>
                        </c:choose>
                        <c:forEach begin="1" end="${totalPages}" var="i">
                            <c:choose>
                                <c:when test="${i == currentPage}">
                                    <span class="pagination-link is-current" aria-current="page">${i}</span>
                                </c:when>
                                <c:otherwise>
                                    <a class="pagination-link" href="${ctx}/admin/customers?page=${i}${filterQs}">${i}</a>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                        <c:choose>
                            <c:when test="${currentPage < totalPages}">
                                <a class="pagination-link" href="${ctx}/admin/customers?page=${currentPage + 1}${filterQs}">Sau &#8250;</a>
                            </c:when>
                            <c:otherwise><span class="pagination-link is-disabled">Sau &#8250;</span></c:otherwise>
                        </c:choose>
                    </nav>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
