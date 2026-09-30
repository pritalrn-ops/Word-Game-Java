package wordgame.handler;

import wordgame.Db;
import wordgame.PasswordHasher;
import wordgame.Templates;
import wordgame.Web;
import wordgame.model.User;

import java.io.IOException;

/**
 * Registration and login handling. Mirrors routes/auth.py, including the exact
 * username/password validation rules and flash messages.
 */
public class AuthHandler {

    /**
     * Validate username:
     * - At least 5 characters
     * - Must contain both uppercase and lowercase letters
     * Returns an error message, or null if valid.
     */
    public static String validateUsername(String username) {
        if (username.length() < 5) {
            return "Username must be at least 5 characters long.";
        }
        boolean hasUpper = username.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = username.chars().anyMatch(Character::isLowerCase);
        if (!(hasUpper && hasLower)) {
            return "Username must contain both uppercase and lowercase letters.";
        }
        return null;
    }

    /**
     * Validate password:
     * - At least 5 characters
     * - Must contain alphabetic characters
     * - Must contain numeric characters
     * - Must contain at least one special character from: $, %, *, &
     * Returns an error message, or null if valid.
     */
    public static String validatePassword(String password) {
        if (password.length() < 5) {
            return "Password must be at least 5 characters long.";
        }
        boolean hasAlpha = password.chars().anyMatch(Character::isLetter);
        boolean hasNumeric = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars()
                .anyMatch(c -> c == '$' || c == '%' || c == '*' || c == '&');
        if (!hasAlpha) {
            return "Password must contain at least one letter.";
        }
        if (!hasNumeric) {
            return "Password must contain at least one number.";
        }
        if (!hasSpecial) {
            return "Password must contain at least one special character ($, %, *, &).";
        }
        return null;
    }

    /** GET/POST /register */
    public static void register(Web.Ctx ctx) throws IOException {
        if (ctx.isAuthenticated()) {
            ctx.redirect("/dashboard");
            return;
        }

        if ("POST".equalsIgnoreCase(ctx.method)) {
            String username = ctx.form.getOrDefault("username", "").trim();
            String password = ctx.form.getOrDefault("password", "");

            String usernameError = validateUsername(username);
            if (usernameError != null) {
                ctx.flash(usernameError, "error");
                ctx.html(Templates.base(ctx, Templates.register(username)));
                return;
            }

            String passwordError = validatePassword(password);
            if (passwordError != null) {
                ctx.flash(passwordError, "error");
                ctx.html(Templates.base(ctx, Templates.register(username)));
                return;
            }

            if (ctx.db.userByUsername(username) != null) {
                ctx.flash("Username already exists. Please choose another.", "error");
                ctx.html(Templates.base(ctx, Templates.register(username)));
                return;
            }

            User user = new User(username, false);
            user.passwordHash = PasswordHasher.hash(password);
            ctx.db.addUser(user);

            ctx.flash("Registration successful! Please log in.", "success");
            ctx.redirect("/login");
            return;
        }

        ctx.html(Templates.base(ctx, Templates.register("")));
    }

    /** GET/POST /login */
    public static void login(Web.Ctx ctx) throws IOException {
        if (ctx.isAuthenticated()) {
            if (ctx.currentUser().isAdmin) {
                ctx.redirect("/admin/daily_report");
            } else {
                ctx.redirect("/dashboard");
            }
            return;
        }

        if ("POST".equalsIgnoreCase(ctx.method)) {
            String username = ctx.form.getOrDefault("username", "").trim();
            String password = ctx.form.getOrDefault("password", "");

            User user = ctx.db.userByUsername(username);
            if (user != null && PasswordHasher.verify(password, user.passwordHash)) {
                ctx.login(user);
                if (user.isAdmin) {
                    ctx.redirect("/admin/daily_report");
                } else {
                    ctx.redirect("/dashboard");
                }
                return;
            }

            ctx.flash("Invalid username or password.", "error");
        }

        ctx.html(Templates.base(ctx, Templates.login()));
    }

    /** GET /logout (login required) */
    public static void logout(Web.Ctx ctx) throws IOException {
        ctx.logout();
        ctx.flash("You have been logged out.", "success");
        ctx.redirect("/login");
    }
}
