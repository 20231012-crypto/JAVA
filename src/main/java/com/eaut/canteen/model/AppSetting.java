package com.eaut.canteen.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One editable business setting, with the metadata the settings screen needs to render itself.
 *
 * <p>The screen is generated from these rows rather than hand-written per field: {@code groupName}
 * becomes a section, {@code displayName} the label, {@code hint} the help text, and
 * {@code valueType} chooses the input control. Adding a setting is therefore one INSERT in a
 * migration, with no JSP to edit — which is the whole reason app_settings is key/value rather than
 * one wide row.
 */
public class AppSetting {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String settingKey;
    private String settingValue;
    private String valueType;
    private String groupName;
    private String displayName;
    private String hint;
    private int sortOrder;
    private LocalDateTime updatedAt;
    private String updatedByName;

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
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

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    /**
     * The HTML input type for this value. BOOL is handled separately in the view (it renders as a
     * switch, not a text field), so it is not represented here.
     */
    public String getInputType() {
        return switch (valueType == null ? "STRING" : valueType) {
            case "INT", "DECIMAL" -> "number";
            case "TIME" -> "time";
            default -> "text";
        };
    }

    /**
     * Named ...Type, not isBoolean(): EL reserves "boolean" as a keyword, so ${s.boolean} fails to
     * parse at JSP compile time rather than at build time.
     */
    public boolean isBooleanType() {
        return "BOOL".equals(valueType);
    }

    /** Lets the view render a checked switch without parsing the string itself. */
    public boolean isTruthy() {
        return "true".equalsIgnoreCase(settingValue) || "1".equals(settingValue);
    }

    /** DECIMAL needs a fractional step; INT must stay whole. */
    public String getInputStep() {
        return "DECIMAL".equals(valueType) ? "0.01" : "1";
    }

    public String getUpdatedAtDisplay() {
        return updatedAt == null ? "" : updatedAt.format(DISPLAY_FORMAT);
    }
}
