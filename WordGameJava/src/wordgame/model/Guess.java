package wordgame.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A single guess within a game session.
 * Mirrors the Guess model from the original Python models.py.
 */
public class Guess implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public int sessionId;
    public String guessWord;
    public int guessNumber;        // 1-5
    public List<Cell> result;      // [{letter, status}, ...]
    public LocalDateTime createdAt;

    public Guess() {
    }

    public Guess(int sessionId, String guessWord, int guessNumber, List<Cell> result) {
        this.sessionId = sessionId;
        this.guessWord = guessWord;
        this.guessNumber = guessNumber;
        this.result = result;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "<Guess " + guessWord + " #" + guessNumber + ">";
    }
}
