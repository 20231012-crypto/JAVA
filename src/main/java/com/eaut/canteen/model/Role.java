package com.eaut.canteen.model;

/**
 * A user type ("đối tượng sử dụng") — no longer a fixed 4-value enum. Roles are rows in the
 * {@code roles} table, created and renamed freely from /admin/roles; what a role can actually do
 * is the set of {@link Permission} keys granted to it via role_permissions (see RoleDAO,
 * SecurityFilter). {@code isSystem} only protects the four seeded roles from deletion — their
 * permissions are just as editable as any custom role's.
 */
public class Role {

    private int roleId;
    private String roleKey;
    private String displayName;
    private boolean system;
    private boolean customerDefault;

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    /** True for the one role assigned to new self-service Google sign-ups (see GoogleAuthServlet). */
    public boolean isCustomerDefault() {
        return customerDefault;
    }

    public void setCustomerDefault(boolean customerDefault) {
        this.customerDefault = customerDefault;
    }
}
