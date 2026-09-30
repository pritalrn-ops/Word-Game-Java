package wordgame.model;

import java.io.Serializable;

/**
 * A single evaluated letter within a guess: the letter plus its status.
 * Status is one of: "green", "orange", "grey".
 * Equivalent to the {letter, status} dicts stored as JSON in the Python version.
 */
public class Cell implements Serializable {
    private static final long serialVersionUID = 1L;

    public String letter;
    public String status;

    public Cell() {
    }

    public Cell(String letter, String status) {
        this.letter = letter;
        this.status = status;
    }
}
