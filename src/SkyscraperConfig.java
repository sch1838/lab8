import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Represents a single configuration in the skyscraper puzzle.
 *
 * @author RIT CS
 * @author YOUR NAME HERE
 */
public class SkyscraperConfig implements Configuration {
    /** empty cell value */
    public final static int EMPTY = 0;

    /** empty cell value display */
    public final static char EMPTY_CELL = '.';

    private final int gridSize;

    private final int[][] grid;

    private final List<Integer> NESW = new ArrayList<>();

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

            // Iterate over twice as many rows as the grid will have
            // Doubling occurs to cover edge values without additional loops
            for(int row = 0; row < this.gridSize * 2; row ++) {
                for(int col = 0; col < this.gridSize; col ++) {
                    if(f.hasNextInt()) {
                        int value = f.nextInt();

                        if (this.gridFocus == null && value == EMPTY) {
                            // Store the first empty grid index as the focus for solving (only once while scanning)
                            // This occurs here to prevent unnecessary future iteration to determine the focus for
                            // generating successors
                            this.gridFocus = new Focus(row, col);
                        }

                        if(row < this.gridSize) {
                            // Add value to edge counts
                            this.NESW.add(value);
                        } else {
                            // Insert value into grid
                            this.grid[row - this.gridSize][col] = value;
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
        // Initialize grid size and array using values from provided config
        this.gridSize = copy.gridSize;
        this.grid = new int[this.gridSize][this.gridSize];

        // Copy all edge values from provided config
        this.NESW.addAll(copy.NESW);

        for (int row = 0; row < this.grid.length; row ++) {
            // Copy each row of the provided config grid into the rows of this grid
            System.arraycopy(copy.grid[row], 0, this.grid[row], 0, this.grid.length);
        }
    }

    @Override
    public boolean isGoal() {

        // TODO

        return false; // remove after implementing
    }

    /**
     * getSuccessors
     *
     * @returns Collection of Configurations
     */
    @Override
    public Collection<Configuration> getSuccessors() {

        // TODO

        return new ArrayList<>();   // remove after implementing

    }

    /**
     * isValid() - checks if current config is valid
     *
     * @returns true if config is valid, false otherwise
     */
    @Override
    public boolean isValid() {

        // TODO

        return false;  // remove after implementing
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

        /* A Focus is complete when it cannot be incremented. */
        private boolean complete = false;

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
            if(this.complete) {
                // Do not increment if the focus is complete
                return;
            }

            int sze = config.gridSize;

            if(this.col + 1 < sze) {
                // First increment column if it will not exceed the grid size
                this.col ++;
            } else if (this.row < sze) {
                // Otherwise, increment the row if it will not exceed the grid size and reset the column
                this.row ++;
                this.col = 0;
            } else {
                // If row and column cannot increment, the focus has completed
                this.complete = true;
            }

            if(!complete) {
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

            // The Focus will only become incomplete if it has been decremented at all
            this.complete = false;
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
    }
}
