<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<jsp:include page="/WEB-INF/views/common/nav.jsp" />
<main class="page-content">
    <div class="container" style="max-width:600px;">
        <h1>${empty product ? 'Thêm sản phẩm' : 'Sửa sản phẩm'}</h1>

        <c:if test="${not empty error}">
            <div class="alert alert-error"><c:out value="${error}" /></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/admin/products/save" enctype="multipart/form-data">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <c:if test="${not empty product}">
                <input type="hidden" name="productId" value="${product.productId}">
            </c:if>

            <div class="form-group">
                <label for="name">Tên sản phẩm</label>
                <input type="text" id="name" name="name" value="${product.name}" required>
            </div>
            <div class="form-group">
                <label for="categoryId">Danh mục</label>
                <select id="categoryId" name="categoryId" required>
                    <c:forEach var="cat" items="${categories}">
                        <option value="${cat.categoryId}" ${product.categoryId == cat.categoryId ? 'selected' : ''}>
                            <c:out value="${cat.name}" />
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-group">
                <label for="description">Mô tả</label>
                <textarea id="description" name="description" rows="3">${product.description}</textarea>
            </div>
            <div class="form-group">
                <label for="price">Giá bán hiện tại (₫)</label>
                <input type="number" id="price" name="price" min="0" step="1000" value="${product.price}" required>
                <span class="hint">Giá thực tế tính tiền cho khách — luôn là giá này, kể cả khi có khuyến mãi bên dưới.</span>
            </div>
            <div class="form-group">
                <label for="originalPrice">Giá gốc trước khuyến mãi (₫, để trống nếu không có KM)</label>
                <input type="number" id="originalPrice" name="originalPrice" min="0" step="1000" value="${product.originalPrice}">
                <span class="hint">Chỉ khi giá này lớn hơn giá bán, sản phẩm mới hiện badge giảm giá + giá gạch ngang. Để trống hoặc xoá để tắt khuyến mãi.</span>
            </div>
            <div class="form-group">
                <label for="promoTargetQuantity">Mục tiêu số lượng khuyến mãi (để trống nếu không cần thanh tiến độ)</label>
                <input type="number" id="promoTargetQuantity" name="promoTargetQuantity" min="1" value="${product.promoTargetQuantity}">
                <span class="hint">Hiện thanh "Đã bán X/Y" — X tự tính từ đơn hàng đã hoàn thành thật, Y là số bạn nhập ở đây.</span>
            </div>
            <div class="form-group">
                <label for="unit">Đơn vị tính</label>
                <input type="text" id="unit" name="unit" value="${product.unit}" placeholder="phần, ly, chai...">
            </div>
            <div class="form-group">
                <label for="avgPrepMinutes">Thời gian chế biến trung bình (phút)</label>
                <input type="number" id="avgPrepMinutes" name="avgPrepMinutes" min="1" max="120" value="${empty product.productId ? 10 : product.avgPrepMinutes}">
                <span class="hint">Dùng để tính đồng hồ đếm ngược trên bảng bếp khi đơn được duyệt.</span>
            </div>
            <div class="form-group">
                <label for="image">Ảnh sản phẩm</label>
                <c:if test="${not empty product.imageFilename}">
                    <img src="${pageContext.request.contextPath}/images/${product.imageFilename}" alt="${product.name}"
                         style="width:120px; border-radius:8px; display:block; margin-bottom:8px;">
                </c:if>
                <input type="file" id="image" name="image" accept=".jpg,.jpeg,.png,.webp">
                <span class="hint">Định dạng JPG, PNG hoặc WEBP, tối đa 5MB.</span>
            </div>

            <button type="submit" class="btn btn-primary">Lưu sản phẩm</button>
            <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admin/products">Hủy</a>
        </form>
    </div>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
