package me.usainsrht.ujobs.gui;

/**
 * Where things go in the job menus: a header row, rows of jobs (seven per row, centered)
 * and a footer row holding the navigation buttons.
 */
public final class GuiLayout {

    private static final int COLUMNS = 9;
    private static final int JOBS_PER_ROW = 7;
    private static final int HEADER_AND_FOOTER_ROWS = 2;
    private static final int MAX_ROWS = 6;

    /** Number of jobs the menus have room for. */
    public static final int MAX_JOBS = (MAX_ROWS - HEADER_AND_FOOTER_ROWS) * JOBS_PER_ROW;

    private GuiLayout() {
    }

    public static int rows(int jobCount) {
        int rows = (int) Math.ceil(jobCount / (float) JOBS_PER_ROW) + HEADER_AND_FOOTER_ROWS;
        return Math.min(rows, MAX_ROWS);
    }

    /** Slot of the job at the given 1-based position, or -1 when the menu has no room for it. */
    static int jobSlot(int position, int inventorySize) {
        int index = position - 1;
        int slot = (index / JOBS_PER_ROW + 1) * COLUMNS + index % JOBS_PER_ROW + 1;
        return slot < inventorySize - COLUMNS ? slot : -1;
    }

    static int leftButtonSlot(int inventorySize) {
        return inventorySize - COLUMNS;
    }

    static int rightButtonSlot(int inventorySize) {
        return inventorySize - 1;
    }
}
