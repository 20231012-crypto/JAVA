package com.eaut.canteen.model;

public class Category {

    private int categoryId;
    private String name;
    /** NULL = top-level group (mega-menu column header, no products directly under it); set = a leaf sub-category products belong to. */
    private Integer parentCategoryId;
    private boolean active;

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(Integer parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }

    public boolean isTopLevel() {
        return parentCategoryId == null;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
