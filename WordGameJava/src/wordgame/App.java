package wordgame;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import wordgame.handler.AdminHandler;
import wordgame.handler.AuthHandler;
import wordgame.handler.GameHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Application entry point. Sets up the HTTP server, wires up all routes, applies
 * login/admin guards, and serves static files.
 *
 * This is the Java equivalent of app.py (the Flask app factory) combined with the
 * blueprint registration and the seed step. On startup it seeds the data store
 * (20 words + the Admin1 user) if needed, then serves the game at
 * http://127.0.0.1:5000 — the same address the Flask version used.
 */
public class App {

    private static final int PORT = 5000;

    public static void main(String[] args) throws IOException {
        // Seed words + admin user (idempotent), same as running seed_words.py.
        Db.get().seed();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/", App::route);
        server.setExecutor(null); // default executor
        server.start();

        System.out.println();
        System.out.println("Guess the Word is running at http://127.0.0.1:" + PORT);
        System.out.println("Admin login -> username: Admin1   password: Admin1$");
        System.out.println("Press Ctrl+C to stop.");
    }

    /** Single dispatcher for every request. */
    private static void route(HttpExchange ex) {
        try {
            Web.Ctx ctx = new Web.Ctx(ex);
            String path = ctx.path;
            String method = ctx.method;

            // Static files
            if (path.equals("/static/style.css")) {
                serveStatic(ctx, "static/style.css", "text/css; charset=utf-8");
                return;
            }

            // Root -> login
            if (path.equals("/")) {
                ctx.redirect("/login");
                return;
            }

            // Auth
            if (path.equals("/register")) {
                AuthHandler.register(ctx);
                return;
            }
            if (path.equals("/login")) {
                AuthHandler.login(ctx);
                return;
            }
            if (path.equals("/logout")) {
                if (requireLogin(ctx)) {
                    AuthHandler.logout(ctx);
                }
                return;
            }

            // Game
            if (path.equals("/dashboard")) {
                if (requireLogin(ctx)) {
                    GameHandler.dashboard(ctx);
                }
                return;
            }
            if (path.equals("/start_game") && method.equalsIgnoreCase("POST")) {
                if (requireLogin(ctx)) {
                    GameHandler.startGame(ctx);
                }
                return;
            }
            if (path.startsWith("/play/")) {
                if (requireLogin(ctx)) {
                    Integer id = parseId(path, "/play/");
                    if (id == null) {
                        notFound(ctx);
                    } else {
                        GameHandler.play(ctx, id);
                    }
                }
                return;
            }
            if (path.startsWith("/guess/") && method.equalsIgnoreCase("POST")) {
                if (requireLogin(ctx)) {
                    Integer id = parseId(path, "/guess/");
                    if (id == null) {
                        notFound(ctx);
                    } else {
                        GameHandler.submitGuess(ctx, id);
                    }
                }
                return;
            }

            // Admin
            if (path.equals("/admin/daily_report")) {
                if (requireAdmin(ctx)) {
                    AdminHandler.dailyReport(ctx);
                }
                return;
            }
            if (path.equals("/admin/user_report")) {
                if (requireAdmin(ctx)) {
                    AdminHandler.userReport(ctx);
                }
                return;
            }

            notFound(ctx);
        } catch (Exception e) {
            // Never let an exception crash the server; return a 500.
            e.printStackTrace();
            try {
                byte[] body = "Internal Server Error".getBytes();
                ex.sendResponseHeaders(500, body.length);
                ex.getResponseBody().write(body);
                ex.close();
            } catch (IOException ignored) {
            }
        }
    }

    // ----- Guards (mirror Flask-Login @login_required and the @admin_required decorator) -----

    private static boolean requireLogin(Web.Ctx ctx) throws IOException {
        if (!ctx.isAuthenticated()) {
            ctx.flash("Please log in to access this page.", "error");
            ctx.redirect("/login");
            return false;
        }
        return true;
    }

    private static boolean requireAdmin(Web.Ctx ctx) throws IOException {
        if (!requireLogin(ctx)) {
            return false;
        }
        if (!ctx.currentUser().isAdmin) {
            ctx.flash("Admin access required.", "error");
            ctx.redirect("/dashboard");
            return false;
        }
        return true;
    }

    // ----- Helpers -----

    private static Integer parseId(String path, String prefix) {
        String rest = path.substring(prefix.length());
        // Strip any trailing path segments
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            rest = rest.substring(0, slash);
        }
        try {
            return Integer.parseInt(rest);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void serveStatic(Web.Ctx ctx, String relativePath, String contentType)
            throws IOException {
        Path file = Path.of(relativePath);
        if (!Files.exists(file)) {
            notFound(ctx);
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        ctx.ex.getResponseHeaders().set("Content-Type", contentType);
        ctx.ex.sendResponseHeaders(200, bytes.length);
        ctx.ex.getResponseBody().write(bytes);
        ctx.ex.close();
    }

    private static void notFound(Web.Ctx ctx) throws IOException {
        ctx.send(404, "Not Found", "text/plain; charset=utf-8");
    }
}
