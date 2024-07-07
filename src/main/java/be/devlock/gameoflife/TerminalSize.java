package be.devlock.gameoflife;

public class TerminalSize {
    private final int rows;
    private final int columns;

    public TerminalSize(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }
}
