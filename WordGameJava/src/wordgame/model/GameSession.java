package wordgame.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * A single game session: one word for a user to guess.
 * Mirrors the GameSession model from the original Python models.py.
 */
public class GameSession implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public int userId;
    public int wordId;
    public LocalDate date;
    public Boolean isWon;          // null = in progress
    public boolean isComplete;

    public GameSession() {
    }

    public GameSession(int userId, int wordId, LocalDate date, boolean isComplete) {
        this.userId = userId;
        this.wordId = wordId;
        this.date = date;
        this.isWon = null;
        this.isComplete = isComplete;
    }

    @Override
    public String toString() {
        return "<GameSession " + id + " user=" + userId + ">";
    }
}
