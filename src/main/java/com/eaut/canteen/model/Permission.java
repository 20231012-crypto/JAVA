package com.eaut.canteen.model;

/**
 * One row of the fixed permissions catalog — one per real, enforced action/functional area (see
 * SecurityFilter's rule table). This catalog is not admin-editable: adding a permission means
 * adding the enforcement code for it, not just a database row. What IS admin-editable is which
 * roles hold which of these (see RoleDAO#setRolePermissions, /admin/roles).
 */
public class Permission {

    private String permissionKey;
    private String groupName;
    private String displayName;
    private int sortOrder;

    public String getPermissionKey() {
        return permissionKey;
    }

    public void setPermissionKey(String permissionKey) {
        this.permissionKey = permissionKey;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
