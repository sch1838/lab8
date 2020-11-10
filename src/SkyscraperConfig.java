import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Represents a single configuration in the skyscraper puzzle.
 *
 * @author RIT CS
 * @author Samuel Henderson
 */
public class SkyscraperConfig implements Configuration {
    /** empty cell value */
    public final static int EMPTY = 0;

    /** empty cell value display */
    public final static String EMPTY_CELL = ".";

    private final int[][] grid;
    private final List<Integer> NESW = new ArrayList<>();

    private final int gridSize;
    private Focus gridFocus = null;

    /**
     * Constructor
     *
     * @param filename the filename
     *  <p>
     *  Read the board file.  It is organized as follows:
     *  DIM     # square DIMension of board (1-9)
     *  lookNS   # DIM values (1-DIM) left to right
     *  lookEW   # DIM values (1-DIM) top to bottom
     *  lookSN   # DIM values (1-DIM) left to right
     *  lookWE   # DIM values (1-DIM) top to bottom
     *  row 1 values    # 0 for empty, (1-DIM) otherwise
     *  row 2 values    # 0 for empty, (1-DIM) otherwise
     *  ...
     *
     *  @throws FileNotFoundException if file not found
     */
    SkyscraperConfig(String filename) throws FileNotFoundException {
        Scanner f = new Scanner(new File(filename));

        if(f.hasNextInt()) {
            // Initialize grid size and the grid array
            this.gridSize = f.nextInt();
            this.grid = new int[this.gridSize][this.gridSize];

            // Iterate over 4 + n lines - four lines hold the edge values and n hold the initial grid values
            // Doubling occurs to cover edge values without additional loops
            for(int row = 0; row < 4 + this.gridSize; row ++) {
                for(int col = 0; col < this.gridSize; col ++) {
                    if(f.hasNextInt()) {
                        int value = f.nextInt();

                        if (this.gridFocus == null && value == EMPTY) {
                            // Store the first empty grid index as the focus for solving (only once while scanning)
                            // This occurs here to prevent unnecessary future iteration to determine the focus for
                            // generating successors
                            this.gridFocus = new Focus(row - 4, col);
                        }

                        if(row < 4) {
                            // Add value to edge counts
                            this.NESW.add(value);
                        } else {
                            // Insert value into grid
                            this.grid[row - 4][col] = value;
                        }
                    } else {
                        // Cease inner iteration if there are no more integers to read
                        break;
                    }
                }
            }
        } else {
            // Everything in here is as empty as the provided file
            this.gridSize = 0;
            this.grid = new int[0][0];
        }

        // close the input file
        f.close();
    }

    /**
     * Copy constructor
     *
     * @param copy SkyscraperConfig instance
     */
    public SkyscraperConfig(SkyscraperConfig copy) {
        // gridSize is copied from the grid length in the field argument constructor

        // Pass a clone of the provided config's grid so modifying it will not affect the grid of this config
        this(copy.grid.clone(), copy.NESW, copy.gridFocus);
    }

    /**
     * Constructs a new SkyscraperConfig from some elements of an existing or uncreated other.
     *
     * @param grid The integer grid - this must be a clone or discarded after it is used here
     * @param nesw The list of edge values organized by direction
     */
    private SkyscraperConfig(int[][] grid, List<Integer> nesw, Focus gridFocus) {
        this.gridSize = grid.length;
        // No need to clone since the array reference will not be modified elsewhere
        this.grid = grid;
        this.NESW.addAll(nesw);
        this.gridFocus = new Focus(gridFocus.row(), gridFocus.col());
    }

    @Override
    public boolean isGoal() {

        return this.gridFocus.complete(this);

        // Is the matrix full
    }

    /**
     * Provides the valid successors to the current SkyscraperConfig.
     *
     * @return A collection of valid Configurations
     */
    @Override
    public Collection<Configuration> getSuccessors() {

        List<Configuration> validConfigurations = new ArrayList<>();

        for (int val = 1; val <= this.gridSize; val ++) {
            int[][] testSuccessor = new int[this.gridSize][this.gridSize];

            for (int row = 0; row < this.grid.length; row++) {
                testSuccessor[row] = this.grid[row].clone();
            }

            testSuccessor[this.gridFocus.row()][this.gridFocus.col()] = val;

            if (validPlacement(testSuccessor)) {
                validConfigurations.add(new SkyscraperConfig(testSuccessor, this.NESW, Focus.createIncrement(this.gridFocus, this)));
            }
        }

        return validConfigurations;
    }

    /**
     * isValid() - checks if current config is valid
     *
     * @returns true if config is valid, false otherwise
     */
    @Override
    public boolean isValid() {

        int colLimit = 0;

        if(this.gridFocus.complete(this)) {
            colLimit = this.gridSize;
        } else if(this.gridFocus.row() == this.gridSize - 1) {
            colLimit = this.gridFocus.col();
        }

        int rowLimit = this.gridSize;

        if(this.gridFocus.row() < gridSize) {
            rowLimit = this.gridFocus.row();
        }

        Set<Integer>
                visibleN = new HashSet<>(),
                visibleS = new HashSet<>(),
                visibleE = new HashSet<>(),
                visibleW = new HashSet<>();

        for(int row = 0; row < rowLimit; row ++) {
            // If the focus is complete, iterate through all rows - otherwise all before focused row

            // The column scanned is dependent on the value of the outer loop, so it should be checked against focus

            // Initialize max east/west values to be the first on either edge
            // The last value checked from one direction is the first to be checked by the other
            int maxE = this.grid[row][this.gridSize - 1], maxW = this.grid[row][0];
            int maxS = this.grid[this.gridSize - 1][row], maxN = this.grid[0][row];

            // Get the edge values for this row
            int edgeE = getEdge(EAST,  row), edgeW = getEdge(WEST,  row);
            int edgeN = getEdge(NORTH, row), edgeS = getEdge(SOUTH, row);

            for (int col = 0; col < this.gridSize; col++) {

                int valW = this.grid[row][col], valE = this.grid[row][this.gridSize - 1 - col];
                int valN = this.grid[col][row], valS = this.grid[this.gridSize - 1 - col][row];

                if (compareAndAdd(maxW, valW, edgeW, visibleW) || compareAndAdd(maxE, valE, edgeE, visibleE)) {
                    return false;
                }

                maxW = Math.max(maxW, valW);
                maxE = Math.max(maxE, valE);

                if (row < colLimit) {
                    if (compareAndAdd(maxN, valN, edgeN, visibleN) || compareAndAdd(maxS, valS, edgeS, visibleS)) {
                        return false;
                    }

                    maxN = Math.max(maxN, valN);
                    maxS = Math.max(maxS, valS);
                }

            }

            if(edgeW != visibleW.size() || edgeE != visibleE.size()) {
                return false;
            }

            if(row < colLimit) {
                if(edgeN != visibleN.size() || edgeS != visibleS.size()) {
                    return false;
                }
            }

            visibleE.clear();
            visibleW.clear();
            visibleN.clear();
            visibleS.clear();
        }

        return true;
    }

    private boolean compareAndAdd(int max, int val, int edge, Set<Integer> set) {
        if(max <= val) {
            set.add(val);
            return edge < set.size();
        }

        return false;
    }

    /**
     * Verifies that the provided grid does not contain duplicates in the currently focused row and column.
     *
     * @param grid The grid to be scanned.
     */
    private boolean validPlacement(int[][] grid) {

        // Get the currently focused row and column
        int row = this.gridFocus.row(), col = this.gridFocus.col();

        // Get the new inserted value
        int value = grid[row][col];

        Set<Integer> visibleW = new HashSet<>(), visibleN = new HashSet<>();

        int maxW = grid[row][0], maxN = grid[0][col];

        int edgeW = this.getEdge(WEST, row), edgeN = this.getEdge(NORTH, col);

        for(int index = 0; index < this.gridSize; index ++) {
            int valW = grid[row][index], valN = grid[index][col];

            int oldValW = this.grid[row][index], oldValN = this.grid[index][col];

            if(oldValW == value || oldValN == value) {
                // Grid is not valid if any value in the row or column matches the inserted value
                return false;
            }

            if(compareAndAdd(maxW, valW, edgeW, visibleW) || compareAndAdd(maxN, valN, edgeN, visibleN)) {
                return false;
            }

            maxW = Math.max(maxW, valW); maxN = Math.max(maxN, valN);
        }

        return true;
    }

    private static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;

    private int getEdge(int lookDir, int index) {

        if(0 <= index && index < this.gridSize) {
            return this.NESW.get(lookDir * this.gridSize + index);
        }

        return -1;
    }

    /**
     * toString() method
     *
     * @return String representing configuration board & grid w/ look values.
     * The format of the output for the problem solving initial config is:
     *
     *   1 2 4 2
     *   --------
     * 1|. . . .|3
     * 2|. . . .|3
     * 3|. . . .|1
     * 3|. . . .|2
     *   --------
     *   4 2 1 2
     */
    @Override
    public String toString() {

        StringBuilder out = new StringBuilder(); int sze = this.gridSize;

        try {
            out.append("N: ").append(NESW.subList(0, sze)).append("\n");
            out.append("E: ").append(NESW.subList(sze, sze * 2)).append("\n");
            out.append("S: ").append(NESW.subList(sze * 2, sze * 3)).append("\n");
            out.append("W: ").append(NESW.subList(sze * 3, sze * 4)).append("\n");
        } catch (IndexOutOfBoundsException e) {
            // Except if there are less than gridSize^2 edge values
            return "Operation failed at SkyscraperConfig::toString(): Invalid edge values";
        }

        for (int[] ints : this.grid) {
            for (int col = 0; col < this.grid.length; col++) {
                // Add the value or an empty space to the end of the output String
                int val = ints[col];
                out.append(val == EMPTY ? EMPTY_CELL : val).append(" ");

                if (col + 1 == this.grid.length) {
                    // New line if this value is the last in its row
                    out.append("\n");
                }
            }
        }

        return out.toString();
    }

    private static class Focus {
        /* The current row and column of this Focus. */
        private int row, col;

        /**
         * Constructs a new Focus.
         *
         * @param row The starting row of this Focus
         * @param col The starting column of this Focus
         */
        protected Focus(int row, int col) {
            this.row = row; this.col = col;
        }

        protected static Focus createIncrement(Focus source, SkyscraperConfig config) {
            Focus focus = new Focus(source.row(), source.col());
            focus.increment(config);
            return focus;
        }

        /**
         * Handles forward movement of this Focus.
         *
         * The Focus will not move if it cannot do so without exceeding the bounds of the config grid - at this point,
         * the Focus is complete and will no longer increment.
         *
         * If the Focus is incremented, is not complete, and the value in the config grid that this Focus points to is
         * not empty, this method will call itself recursively until its Focus is complete or an empty value is found.
         *
         * Unless it is complete, the Focus should always point to the row and column of the next empty value in the
         * grid of the config.
         *
         * @param config The config to which this Focus is attached
         */
        protected void increment(SkyscraperConfig config) {
            if(this.complete(config)) {
                // Do not increment if the focus is complete
                return;
            }

            int sze = config.gridSize;

            if(this.col + 1 < sze) {
                // First increment column if it will not exceed the grid size
                this.col ++;
            } else if (this.row + 1 < sze) {
                // Otherwise, increment the row if it will not exceed the grid size and reset the column
                this.row ++;
                this.col = 0;
            }

            if(!this.complete(config)) {
                // If the focus is not complete (row and column can no longer be incremented), allow recursion
                if (config.grid[this.row][this.col] != EMPTY) {
                    // If the grid value at the row/column of this focus is not empty, attempt to increment again
                    increment(config);
                }
            }
        }

        /**
         * Handles reverse motion of this Focus.
         *
         * This Focus can only be decremented if it is not at (0, 0).
         *
         * If the Focus is decremented, it will become incomplete.
         *
         * @param config The config to which this Focus is attached
         */
        protected void decrement(SkyscraperConfig config) {
            if(this.col == 0 && this.row == 0) {
                // Cannot decrement if the focus is at (0, 0)
                return;
            }

            if(this.col == 0) {
                // If the Focus is at the first column, move up to the last column of the previous row
                this.row --;
                this.col = config.gridSize - 1;
            } else {
                // Otherwise, simply move back one column
                this.col --;
            }
        }

        /**
         * Access the current row of this Focus.
         */
        protected int row() {
            return this.row;
        }

        /**
         * Access the current column of this Focus.
         */
        protected int col() {
            return this.col;
        }

        protected boolean complete(SkyscraperConfig config) {
            int sze = config.gridSize;
            int val = config.grid[this.row][this.col];

            return this.row == sze - 1 && this.col == sze - 1 && val != EMPTY;
        }

        @Override
        public String toString() {
            return "[" + this.row + ":" + this.col + "]";
        }
    }
}
