# Guess the Word 🟩

A Wordle-style word guessing game written in **pure Java** with **no external
libraries** — just the JDK. Users register, log in, and guess 5-letter words with
color-coded feedback, while an admin can view play statistics.

## Features

- **User Registration & Login** with username and password validation
- **Wordle-style game** with color-coded feedback (green / orange / grey),
  including correct handling of duplicate letters
- **Max 5 guesses** per word, **max 3 words per day**
- **Admin reports**: daily stats and a per-user breakdown
- **20 pre-loaded 5-letter words**

## Requirements

- A **JDK 17 or newer** (`javac` and `java` on your PATH). No Maven, no Gradle,
  no database driver, and no internet connection needed.

Check your setup with:

```bash
java -version
javac -version
```

## Run it

### macOS / Linux

```bash
./run.sh
```

(If you get a permissions error, run `chmod +x run.sh` first.)

### Windows

```bat
run.bat
```

### Or compile and run manually

macOS / Linux:

```bash
mkdir out
javac -d out $(find src -name "*.java")
java -cp out wordgame.App
```

Windows:

```bat
dir /s /b src\*.java > sources.txt && javac -d out @sources.txt && del sources.txt
java -cp out wordgame.App
```

The app runs at **http://127.0.0.1:5000**

On the first run it automatically creates the 20 words and the admin user, then
prints the address. Data is saved to a `guess_the_word.db` file in the project
folder and persists across restarts.

## Default Admin Credentials

| Username | Password |
|----------|----------|
| Admin1   | Admin1$  |

## How to Play

1. Register a new account or log in
2. Click "Start New Game"
3. Enter a 5-letter word and submit
4. Letters are color-coded:
   - 🟩 **Green** — Correct letter, correct position
   - 🟧 **Orange** — Correct letter, wrong position
   - ⬜ **Grey** — Letter not in the word
5. You have **5 guesses** to find the word
6. You can play up to **3 words per day**

## Validation Rules

**Username:** at least 5 characters, and must contain both an uppercase and a
lowercase letter.

**Password:** at least 5 characters, and must contain at least one letter, one
number, and one special character from `$ % * &`.

## Project Structure

```
├── run.sh / run.bat            # Compile + run helpers
├── static/
│   └── style.css               # Styling
└── src/wordgame/
    ├── App.java                # Entry point: HTTP server, routing, access guards, seeding
    ├── Db.java                 # In-memory data store persisted to a file
    ├── Web.java                # Request/response handling, cookie sessions, flash messages
    ├── Templates.java          # Server-side HTML rendering
    ├── PasswordHasher.java     # Salted PBKDF2 password hashing
    ├── model/
    │   ├── User.java
    │   ├── Word.java
    │   ├── GameSession.java
    │   ├── Guess.java
    │   └── Cell.java           # One evaluated letter (letter + green/orange/grey status)
    └── handler/
        ├── AuthHandler.java    # Registration & login
        ├── GameHandler.java    # Game logic (start, play, guess, evaluation)
        └── AdminHandler.java   # Admin reports
```

## Tech Stack

- **Java 17+** (standard library only)
- Built-in `com.sun.net.httpserver` HTTP server
- File-based persistence via Java serialization
- Server-rendered HTML/CSS
