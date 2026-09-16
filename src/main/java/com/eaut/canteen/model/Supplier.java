package com.eaut.canteen.model;

import java.math.BigDecimal;

/**
 * Nhà cung cấp.
 *
 * <p>Trước đây đây chỉ là một ô chữ tự do trên phiếu nhập, nên "Cty Minh Anh", "minh anh" và
 * "Minh Anh " là ba nhà cung cấp khác nhau đối với hệ thống, và câu hỏi đơn giản nhất của việc
 * mua hàng — tháng này đã nhập bao nhiêu từ ai, còn nợ ai bao nhiêu — không trả lời được.
 */
public class Supplier {

    private int supplierId;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String note;
    private boolean active = true;

    /**
     * Số liệu tổng hợp, chỉ có khi đọc bằng findAllWithStats — không phải cột trong bảng.
     * Giữ ở đây thay vì tạo một lớp riêng vì màn hình duy nhất dùng chúng là danh sách NCC.
     */
    private int importCount;
    private BigDecimal totalValue = BigDecimal.ZERO;
    private BigDecimal totalDebt = BigDecimal.ZERO;

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getImportCount() {
        return importCount;
    }

    public void setImportCount(int importCount) {
        this.importCount = importCount;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(BigDecimal totalDebt) {
        this.totalDebt = totalDebt;
    }
}
