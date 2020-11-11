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
        // NOTE: gridSize is copied from the grid length in the field argument constructor

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
        // Null focus indicates that the grid was already solved - otherwise, only true when the focus has completed
        // for this SkyscraperConfig
        return this.gridFocus == null || this.gridFocus.complete(this);
    }

    /**
     * Provides the successors to the current SkyscraperConfig.
     *
     * The returned collection will exclude successors with rows or columns containing duplicate values, and some
     * where the new value would cause the number of visible buildings to exceed the required number from North or West.
     *
     * @return A collection of valid Configurations
     */
    @Override
    public Collection<Configuration> getSuccessors() {

        List<Configuration> validConfigurations = new ArrayList<>();

        for (int val = 1; val <= this.gridSize; val ++) {
            // Create empty test grid, fill with current values, and insert current test value
            int[][] testSuccessor = new int[this.gridSize][this.gridSize];

            for (int row = 0; row < this.grid.length; row++) {
                testSuccessor[row] = this.grid[row].clone();
            }

            testSuccessor[this.gridFocus.row()][this.gridFocus.col()] = val;

            // Early prune to eliminate some successors without creating a new SkyscraperConfig
            if (validPlacement(testSuccessor)) {
                validConfigurations.add(new SkyscraperConfig(testSuccessor, this.NESW, Focus.createIncrement(this.gridFocus, this)));
            }
        }

        return validConfigurations;
    }

    /**
     * isValid() - checks if current config is valid
     *
     * @return true if config is valid, false otherwise
     */
    @Override
    public boolean isValid() {

        // Limits the number of scanned columns - 0 or the number of filled columns
        int colLimit = 0;

        if(this.gridFocus.complete(this)) {
            colLimit = this.gridSize;
        } else if(this.gridFocus.row() == this.gridSize - 1) {
            colLimit = this.gridFocus.col();
        }

        // Limits the number of scanned rows - will not scan incomplete or empty rows
        int rowLimit = this.gridSize;

        if(this.gridFocus.row() < gridSize) {
            rowLimit = this.gridFocus.row();
        }

        // The sets of visible values for each direction - the functionality of these is the same as in validPlacement()
        Set<Integer>
                visibleN = new HashSet<>(),
                visibleS = new HashSet<>(),
                visibleE = new HashSet<>(),
                visibleW = new HashSet<>();

        // Scan laterally and longitudinally simultaneously
        for(int row = 0; row < rowLimit; row ++) {
            // Initialize max east/west values to be the first on either edge
            // The last value checked from one direction is the first to be checked by the other
            int maxE = this.grid[row][this.gridSize - 1], maxW = this.grid[row][0];
            int maxS = this.grid[this.gridSize - 1][row], maxN = this.grid[0][row];

            // Get the edge values for the row and column
            int edgeE = getEdge(EAST,  row), edgeW = getEdge(WEST,  row);
            int edgeN = getEdge(NORTH, row), edgeS = getEdge(SOUTH, row);

            for (int col = 0; col < this.gridSize; col++) {
                // Hold current values - column positions are inverse of row positions
                int valW = this.grid[row][col], valE = this.grid[row][this.gridSize - 1 - col];
                int valN = this.grid[col][row], valS = this.grid[this.gridSize - 1 - col][row];

                // Check that any new value has not caused excess visible values for any set and update maximums

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

            // The Configuration is invalid at this point if the number of visible values in any row or column
            // is not equal to the number required by the corresponding edge value

            if(edgeW != visibleW.size() || edgeE != visibleE.size()) {
                return false;
            }

            if(row < colLimit) {
                if(edgeN != visibleN.size() || edgeS != visibleS.size()) {
                    return false;
                }
            }

            // Clear all visible sets for use with upcoming row/column
            visibleE.clear();
            visibleW.clear();
            visibleN.clear();
            visibleS.clear();
        }

        return true;
    }

    /**
     * Attempts to add a value to a set of visible values and determines whether the size of the set thereafter exceeds
     * its edge value.
     *
     * A result of false indicates that the edge requisite has not been exceeded by the size of the set.
     *
     * A value will only be added to a set if it is greater than the maximum value before it - this indicates that the
     * building it represents is taller than any others before it when looking from a particular direction.
     *
     * @param max The current maximum value for a row and direction
     * @param val The value that may be added to the set
     * @param edge The edge value representing the required size of the set
     * @param set The set of visible values for a row and direction
     */
    private boolean compareAndAdd(int max, int val, int edge, Set<Integer> set) {
        if(max <= val) {
            // Add if visible
            set.add(val);
            return edge < set.size();
        }

        return false;
    }

    /**
     * Verifies that the provided grid does not contain duplicates in the currently focused row and column.
     *
     * Checks as much of the grid as is filled to determine if the new value will immediately disrupt the edge values.
     * This is only checked from the North and West directions, as the East and South will sometimes depend on values
     * that have not yet been placed.
     *
     * An important distinction is herein made between the provided grid and the grid of this SkyscraperConfiguration.
     * Values from this grid are used to prevent duplicates because the new grid will contain the newly placed value,
     * triggering a false negative when it is reached iteratively.
     *
     * This method is called on a SkyscraperConfig when generating its successors; the parameter grid is thus a
     * successor with a new value already placed at the position determined by the gridFocus of this config.
     *
     * @param grid The grid to be scanned.
     */
    private boolean validPlacement(int[][] grid) {

        // Get the currently focused row and column from gridFocus
        int row = this.gridFocus.row(), col = this.gridFocus.col();

        // Get the new inserted value
        int value = grid[row][col];

        // Store the visible values from West and North directions
        Set<Integer> visibleW = new HashSet<>(), visibleN = new HashSet<>();

        // Initial maximum values are the first in the row and column
        int maxW = grid[row][0], maxN = grid[0][col];

        // Edge values for looking directions are needed to check that the size of either set of visible values has
        // exceeded the edge value for its corresponding row or column
        int edgeW = this.getEdge(WEST, row), edgeN = this.getEdge(NORTH, col);

        // Iterate over the row and column simultaneously
        for(int index = 0; index < this.gridSize; index ++) {
            // Values from successor grid
            int valW = grid[row][index], valN = grid[index][col];

            // Values from this grid
            int oldValW = this.grid[row][index], oldValN = this.grid[index][col];

            if(oldValW == value || oldValN == value) {
                // Grid is not valid if any value in the row or column matches the inserted value
                return false;
            }

            // Attempt to add the current successor values to their corresponding sets - if they are added and this
            // causes the size of either set to exceed its edge value, this method will return false and a successor
            // will not be created from the parameter grid
            if(compareAndAdd(maxW, valW, edgeW, visibleW) || compareAndAdd(maxN, valN, edgeN, visibleN)) {
                return false;
            }

            // Update the max values for visibility comparison - visibility in a direction is based on whether the
            // current value is taller than the maximum value before it
            maxW = Math.max(maxW, valW); maxN = Math.max(maxN, valN);
        }

        return true;
    }

    // Constants used in conjunction with getEdge as the lookDir parameter
    private static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;

    /**
     * Provides the edge value for a set direction and an index.
     *
     * @param lookDir The direction from which the buildings will be viewed
     * @param index The edge index local to the section of the NESW list occupied by one edge -> [0, gridSize)
     */
    private int getEdge(int lookDir, int index) {

        if(0 <= index && index < this.gridSize) {
            // The edge values of each direction start at n = [0, 4) * gridSize
            // For a gridSize of 4: NESW = [N0, N1, N2, N3, E0, ... E3, S0, ... S3, W0, ... W3]
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
            // Except if there are less than 4 * gridSize edge values
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

    /**
     * The Focus class represents a point in (row, column) format that refers to the next empty (0) value in a
     * SkyscraperConfig grid. If no such values exist in the grid, it represents the 'cursor' location of a config,
     * that is, the position in the grid that the config will resolve next.
     *
     * Focus instances should not be shared by configurations with different grid sizes.
     */
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

        /**
         * Creates a new Focus that is an incremented version of that which is provided.
         *
         * @param source The Focus to be copied and modified
         * @param config The config either Focus is attached to
         */
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

        /**
         * Provides the status of this Focus given a config.
         *
         * The Focus is complete only if it is at the last possible index of the config grid and the value at that
         * index is not empty (0).
         */
        protected boolean complete(SkyscraperConfig config) {
            int sze = config.gridSize;
            int val = config.grid[this.row][this.col];

            return this.row == sze - 1 && this.col == sze - 1 && val != EMPTY;
        }

        /**
         * [ROW:COL]
         */
        @Override
        public String toString() {
            return "[" + this.row + ":" + this.col + "]";
        }
    }
}
