package wordgame.handler;

import wordgame.Templates;
import wordgame.Web;
import wordgame.model.Cell;
import wordgame.model.GameSession;
import wordgame.model.Guess;
import wordgame.model.Word;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Game logic. Mirrors routes/game.py: the dashboard, starting a game, playing,
 * and submitting guesses — including the daily limit, the 5-guess cap, and the
 * two-pass green/orange/grey evaluation that handles duplicate letters correctly.
 */
public class GameHandler {

    /**
     * Evaluate a guess against the target word.
     * Returns a list of 5 cells with statuses:
     *   green  - correct letter in correct position
     *   orange - correct letter in wrong position
     *   grey   - letter not in the word
     * Duplicate letters are handled with a two-pass algorithm.
     */
    public static List<Cell> evaluateGuess(String guess, String target) {
        Cell[] result = new Cell[5];
        Character[] targetChars = new Character[5];
        Character[] guessChars = new Character[5];
        for (int i = 0; i < 5; i++) {
            targetChars[i] = target.charAt(i);
            guessChars[i] = guess.charAt(i);
        }

        // First pass: mark exact matches (green)
        for (int i = 0; i < 5; i++) {
            if (guessChars[i] != null && guessChars[i].equals(targetChars[i])) {
                result[i] = new Cell(String.valueOf(guessChars[i]), "green");
                targetChars[i] = null;   // consumed from target
                guessChars[i] = null;    // processed
            }
        }

        // Second pass: mark wrong-position (orange) and not-in-word (grey)
        for (int i = 0; i < 5; i++) {
            if (guessChars[i] == null) {
                continue;  // already matched as green
            }
            int foundIndex = -1;
            for (int j = 0; j < 5; j++) {
                if (targetChars[j] != null && targetChars[j].equals(guessChars[i])) {
                    foundIndex = j;
                    break;
                }
            }
            if (foundIndex >= 0) {
                result[i] = new Cell(String.valueOf(guessChars[i]), "orange");
                targetChars[foundIndex] = null;  // consume first available occurrence
            } else {
                result[i] = new Cell(String.valueOf(guessChars[i]), "grey");
            }
        }

        List<Cell> list = new ArrayList<>(5);
        for (Cell c : result) {
            list.add(c);
        }
        return list;
    }

    /** GET /dashboard (login required) */
    public static void dashboard(Web.Ctx ctx) throws IOException {
        int userId = ctx.currentUser().id;

        GameSession active = ctx.db.activeSession(userId);
        if (active != null) {
            ctx.redirect("/play/" + active.id);
            return;
        }

        LocalDate today = LocalDate.now();
        int todayCount = ctx.db.countSessionsToday(userId, today);
        boolean canPlay = todayCount < 3;

        ctx.html(Templates.base(ctx, Templates.gameDashboard(todayCount, canPlay)));
    }

    /** POST /start_game (login required) */
    public static void startGame(Web.Ctx ctx) throws IOException {
        int userId = ctx.currentUser().id;
        LocalDate today = LocalDate.now();
        int todayCount = ctx.db.countSessionsToday(userId, today);

        if (todayCount >= 3) {
            ctx.flash("You have reached the maximum of 3 words per day.", "error");
            ctx.redirect("/dashboard");
            return;
        }

        Word word = ctx.db.randomWord();
        if (word == null) {
            ctx.flash("No words available in the database. Contact an administrator.", "error");
            ctx.redirect("/dashboard");
            return;
        }

        GameSession session = new GameSession(userId, word.id, today, false);
        ctx.db.addSession(session);

        ctx.redirect("/play/" + session.id);
    }

    /** GET /play/{id} (login required) */
    public static void play(Web.Ctx ctx, int sessionId) throws IOException {
        GameSession gameSession = ctx.db.sessionById(sessionId);
        if (gameSession == null) {
            notFound(ctx);
            return;
        }

        if (gameSession.userId != ctx.currentUser().id) {
            ctx.flash("Access denied.", "error");
            ctx.redirect("/dashboard");
            return;
        }

        List<Guess> guesses = ctx.db.guessesForSession(sessionId);
        List<List<Cell>> guessData = new ArrayList<>();
        for (Guess g : guesses) {
            guessData.add(g.result);
        }

        boolean gameOver = gameSession.isComplete;
        boolean won = gameSession.isWon != null && gameSession.isWon;
        String message = "";
        if (gameOver) {
            if (won) {
                message = "Congratulations! You guessed the word!";
            } else {
                Word w = ctx.db.wordById(gameSession.wordId);
                message = "Better luck next time! The word was " + w.word + ".";
            }
        }

        ctx.html(Templates.base(ctx,
                Templates.gamePlay(sessionId, guessData, gameOver, won, message)));
    }

    /** POST /guess/{id} (login required) */
    public static void submitGuess(Web.Ctx ctx, int sessionId) throws IOException {
        GameSession gameSession = ctx.db.sessionById(sessionId);
        if (gameSession == null) {
            notFound(ctx);
            return;
        }

        if (gameSession.userId != ctx.currentUser().id) {
            ctx.flash("Access denied.", "error");
            ctx.redirect("/dashboard");
            return;
        }

        if (gameSession.isComplete) {
            ctx.flash("This game is already over.", "error");
            ctx.redirect("/play/" + sessionId);
            return;
        }

        String guessWord = ctx.form.getOrDefault("guess", "").toUpperCase().trim();

        // Validate: exactly 5 alphabetic characters
        if (guessWord.length() != 5 || !guessWord.chars().allMatch(Character::isLetter)) {
            ctx.flash("Please enter exactly 5 letters.", "error");
            ctx.redirect("/play/" + sessionId);
            return;
        }

        int existingGuesses = ctx.db.countGuesses(sessionId);
        if (existingGuesses >= 5) {
            ctx.flash("Maximum guesses reached.", "error");
            ctx.redirect("/play/" + sessionId);
            return;
        }

        Word target = ctx.db.wordById(gameSession.wordId);
        List<Cell> result = evaluateGuess(guessWord, target.word);

        Guess guess = new Guess(sessionId, guessWord, existingGuesses + 1, result);
        ctx.db.addGuess(guess);

        if (guessWord.equals(target.word)) {
            gameSession.isWon = true;
            gameSession.isComplete = true;
        } else if (existingGuesses + 1 >= 5) {
            gameSession.isWon = false;
            gameSession.isComplete = true;
        }
        ctx.db.save();

        ctx.redirect("/play/" + sessionId);
    }

    private static void notFound(Web.Ctx ctx) throws IOException {
        ctx.send(404, "Not Found", "text/plain; charset=utf-8");
    }
}
