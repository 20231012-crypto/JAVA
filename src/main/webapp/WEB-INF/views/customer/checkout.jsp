<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<c:set var="needsInfoGate" value="${empty customerPhone or (customerIsEautStudent and empty customerStudentId)}" />
<c:if test="${needsInfoGate}">
    <div style="position:fixed; inset:0; background:rgba(15,23,42,0.6); z-index:1000; display:flex; align-items:center; justify-content:center; padding:20px;">
        <div class="card" style="max-width:420px; width:100%; padding:28px;">
            <h2 style="margin-bottom:8px;">Thêm thông tin để đặt hàng</h2>
            <p class="hint" style="margin-bottom:20px;">Tài khoản Google của bạn chưa có sẵn số điện thoại<c:if test="${customerIsEautStudent}"> và MSSV</c:if> — chỉ cần nhập một lần.</p>
            <c:if test="${not empty sessionScope.phoneError}">
                <div class="alert alert-error"><c:out value="${sessionScope.phoneError}" /></div>
                <c:remove var="phoneError" scope="session" />
            </c:if>
            <form method="post" action="${pageContext.request.contextPath}/account/phone">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="redirect" value="${pageContext.request.contextPath}/checkout">
                <div class="form-group">
                    <label for="phoneInput">Số điện thoại</label>
                    <input type="tel" id="phoneInput" name="phone" value="${customerPhone}" placeholder="09xxxxxxxx" required autofocus>
                </div>
                <c:if test="${customerIsEautStudent}">
                    <div class="form-group">
                        <label for="studentIdInput">Mã số sinh viên (MSSV)</label>
                        <input type="text" id="studentIdInput" name="studentId" value="${customerStudentId}" placeholder="2023xxxx" required>
                    </div>
                    <div class="form-group">
                        <label for="classNameInput">Lớp / Khoa</label>
                        <input type="text" id="classNameInput" name="className" value="${customerClassName}" placeholder="K14 CNTT">
                    </div>
                </c:if>
                <button type="submit" class="btn btn-primary" style="width:100%;">Lưu &amp; tiếp tục đặt hàng</button>
            </form>
        </div>
    </div>
</c:if>
<main class="page-content">
    <div class="container">
        <h1>Thanh toán</h1>

        <c:if test="${not empty error}">
            <div class="alert alert-error"><c:out value="${error}" /></div>
        </c:if>

        <c:if test="${shopAcceptingOrders == false}">
            <div class="alert alert-error">🚫 Căng tin đang tạm ngưng nhận đơn (quá tải hoặc ngoài giờ phục vụ). Vui lòng quay lại sau.</div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/checkout/place" id="checkoutForm" class="show-spinner-on-submit">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <div class="checkout-layout">
                <div>
                    <div class="form-group">
                        <label for="buildingId">Giao đến tòa nhà</label>
                        <select id="buildingId" name="buildingId" required onchange="recalcCheckoutTotal()">
                            <option value="" data-fee="0">-- Chọn tòa nhà --</option>
                            <c:forEach var="b" items="${buildings}">
                                <option value="${b.buildingId}" data-fee="${b.shippingFee}">
                                    <c:out value="${b.name}" /> (phí ship <fmt:formatNumber value="${b.shippingFee}" type="number" groupingUsed="true" />₫)
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="note">Ghi chú (số phòng, tầng...)</label>
                        <textarea id="note" name="note" rows="3"></textarea>
                    </div>
                    <div class="form-group">
                        <label>Phương thức thanh toán</label>
                        <div style="display:flex; flex-direction:column; gap:8px; margin-top:8px;">
                            <label style="display:flex; align-items:center; gap:8px; font-weight:400;">
                                <input type="radio" name="paymentMethod" value="COD" checked style="width:auto;">
                                Thanh toán khi nhận hàng (COD)
                            </label>
                            <label style="display:flex; align-items:center; gap:8px; font-weight:400;">
                                <input type="radio" name="paymentMethod" value="VIETQR" style="width:auto;">
                                Chuyển khoản VietQR
                            </label>
                            <label style="display:flex; align-items:center; gap:8px; font-weight:400;">
                                <input type="radio" name="paymentMethod" value="WALLET" style="width:auto;"
                                       ${walletBalance <= 0 ? 'disabled' : ''}>
                                Ví EAUT Pay (số dư: <fmt:formatNumber value="${walletBalance}" type="number" groupingUsed="true" />₫)
                            </label>
                        </div>
                    </div>
                    <c:if test="${loyaltyPoints > 0}">
                        <div class="form-group">
                            <label style="display:flex; align-items:center; gap:8px; font-weight:400;">
                                <input type="checkbox" id="useLoyaltyPoints" name="useLoyaltyPoints" style="width:auto;" onchange="recalcCheckoutTotal()">
                                Dùng điểm tích luỹ (bạn có <strong>${loyaltyPoints}</strong> điểm, mỗi điểm = <fmt:formatNumber value="${redeemValuePerPoint}" type="number" groupingUsed="true" />đ)
                            </label>
                        </div>
                    </c:if>
                </div>

                <div class="order-summary">
                    <h2 style="margin-bottom:12px;">Đơn hàng của bạn</h2>
                    <c:forEach var="item" items="${cart.items}">
                        <div class="summary-row">
                            <span><c:out value="${item.productName}" /> x${item.quantity}</span>
                            <span><fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true" />₫</span>
                        </div>
                    </c:forEach>
                    <div class="summary-row">
                        <span>Tạm tính</span>
                        <span id="sumSubtotal"><fmt:formatNumber value="${cart.subtotal}" type="number" groupingUsed="true" />₫</span>
                    </div>
                    <div class="summary-row">
                        <span>Phí ship</span>
                        <span id="sumShipping">— chọn tòa nhà —</span>
                    </div>
                    <c:if test="${not empty smartIdDiscount}">
                        <div class="summary-row" style="color:var(--color-gold);">
                            <span class="smart-id-badge" style="padding:2px 8px;">🪪 EAUT Smart ID</span>
                            <span>-<fmt:formatNumber value="${smartIdDiscount}" type="number" groupingUsed="true" />₫</span>
                        </div>
                    </c:if>
                    <div class="summary-row" id="sumLoyaltyRow" hidden style="color:var(--color-gold);">
                        <span>🎁 Điểm tích luỹ</span>
                        <span id="sumLoyaltyValue">-0₫</span>
                    </div>
                    <div class="summary-row summary-total">
                        <span>Tổng cộng</span>
                        <span id="sumTotal"><fmt:formatNumber value="${cart.subtotal}" type="number" groupingUsed="true" />₫</span>
                    </div>
                    <button type="submit" class="btn btn-primary" style="width:100%; margin-top:16px;" ${shopAcceptingOrders == false ? 'disabled' : ''}>Đặt hàng</button>
                </div>
            </div>
        </form>
    </div>
</main>
<script>
    // Subtotal/discount are fixed server-side values embedded here; only the shipping fee (depends
    // on the <select>) and the loyalty checkbox change client-side, so only those two need
    // recomputing — this preview always matches what CheckoutServlet will actually charge because
    // it uses the exact same numbers (subtotal, Smart ID discount, redeem rate) the server used.
    var CHECKOUT_SUBTOTAL = <c:out value="${cart.subtotal}" />;
    var CHECKOUT_SMART_ID_DISCOUNT = <c:out value="${empty smartIdDiscount ? 0 : smartIdDiscount}" />;
    var CHECKOUT_LOYALTY_POINTS = <c:out value="${empty loyaltyPoints ? 0 : loyaltyPoints}" />;
    var CHECKOUT_REDEEM_VALUE_PER_POINT = <c:out value="${empty redeemValuePerPoint ? 0 : redeemValuePerPoint}" />;

    function formatVnd(n) {
        return Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".") + "₫";
    }

    function recalcCheckoutTotal() {
        var select = document.getElementById("buildingId");
        var selectedOption = select.options[select.selectedIndex];
        var fee = selectedOption ? Number(selectedOption.getAttribute("data-fee")) || 0 : 0;
        var hasBuilding = select.value !== "";

        document.getElementById("sumShipping").textContent = hasBuilding ? formatVnd(fee) : "— chọn tòa nhà —";

        var payableBeforeLoyalty = CHECKOUT_SUBTOTAL + fee - CHECKOUT_SMART_ID_DISCOUNT;

        var useLoyalty = document.getElementById("useLoyaltyPoints");
        var loyaltyRow = document.getElementById("sumLoyaltyRow");
        var loyaltyDiscount = 0;
        if (useLoyalty && useLoyalty.checked && CHECKOUT_REDEEM_VALUE_PER_POINT > 0) {
            var maxRedeemable = Math.floor(payableBeforeLoyalty / CHECKOUT_REDEEM_VALUE_PER_POINT);
            var pointsUsed = Math.min(CHECKOUT_LOYALTY_POINTS, maxRedeemable);
            loyaltyDiscount = pointsUsed * CHECKOUT_REDEEM_VALUE_PER_POINT;
            loyaltyRow.hidden = pointsUsed <= 0;
            document.getElementById("sumLoyaltyValue").textContent = "-" + formatVnd(loyaltyDiscount) + " (" + pointsUsed + " điểm)";
        } else if (loyaltyRow) {
            loyaltyRow.hidden = true;
        }

        document.getElementById("sumTotal").textContent = formatVnd(Math.max(0, payableBeforeLoyalty - loyaltyDiscount));
    }

    recalcCheckoutTotal();
</script>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
