package com.eaut.canteen.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class User {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int userId;
    private String username;
    private String passwordHash;
    private String googleSub;
    private AuthProvider authProvider = AuthProvider.LOCAL;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private AccountStatus status;
    private Integer buildingId;
    private BigDecimal walletBalance = BigDecimal.ZERO;
    private boolean eautStudent;
    private int loyaltyPoints;
    private String studentId;
    private String className;
    private boolean onDuty;
    private LocalDateTime createdAt;

    /**
     * This actor's permission keys for the current session, as a Map so JSTL/EL can test
     * membership with {@code ${sessionScope.user.permissions['products.manage']}} (Set has no EL
     * bracket-index support; Map does). Populated once at login (see LoginServlet/
     * GoogleAuthServlet) from RoleDAO#findPermissionKeysForRole — never re-derived per request.
     * Empty/absent for User instances built only for display (e.g. "sold_by" on someone else's order).
     */
    private Map<String, Boolean> permissions = Map.of();

    public User() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Integer getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Integer buildingId) {
        this.buildingId = buildingId;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
    }

    /** Smart ID marker: true when this account's email was @eaut.edu.vn at signup — see AppConfig "smartId.discountPercent". */
    public boolean isEautStudent() {
        return eautStudent;
    }

    public void setEautStudent(boolean eautStudent) {
        this.eautStudent = eautStudent;
    }

    /** Tích điểm balance, earned on completed online orders — see AppConfig "loyalty.vndPerPoint" / "loyalty.redeemValuePerPoint". */
    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    /** MSSV — collected via the checkout info gate for @eaut.edu.vn customers; null for staff and non-student customers. */
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    /** Khoa/Lớp — collected alongside studentId. */
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    /** Staff self-reported "đang trực" status, shown on the sales/store Kanban boards. */
    public boolean isOnDuty() {
        return onDuty;
    }

    public void setOnDuty(boolean onDuty) {
        this.onDuty = onDuty;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions = permissions;
    }

    public boolean hasPermission(String permissionKey) {
        return Boolean.TRUE.equals(permissions.get(permissionKey));
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedAtDisplay() {
        return createdAt == null ? "" : createdAt.format(DISPLAY_FORMAT);
    }
}
