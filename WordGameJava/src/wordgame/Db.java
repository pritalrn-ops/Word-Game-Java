package wordgame;

import wordgame.model.GameSession;
import wordgame.model.Guess;
import wordgame.model.User;
import wordgame.model.Word;

import java.io.Serializable;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * In-memory data store persisted to a single file on disk.
 *
 * This replaces Flask-SQLAlchemy + SQLite from the original project. It holds the
 * same four "tables" (users, words, sessions, guesses), auto-increments ids, and
 * saves/loads everything to guess_the_word.db so data survives restarts — the same
 * behaviour the SQLite database gave. No external database driver is required.
 */
public class Db implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String DATA_FILE = "guess_the_word.db";
    private static final Random RANDOM = new Random();

    public List<User> users = new ArrayList<>();
    public List<Word> words = new ArrayList<>();
    public List<GameSession> sessions = new ArrayList<>();
    public List<Guess> guesses = new ArrayList<>();

    public int nextUserId = 1;
    public int nextWordId = 1;
    public int nextSessionId = 1;
    public int nextGuessId = 1;

    private static Db instance;

    /** Singleton accessor: loads from disk on first use. */
    public static synchronized Db get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    // ---------- Persistence ----------

    private static Db load() {
        File f = new File(DATA_FILE);
        if (f.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
                return (Db) in.readObject();
            } catch (Exception e) {
                System.err.println("Could not read data file, starting fresh: " + e.getMessage());
            }
        }
        return new Db();
    }

    public synchronized void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(this);
        } catch (IOException e) {
            System.err.println("Could not save data file: " + e.getMessage());
        }
    }

    // ---------- Insert helpers ----------

    public synchronized User addUser(User u) {
        u.id = nextUserId++;
        users.add(u);
        save();
        return u;
    }

    public synchronized Word addWord(Word w) {
        w.id = nextWordId++;
        words.add(w);
        save();
        return w;
    }

    public synchronized GameSession addSession(GameSession s) {
        s.id = nextSessionId++;
        sessions.add(s);
        save();
        return s;
    }

    public synchronized Guess addGuess(Guess g) {
        g.id = nextGuessId++;
        guesses.add(g);
        save();
        return g;
    }

    // ---------- Query helpers ----------

    public User userById(int id) {
        for (User u : users) {
            if (u.id == id) {
                return u;
            }
        }
        return null;
    }

    public User userByUsername(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return u;
            }
        }
        return null;
    }

    public List<User> players() {
        List<User> result = new ArrayList<>();
        for (User u : users) {
            if (!u.isAdmin) {
                result.add(u);
            }
        }
        return result;
    }

    public Word wordById(int id) {
        for (Word w : words) {
            if (w.id == id) {
                return w;
            }
        }
        return null;
    }

    public Word randomWord() {
        if (words.isEmpty()) {
            return null;
        }
        return words.get(RANDOM.nextInt(words.size()));
    }

    public GameSession sessionById(int id) {
        for (GameSession s : sessions) {
            if (s.id == id) {
                return s;
            }
        }
        return null;
    }

    /** First active (incomplete) session for a user, or null. */
    public GameSession activeSession(int userId) {
        for (GameSession s : sessions) {
            if (s.userId == userId && !s.isComplete) {
                return s;
            }
        }
        return null;
    }

    public int countSessionsToday(int userId, LocalDate today) {
        int count = 0;
        for (GameSession s : sessions) {
            if (s.userId == userId && s.date.equals(today)) {
                count++;
            }
        }
        return count;
    }

    public List<GameSession> sessionsByDate(LocalDate date) {
        List<GameSession> result = new ArrayList<>();
        for (GameSession s : sessions) {
            if (s.date.equals(date)) {
                result.add(s);
            }
        }
        return result;
    }

    public List<GameSession> sessionsByUser(int userId) {
        List<GameSession> result = new ArrayList<>();
        for (GameSession s : sessions) {
            if (s.userId == userId) {
                result.add(s);
            }
        }
        return result;
    }

    /** Guesses for a session, ordered by guess number. */
    public List<Guess> guessesForSession(int sessionId) {
        List<Guess> result = new ArrayList<>();
        for (Guess g : guesses) {
            if (g.sessionId == sessionId) {
                result.add(g);
            }
        }
        result.sort((a, b) -> Integer.compare(a.guessNumber, b.guessNumber));
        return result;
    }

    public int countGuesses(int sessionId) {
        int count = 0;
        for (Guess g : guesses) {
            if (g.sessionId == sessionId) {
                count++;
            }
        }
        return count;
    }

    // ---------- Seeding ----------

    private static final String[] SEED_WORDS = {
            "APPLE", "BRAVE", "CHARM", "CRANE", "DANCE",
            "EAGLE", "FLAME", "GRAPE", "HOUSE", "JOKER",
            "KNEEL", "LEMON", "MANGO", "NOBLE", "OLIVE",
            "PEARL", "QUEEN", "RAVEN", "TOWER", "UNITY",
    };

    /**
     * Populate the store with 20 five-letter words and one admin user
     * (Admin1 / Admin1$) if they are not already present. Idempotent, exactly
     * like the original seed_words.py script.
     */
    public synchronized void seed() {
        int added = 0;
        for (String w : SEED_WORDS) {
            boolean exists = false;
            for (Word existing : words) {
                if (existing.word.equals(w)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                Word word = new Word(w);
                word.id = nextWordId++;
                words.add(word);
                added++;
            }
        }
        System.out.println("Seeded " + added + " words (total: " + words.size() + ").");

        String adminUsername = "Admin1";
        if (userByUsername(adminUsername) == null) {
            User admin = new User(adminUsername, true);
            admin.passwordHash = PasswordHasher.hash("Admin1$");
            admin.id = nextUserId++;
            users.add(admin);
            System.out.println("Admin user created: username='" + adminUsername + "', password='Admin1$'");
        } else {
            System.out.println("Admin user '" + adminUsername + "' already exists.");
        }
        save();
    }
}
