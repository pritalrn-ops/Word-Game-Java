package wordgame;

import wordgame.model.Cell;
import wordgame.model.User;

import java.util.List;

/**
 * Server-side HTML rendering. Each method produces the same markup the original
 * Jinja2 templates emitted, so the pages look and behave identically. The base()
 * method reproduces base.html (navbar, flash messages, footer); the other methods
 * fill in the {% block content %} for each page.
 */
public class Templates {

    /** Escape a value for safe HTML output (Jinja2 autoescaping equivalent). */
    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** Wrap page content in the shared base layout. Mirrors base.html. */
    public static String base(Web.Ctx ctx, String content) {
        User user = ctx.currentUser();

        StringBuilder nav = new StringBuilder();
        if (user != null) {
            nav.append("<span class=\"nav-user\">Hello, ").append(esc(user.username)).append("</span>\n");
            if (user.isAdmin) {
                nav.append("<a href=\"/admin/daily_report\">Daily Report</a>\n");
                nav.append("<a href=\"/admin/user_report\">User Report</a>\n");
            } else {
                nav.append("<a href=\"/dashboard\">Play</a>\n");
            }
            nav.append("<a href=\"/logout\">Logout</a>\n");
        } else {
            nav.append("<a href=\"/login\">Login</a>\n");
            nav.append("<a href=\"/register\">Register</a>\n");
        }

        StringBuilder flashes = new StringBuilder();
        for (String[] msg : ctx.popFlashes()) {
            String category = msg[0];
            String message = msg[1];
            flashes.append("<div class=\"flash-message flash-").append(esc(category)).append("\">")
                    .append(esc(message)).append("</div>\n");
        }

        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "    <title>Guess the Word</title>\n"
                + "    <link rel=\"stylesheet\" href=\"/static/style.css\">\n"
                + "</head>\n"
                + "<body>\n"
                + "    <nav class=\"navbar\">\n"
                + "        <div class=\"nav-brand\">\n"
                + "            <a href=\"/\">\uD83D\uDFE9 Guess the Word</a>\n"
                + "        </div>\n"
                + "        <div class=\"nav-links\">\n"
                + nav
                + "        </div>\n"
                + "    </nav>\n\n"
                + "    <main class=\"container\">\n"
                + flashes
                + content
                + "    </main>\n\n"
                + "    <footer class=\"footer\">\n"
                + "        <p>&copy; 2024 Guess the Word Game</p>\n"
                + "    </footer>\n"
                + "</body>\n"
                + "</html>\n";
    }

    /** login.html content block. */
    public static String login() {
        return "<div class=\"auth-container\">\n"
                + "    <div class=\"auth-card\">\n"
                + "        <h2>Login</h2>\n"
                + "        <form method=\"POST\" action=\"/login\">\n"
                + "            <div class=\"form-group\">\n"
                + "                <label for=\"username\">Username</label>\n"
                + "                <input type=\"text\" id=\"username\" name=\"username\" required\n"
                + "                       placeholder=\"Enter your username\" autocomplete=\"username\">\n"
                + "            </div>\n"
                + "            <div class=\"form-group\">\n"
                + "                <label for=\"password\">Password</label>\n"
                + "                <input type=\"password\" id=\"password\" name=\"password\" required\n"
                + "                       placeholder=\"Enter your password\" autocomplete=\"current-password\">\n"
                + "            </div>\n"
                + "            <button type=\"submit\" class=\"btn btn-primary\">Login</button>\n"
                + "        </form>\n"
                + "        <p class=\"auth-link\">\n"
                + "            Don't have an account? <a href=\"/register\">Register here</a>\n"
                + "        </p>\n"
                + "    </div>\n"
                + "</div>\n";
    }

    /** register.html content block. */
    public static String register(String username) {
        return "<div class=\"auth-container\">\n"
                + "    <div class=\"auth-card\">\n"
                + "        <h2>Register</h2>\n"
                + "        <form method=\"POST\" action=\"/register\">\n"
                + "            <div class=\"form-group\">\n"
                + "                <label for=\"username\">Username</label>\n"
                + "                <input type=\"text\" id=\"username\" name=\"username\" required\n"
                + "                       value=\"" + esc(username) + "\"\n"
                + "                       placeholder=\"At least 5 chars, upper & lowercase\">\n"
                + "            </div>\n"
                + "            <div class=\"form-group\">\n"
                + "                <label for=\"password\">Password</label>\n"
                + "                <input type=\"password\" id=\"password\" name=\"password\" required\n"
                + "                       placeholder=\"At least 5 chars, letter + number + special ($%*&)\">\n"
                + "            </div>\n"
                + "            <div class=\"form-hint\">\n"
                + "                <strong>Username:</strong> Min 5 characters, must include uppercase and lowercase letters.<br>\n"
                + "                <strong>Password:</strong> Min 5 characters, must include a letter, a number, and a special character ($, %, *, &).\n"
                + "            </div>\n"
                + "            <button type=\"submit\" class=\"btn btn-primary\">Register</button>\n"
                + "        </form>\n"
                + "        <p class=\"auth-link\">\n"
                + "            Already have an account? <a href=\"/login\">Login here</a>\n"
                + "        </p>\n"
                + "    </div>\n"
                + "</div>\n";
    }

    /** game.html in dashboard mode (no active game). */
    public static String gameDashboard(int gamesToday, boolean canPlay) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"game-container\">\n");
        sb.append("    <h2>Guess the Word</h2>\n\n");
        sb.append("        <div class=\"game-info\">\n");
        sb.append("            <p>Guess a 5-letter word in 5 tries or less!</p>\n");
        sb.append("            <p>Games played today: <strong>").append(gamesToday).append("</strong> / 3</p>\n");
        sb.append("        </div>\n\n");
        if (canPlay) {
            sb.append("            <form method=\"POST\" action=\"/start_game\">\n");
            sb.append("                <button type=\"submit\" class=\"btn btn-primary btn-large\">Start New Game</button>\n");
            sb.append("            </form>\n");
        } else {
            sb.append("            <div class=\"game-limit-message\">\n");
            sb.append("                <p>You have reached the maximum of <strong>3 words per day</strong>.</p>\n");
            sb.append("                <p>Come back tomorrow for more!</p>\n");
            sb.append("            </div>\n");
        }
        sb.append("</div>\n");
        return sb.toString();
    }

    /** game.html in active-game mode (grid, legend, and either the modal or the input form). */
    public static String gamePlay(int sessionId, List<List<Cell>> guesses,
                                  boolean gameOver, boolean won, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"game-container\">\n");
        sb.append("    <h2>Guess the Word</h2>\n\n");

        // Wordle grid
        sb.append("        <div class=\"wordle-grid\">\n");
        for (List<Cell> guess : guesses) {
            sb.append("                <div class=\"guess-row\">\n");
            for (Cell cell : guess) {
                sb.append("                        <div class=\"cell cell-").append(esc(cell.status)).append("\">\n");
                sb.append("                            ").append(esc(cell.letter)).append("\n");
                sb.append("                        </div>\n");
            }
            sb.append("                </div>\n");
        }
        int remaining = 5 - guesses.size();
        for (int i = 0; i < remaining; i++) {
            sb.append("                <div class=\"guess-row\">\n");
            for (int j = 0; j < 5; j++) {
                sb.append("                        <div class=\"cell cell-empty\"></div>\n");
            }
            sb.append("                </div>\n");
        }
        sb.append("        </div>\n\n");

        // Color legend
        sb.append("        <div class=\"color-legend\">\n");
        sb.append("            <span class=\"legend-item\"><span class=\"legend-box legend-green\"></span> Correct position</span>\n");
        sb.append("            <span class=\"legend-item\"><span class=\"legend-box legend-orange\"></span> Wrong position</span>\n");
        sb.append("            <span class=\"legend-item\"><span class=\"legend-box legend-grey\"></span> Not in word</span>\n");
        sb.append("        </div>\n\n");

        if (gameOver) {
            sb.append("            <div class=\"modal-overlay\" id=\"gameOverModal\">\n");
            sb.append("                <div class=\"modal-content\">\n");
            if (won) {
                sb.append("                        <h3>\uD83C\uDF89 Congratulations!</h3>\n");
                sb.append("                        <p>You guessed the word correctly!</p>\n");
            } else {
                sb.append("                        <h3>Better Luck Next Time!</h3>\n");
                sb.append("                        <p>").append(esc(message)).append("</p>\n");
            }
            sb.append("                    <a href=\"/dashboard\" class=\"btn btn-primary\">OK</a>\n");
            sb.append("                </div>\n");
            sb.append("            </div>\n");
        } else {
            sb.append("            <form method=\"POST\" action=\"/guess/").append(sessionId).append("\" class=\"guess-form\">\n");
            sb.append("                <div class=\"input-group\">\n");
            sb.append("                    <input type=\"text\" name=\"guess\" maxlength=\"5\" minlength=\"5\"\n");
            sb.append("                           pattern=\"[A-Za-z]{5}\" required\n");
            sb.append("                           placeholder=\"Enter 5-letter word\"\n");
            sb.append("                           class=\"guess-input\"\n");
            sb.append("                           style=\"text-transform: uppercase;\"\n");
            sb.append("                           autofocus>\n");
            sb.append("                    <button type=\"submit\" class=\"btn btn-primary\">Submit</button>\n");
            sb.append("                </div>\n");
            sb.append("                <p class=\"guess-counter\">Guess ").append(guesses.size() + 1).append(" of 5</p>\n");
            sb.append("            </form>\n");
        }

        sb.append("</div>\n");
        return sb.toString();
    }

    /** admin/daily_report.html content block. */
    public static String dailyReport(String selectedDate, int numUsers, int correctGuesses) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"admin-container\">\n");
        sb.append("    <h2>Daily Report</h2>\n\n");
        sb.append("    <form method=\"POST\" action=\"/admin/daily_report\" class=\"report-form\">\n");
        sb.append("        <div class=\"form-group form-inline\">\n");
        sb.append("            <label for=\"report_date\">Select Date:</label>\n");
        sb.append("            <input type=\"date\" id=\"report_date\" name=\"report_date\"\n");
        sb.append("                   value=\"").append(esc(selectedDate)).append("\" required>\n");
        sb.append("            <button type=\"submit\" class=\"btn btn-primary\">Generate Report</button>\n");
        sb.append("        </div>\n");
        sb.append("    </form>\n\n");
        sb.append("        <div class=\"report-card\">\n");
        sb.append("            <h3>Report for ").append(esc(selectedDate)).append("</h3>\n");
        sb.append("            <table class=\"report-table\">\n");
        sb.append("                <thead>\n");
        sb.append("                    <tr>\n");
        sb.append("                        <th>Metric</th>\n");
        sb.append("                        <th>Value</th>\n");
        sb.append("                    </tr>\n");
        sb.append("                </thead>\n");
        sb.append("                <tbody>\n");
        sb.append("                    <tr>\n");
        sb.append("                        <td>Number of Users</td>\n");
        sb.append("                        <td>").append(numUsers).append("</td>\n");
        sb.append("                    </tr>\n");
        sb.append("                    <tr>\n");
        sb.append("                        <td>Number of Correct Guesses</td>\n");
        sb.append("                        <td>").append(correctGuesses).append("</td>\n");
        sb.append("                    </tr>\n");
        sb.append("                </tbody>\n");
        sb.append("            </table>\n");
        sb.append("        </div>\n");
        sb.append("</div>\n");
        return sb.toString();
    }

    /** One row of the user report table. */
    public static class UserReportEntry {
        public final String date;
        public final int wordsTried;
        public final int correctGuesses;

        public UserReportEntry(String date, int wordsTried, int correctGuesses) {
            this.date = date;
            this.wordsTried = wordsTried;
            this.correctGuesses = correctGuesses;
        }
    }

    /** admin/user_report.html content block. reportUsername null => no report yet. */
    public static String userReport(List<User> users, Integer selectedUserId,
                                    String reportUsername, List<UserReportEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"admin-container\">\n");
        sb.append("    <h2>User Report</h2>\n\n");
        sb.append("    <form method=\"POST\" action=\"/admin/user_report\" class=\"report-form\">\n");
        sb.append("        <div class=\"form-group form-inline\">\n");
        sb.append("            <label for=\"user_id\">Select User:</label>\n");
        sb.append("            <select id=\"user_id\" name=\"user_id\" required>\n");
        sb.append("                <option value=\"\">-- Choose a user --</option>\n");
        for (User u : users) {
            boolean selected = selectedUserId != null && selectedUserId == u.id;
            sb.append("                    <option value=\"").append(u.id).append("\"");
            if (selected) {
                sb.append("selected");
            }
            sb.append(">\n");
            sb.append("                        ").append(esc(u.username)).append("\n");
            sb.append("                    </option>\n");
        }
        sb.append("            </select>\n");
        sb.append("            <button type=\"submit\" class=\"btn btn-primary\">Generate Report</button>\n");
        sb.append("        </div>\n");
        sb.append("    </form>\n\n");

        if (reportUsername != null) {
            sb.append("        <div class=\"report-card\">\n");
            sb.append("            <h3>Report for ").append(esc(reportUsername)).append("</h3>\n");
            if (entries != null && !entries.isEmpty()) {
                sb.append("                <table class=\"report-table\">\n");
                sb.append("                    <thead>\n");
                sb.append("                        <tr>\n");
                sb.append("                            <th>Date</th>\n");
                sb.append("                            <th>Words Tried</th>\n");
                sb.append("                            <th>Correct Guesses</th>\n");
                sb.append("                        </tr>\n");
                sb.append("                    </thead>\n");
                sb.append("                    <tbody>\n");
                for (UserReportEntry e : entries) {
                    sb.append("                            <tr>\n");
                    sb.append("                                <td>").append(esc(e.date)).append("</td>\n");
                    sb.append("                                <td>").append(e.wordsTried).append("</td>\n");
                    sb.append("                                <td>").append(e.correctGuesses).append("</td>\n");
                    sb.append("                            </tr>\n");
                }
                sb.append("                    </tbody>\n");
                sb.append("                </table>\n");
            } else {
                sb.append("                <p>No game data found for this user.</p>\n");
            }
            sb.append("        </div>\n");
        }
        sb.append("</div>\n");
        return sb.toString();
    }
}
