package wordgame.model;

import java.io.Serializable;

/**
 * Five-letter word stored in uppercase.
 * Mirrors the Word model from the original Python models.py.
 */
public class Word implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public String word;

    public Word() {
    }

    public Word(String word) {
        this.word = word;
    }

    @Override
    public String toString() {
        return "<Word " + word + ">";
    }
}
