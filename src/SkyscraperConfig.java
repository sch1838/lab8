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
                out.append(val == 0 ? ". " : val + " ");

                if (col + 1 == this.grid.length) {
                    // New line if this value is the last in its row
                    out.append("\n");
                }
            }
        }

        return out.toString();
    }
}
