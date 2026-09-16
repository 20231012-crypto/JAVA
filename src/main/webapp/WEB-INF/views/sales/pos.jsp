<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- The till does not include common/header.jsp, which is where every other page picks this up.
     Without it fmt:formatNumber falls back to the JVM's locale and prints 50000 instead of
     50.000 — on a screen whose entire job is showing prices. --%>
<fmt:setLocale value="vi_VN" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bán hàng tại quầy - Căng tin EAUT</title>
    <link rel="icon" href="${ctx}/assets/images/brand/eaut-logo.jpg">
    <link rel="stylesheet" href="${ctx}/assets/css/style.css">
    <link rel="stylesheet" href="${ctx}/assets/css/pos.css">
</head>
<%-- The till has its own chrome — no site header, no nav, no footer. It is a full-screen tool on a
     fixed machine, and every pixel spent on site furniture is a pixel not spent on the order. --%>
<body class="pos-body" data-context-path="${ctx}" data-csrf="${csrfToken}">
<jsp:include page="/WEB-INF/views/common/_icon-sprite.jsp" />

<div class="pos-shell" id="pos-shell">

    <header class="pos-topbar">
        <a class="pos-brand" href="${ctx}/admin" title="Về trang quản trị">
            <span class="pos-brand-mark">S</span>
        </a>

        <%-- F3 focuses this. A cashier's hands stay on the keyboard, so the search is the primary
             way items get added; the dropdown below it is filled by pos.js. --%>
        <div class="pos-search">
            <svg class="icon" aria-hidden="true"><use href="#i-search"/></svg>
            <label class="visually-hidden" for="pos-product-search">Tìm sản phẩm</label>
            <input type="search" id="pos-product-search" autocomplete="off"
                   placeholder="Nhập tên sản phẩm hoặc mã SKU (F3)" data-product-search>
            <div class="pos-suggest" data-product-results hidden></div>
        </div>

        <%-- Order tabs. Each is a separate form post so switching works with no JavaScript. --%>
        <nav class="pos-tabs" aria-label="Các đơn đang mở">
            <c:forEach var="t" items="${tabs}">
                <div class="pos-tab ${t.id eq tab.id ? 'is-active' : ''}">
                    <form method="post" action="${ctx}/pos/tab" class="pos-tab-form">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="action" value="switch">
                        <input type="hidden" name="tabId" value="${t.id}">
                        <button type="submit" class="pos-tab-btn">
                            <span class="pos-tab-dot" aria-hidden="true"></span>
                            <c:out value="${t.label}" />
                            <c:if test="${t.itemCount > 0}"><span class="pos-tab-count">${t.itemCount}</span></c:if>
                        </button>
                    </form>
                    <form method="post" action="${ctx}/pos/tab" class="pos-tab-form">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="action" value="close">
                        <input type="hidden" name="tabId" value="${t.id}">
                        <button type="submit" class="pos-tab-close" aria-label="Đóng ${t.label}">&times;</button>
                    </form>
                </div>
            </c:forEach>
            <form method="post" action="${ctx}/pos/tab" class="pos-tab-form">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="new">
                <button type="submit" class="pos-tab-add" aria-label="Mở đơn mới">+</button>
            </form>
        </nav>

        <div class="pos-topbar-right">
            <span class="pos-branch">
                <svg class="icon" aria-hidden="true"><use href="#i-building"/></svg>
                <span>
                    <small>Căng tin</small>
                    Đại học Công nghệ Đông Á
                </span>
            </span>
            <span class="pos-avatar" title="${fn:escapeXml(staff.fullName)}">
                <c:out value="${fn:substring(staff.fullName, 0, 1)}" />
            </span>
        </div>
    </header>

    <div class="pos-main">

        <aside class="pos-rail" aria-label="Chuyển kênh">
            <a class="pos-rail-link is-active" href="${ctx}/pos" title="Bán hàng tại quầy">
                <svg class="icon" aria-hidden="true"><use href="#i-store"/></svg>
            </a>
            <a class="pos-rail-link" href="${ctx}/sales/orders" title="Đơn đặt online">
                <svg class="icon" aria-hidden="true"><use href="#i-receipt"/></svg>
            </a>
            <a class="pos-rail-link" href="${ctx}/products" target="_blank" title="Xem website bán hàng">
                <svg class="icon" aria-hidden="true"><use href="#i-utensils"/></svg>
            </a>
            <a class="pos-rail-link" href="${ctx}/attendance" title="Chấm công">
                <svg class="icon" aria-hidden="true"><use href="#i-clock"/></svg>
            </a>
            <a class="pos-rail-link pos-rail-bottom" href="${ctx}/admin" title="Về trang quản trị">
                <svg class="icon" aria-hidden="true"><use href="#i-dashboard"/></svg>
            </a>
        </aside>

        <section class="pos-order">
            <div class="pos-order-head">
                <span class="pos-order-title">Sản phẩm (<strong>${tab.itemCount}</strong>)</span>

                <form method="post" action="${ctx}/pos/cart" class="pos-split">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="split">
                    <label>
                        <input type="checkbox" name="splitLines" ${tab.splitLines ? 'checked' : ''}
                               onchange="this.form.submit()">
                        Tách dòng sản phẩm
                    </label>
                    <span class="pos-hint-dot" title="Khi bật, thêm cùng một món hai lần sẽ tạo hai dòng riêng thay vì cộng số lượng.">i</span>
                    <noscript><button type="submit" class="btn btn-sm btn-secondary">Áp dụng</button></noscript>
                </form>

                <span class="pos-col-price">Đơn giá</span>
                <span class="pos-col-qty">Số lượng</span>
                <span class="pos-col-total">Thành tiền</span>
            </div>

            <c:if test="${not empty posError}">
                <div class="alert alert-error pos-alert"><c:out value="${posError}" /></div>
            </c:if>
            <c:if test="${not empty posReceiptOrderCode}">
                <div class="alert alert-success pos-alert">
                    Đã thanh toán đơn <strong><c:out value="${posReceiptOrderCode}" /></strong>.
                </div>
            </c:if>

            <div class="pos-lines">
                <c:choose>
                    <c:when test="${tab.itemCount == 0}">
                        <div class="pos-empty">
                            <svg class="pos-empty-art" viewBox="0 0 120 100" aria-hidden="true">
                                <path d="M20 38h80l-8 46a6 6 0 0 1-6 5H34a6 6 0 0 1-6-5z"/>
                                <path d="M20 38 34 18h52l14 20"/>
                                <path d="M48 38v-8a12 12 0 0 1 24 0v8"/>
                                <circle cx="60" cy="62" r="11"/>
                                <path d="M60 55v14M56.5 58.5h7M56.5 65.5h7"/>
                            </svg>
                            <p>Bạn chưa thêm sản phẩm nào</p>
                            <p class="pos-empty-hint">Ấn <kbd>F3</kbd> để tìm kiếm nhanh sản phẩm</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="item" items="${tab.cart.items}">
                            <div class="pos-line">
                                <div class="pos-line-name">
                                    <c:out value="${item.productName}" />
                                    <c:if test="${item.productId < 0}"><span class="pos-badge-custom">tùy chỉnh</span></c:if>
                                </div>
                                <div class="pos-col-price">
                                    <fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true" />₫
                                </div>
                                <div class="pos-col-qty">
                                    <%-- A number input plus a submit, rather than +/- buttons: a
                                         cashier ringing up twelve of something types 12. --%>
                                    <form method="post" action="${ctx}/pos/cart" class="pos-qty-form">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="action" value="qty">
                                        <input type="hidden" name="productId" value="${item.productId}">
                                        <label class="visually-hidden" for="qty-${item.productId}">Số lượng <c:out value="${item.productName}" /></label>
                                        <input type="number" id="qty-${item.productId}" name="quantity"
                                               value="${item.quantity}" min="0" max="999" data-qty-input>
                                        <noscript><button type="submit" class="btn btn-sm btn-secondary">Sửa</button></noscript>
                                    </form>
                                </div>
                                <div class="pos-col-total">
                                    <fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true" />₫
                                </div>
                                <form method="post" action="${ctx}/pos/cart" class="pos-line-remove">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="action" value="remove">
                                    <input type="hidden" name="productId" value="${item.productId}">
                                    <button type="submit" aria-label="Xóa ${item.productName}">
                                        <svg class="icon" aria-hidden="true"><use href="#i-x"/></svg>
                                    </button>
                                </form>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="pos-order-foot">
                <form method="post" action="${ctx}/pos/cart" class="pos-note-form">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="note">
                    <label class="visually-hidden" for="pos-note">Ghi chú đơn hàng</label>
                    <input type="text" id="pos-note" name="note" value="<c:out value='${tab.note}'/>"
                           placeholder="Nhập ghi chú đơn hàng" maxlength="200" data-note-input>
                    <noscript><button type="submit" class="btn btn-sm btn-secondary">Lưu</button></noscript>
                </form>

                <span class="pos-staff">Nhân viên: <strong><c:out value="${staff.fullName}" /></strong></span>

                <button type="button" class="pos-custom-btn" data-open-custom>
                    <svg class="icon" aria-hidden="true"><use href="#i-package"/></svg>
                    Sản phẩm tùy chỉnh <kbd>F2</kbd>
                </button>
            </div>
        </section>

        <aside class="pos-pay">
            <div class="pos-customer">
                <svg class="icon" aria-hidden="true"><use href="#i-users"/></svg>
                <c:choose>
                    <c:when test="${empty tab.customerId}">
                        <label class="visually-hidden" for="pos-customer-search">Tìm kiếm khách hàng</label>
                        <input type="search" id="pos-customer-search" autocomplete="off"
                               placeholder="Tìm kiếm khách hàng" data-customer-search>
                        <div class="pos-suggest" data-customer-results hidden></div>
                    </c:when>
                    <c:otherwise>
                        <span class="pos-customer-name"><c:out value="${tab.customerName}" /></span>
                        <form method="post" action="${ctx}/pos/cart">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="action" value="clear-customer">
                            <button type="submit" class="pos-customer-clear" aria-label="Bỏ chọn khách hàng">&times;</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="pos-pay-body">
                <h2 class="pos-pay-title">Thanh toán</h2>

                <div class="pos-pay-row">
                    <span>Tổng tiền hàng <small>(${tab.itemCount} sản phẩm)</small></span>
                    <span><fmt:formatNumber value="${tab.subtotal}" type="number" groupingUsed="true" />₫</span>
                </div>

                <form method="post" action="${ctx}/pos/cart" class="pos-pay-row pos-discount-row">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="discount">
                    <label for="pos-discount">Thêm giảm giá <kbd>F6</kbd></label>
                    <input type="text" id="pos-discount" name="discount" inputmode="numeric"
                           value="${tab.discount.signum() == 0 ? '' : tab.discount.toPlainString()}"
                           placeholder="0" data-discount-input>
                    <noscript><button type="submit" class="btn btn-sm btn-secondary">Áp dụng</button></noscript>
                </form>

                <div class="pos-pay-row pos-pay-total">
                    <span>Khách phải trả</span>
                    <span><fmt:formatNumber value="${tab.total}" type="number" groupingUsed="true" />₫</span>
                </div>

                <label class="pos-check">
                    <input type="checkbox" id="pos-autoprint" checked data-autoprint>
                    In hóa đơn tự động <kbd>F10</kbd>
                </label>
            </div>

            <form method="post" action="${ctx}/pos/pay" class="pos-pay-actions" data-pay-form>
                <input type="hidden" name="csrfToken" value="${csrfToken}">

                <div class="pos-method">
                    <span class="pos-method-label">Phương thức</span>
                    <%-- Cash first: it is what most counter sales are, and the first radio is the
                         one a hurried cashier will leave selected. --%>
                    <label><input type="radio" name="paymentMethod" value="CASH" checked> Tiền mặt</label>
                    <label><input type="radio" name="paymentMethod" value="VIETQR"> VietQR</label>
                    <label><input type="radio" name="paymentMethod" value="WALLET"> Ví EAUT Pay</label>
                </div>

                <div class="pos-actions">
                    <button type="button" class="btn btn-secondary" data-print-draft>In tạm tính</button>
                    <button type="submit" class="btn btn-primary" ${tab.itemCount == 0 ? 'disabled' : ''}>
                        Thanh toán <kbd>F9</kbd>
                    </button>
                </div>
            </form>
        </aside>
    </div>
</div>

<%-- Custom product dialog (F2). Plain form, so it still works without JavaScript — the button that
     opens it is the only part that needs it, and the fields are reachable by Tab regardless. --%>
<div class="pos-modal" id="pos-custom-modal" hidden role="dialog" aria-modal="true"
     aria-label="Thêm sản phẩm tùy chỉnh">
    <form class="pos-modal-box" method="post" action="${ctx}/pos/cart">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <input type="hidden" name="action" value="custom">
        <h2>Sản phẩm tùy chỉnh</h2>
        <p class="hint">Dùng cho món không có trong thực đơn. Không trừ tồn kho và không tính thuế.</p>
        <div class="form-group">
            <label for="custom-name">Tên sản phẩm</label>
            <input type="text" id="custom-name" name="name" maxlength="150" required>
        </div>
        <div class="form-group">
            <label for="custom-price">Đơn giá (₫)</label>
            <input type="text" id="custom-price" name="price" inputmode="numeric" required>
        </div>
        <div class="form-group">
            <label for="custom-qty">Số lượng</label>
            <input type="number" id="custom-qty" name="quantity" value="1" min="1" max="999">
        </div>
        <div class="pos-actions">
            <button type="button" class="btn btn-secondary" data-close-custom>Hủy</button>
            <button type="submit" class="btn btn-primary">Thêm vào đơn</button>
        </div>
    </form>
</div>

<%-- Hidden add-to-cart form the search dropdown submits. Keeping one form here rather than one per
     suggestion means the dropdown can be rebuilt freely without rebuilding forms. --%>
<form method="post" action="${ctx}/pos/cart" id="pos-add-form" hidden>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <input type="hidden" name="action" value="add">
    <input type="hidden" name="productId" id="pos-add-product">
    <input type="hidden" name="quantity" value="1">
</form>
<form method="post" action="${ctx}/pos/cart" id="pos-customer-form" hidden>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <input type="hidden" name="action" value="customer">
    <input type="hidden" name="customerId" id="pos-customer-id">
</form>

<script src="${ctx}/assets/js/pos.js" defer></script>
</body>
</html>
