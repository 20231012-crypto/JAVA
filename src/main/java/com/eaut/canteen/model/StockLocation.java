package com.eaut.canteen.model;

/** Which of the two stock counters a movement applies to. Mirrors stock_movements.location. */
public enum StockLocation {
    WAREHOUSE("Kho"),
    SHELF("Kệ bán");

    private final String displayName;

    StockLocation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
