package com.eaut.canteen.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StockImport {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private int importId;
    private int adminId;
    private String adminName;
    private String supplierName;
    private String note;
    private LocalDateTime importedAt;

    private String code;
    private Integer supplierId;
    private StockImportStatus status = StockImportStatus.DRAFT;
    private LocalDate expectedDate;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal otherCost = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private LocalDateTime receivedAt;
    private String receivedByName;

    private BigDecimal subtotal = BigDecimal.ZERO;
    private int lineCount;
    private int orderedQuantity;
    private int receivedQuantity;

    public int getImportId() {
        return importId;
    }

    public void setImportId(int importId) {
        this.importId = importId;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public String getImportedAtDisplay() {
        return importedAt == null ? "" : importedAt.format(DISPLAY_FORMAT);
    }

    // ---- Quy trình nhập hàng (từ migration 022) ----

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    public StockImportStatus getStatus() {
        return status;
    }

    public void setStatus(StockImportStatus status) {
        this.status = status;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public String getExpectedDateDisplay() {
        return expectedDate == null ? "" : expectedDate.format(DATE_FORMAT);
    }

    /** Dạng yyyy-MM-dd cho &lt;input type="date"&gt;, khác hẳn dạng đọc dd/MM/yyyy ở trên. */
    public String getExpectedDateInput() {
        return expectedDate == null ? "" : expectedDate.toString();
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
    }

    public BigDecimal getOtherCost() {
        return otherCost;
    }

    public void setOtherCost(BigDecimal otherCost) {
        this.otherCost = otherCost == null ? BigDecimal.ZERO : otherCost;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getReceivedAtDisplay() {
        return receivedAt == null ? "" : receivedAt.format(DISPLAY_FORMAT);
    }

    public String getReceivedByName() {
        return receivedByName;
    }

    public void setReceivedByName(String receivedByName) {
        this.receivedByName = receivedByName;
    }

    // ---- Số liệu tổng hợp từ các dòng, do DAO nạp vào ----

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
    }

    public int getLineCount() {
        return lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    public int getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(int orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public int getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(int receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    /**
     * Tổng phải trả nhà cung cấp: tiền hàng − chiết khấu + chi phí nhập.
     *
     * <p>Tính ở đây chứ không lưu thành cột, vì một con số lưu sẵn sẽ lệch ngay khi ai đó sửa một
     * dòng hàng — cùng lý do giỏ hàng không lưu tổng tiền. Chặn dưới ở 0 để một khoản chiết khấu
     * gõ nhầm quá tay không biến thành số âm mà kế toán phải đi tìm.
     */
    public BigDecimal getTotal() {
        BigDecimal total = subtotal.subtract(discountAmount).add(otherCost);
        return total.signum() < 0 ? BigDecimal.ZERO : total;
    }

    /** Còn nợ nhà cung cấp bao nhiêu. */
    public BigDecimal getDebt() {
        BigDecimal debt = getTotal().subtract(paidAmount);
        return debt.signum() < 0 ? BigDecimal.ZERO : debt;
    }

    public boolean isFullyPaid() {
        return getDebt().signum() == 0;
    }

    /** Số còn đang trên đường về — dùng cho cột "hàng đang về" ở trang tồn kho. */
    public int getPendingQuantity() {
        int pending = orderedQuantity - receivedQuantity;
        return pending < 0 ? 0 : pending;
    }
}
