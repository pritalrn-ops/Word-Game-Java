package wordgame.handler;

import wordgame.Templates;
import wordgame.Web;
import wordgame.model.GameSession;
import wordgame.model.User;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Admin reporting. Mirrors routes/admin.py: a daily report (users who played and
 * correct guesses on a date) and a per-user report (per-date words tried and
 * correct guesses).
 */
public class AdminHandler {

    /** GET/POST /admin/daily_report (admin required) */
    public static void dailyReport(Web.Ctx ctx) throws IOException {
        String selectedDate = LocalDate.now().toString();

        if ("POST".equalsIgnoreCase(ctx.method)) {
            selectedDate = ctx.form.getOrDefault("report_date", LocalDate.now().toString());
        }

        LocalDate reportDate;
        try {
            reportDate = LocalDate.parse(selectedDate);
        } catch (DateTimeParseException e) {
            reportDate = LocalDate.now();
            selectedDate = reportDate.toString();
        }

        List<GameSession> sessions = ctx.db.sessionsByDate(reportDate);

        Set<Integer> uniqueUsers = new TreeSet<>();
        int correctGuesses = 0;
        for (GameSession s : sessions) {
            uniqueUsers.add(s.userId);
            if (s.isWon != null && s.isWon) {
                correctGuesses++;
            }
        }

        ctx.html(Templates.base(ctx,
                Templates.dailyReport(selectedDate, uniqueUsers.size(), correctGuesses)));
    }

    /** GET/POST /admin/user_report (admin required) */
    public static void userReport(Web.Ctx ctx) throws IOException {
        List<User> users = ctx.db.players();
        Integer selectedUserId = null;
        String reportUsername = null;
        List<Templates.UserReportEntry> entries = null;

        if ("POST".equalsIgnoreCase(ctx.method)) {
            String raw = ctx.form.getOrDefault("user_id", "");
            if (!raw.isEmpty()) {
                selectedUserId = Integer.parseInt(raw);
                User user = ctx.db.userById(selectedUserId);

                if (user != null) {
                    reportUsername = user.username;

                    List<GameSession> sessions = ctx.db.sessionsByUser(selectedUserId);
                    // Sort by date descending
                    sessions.sort((a, b) -> b.date.compareTo(a.date));

                    // Group by date (preserve first-seen order, which is date-desc)
                    Map<String, int[]> byDate = new LinkedHashMap<>();
                    for (GameSession session : sessions) {
                        String d = session.date.toString();
                        int[] counts = byDate.computeIfAbsent(d, k -> new int[2]);
                        counts[0] += 1; // words tried
                        if (session.isWon != null && session.isWon) {
                            counts[1] += 1; // correct guesses
                        }
                    }

                    entries = new ArrayList<>();
                    for (Map.Entry<String, int[]> e : byDate.entrySet()) {
                        entries.add(new Templates.UserReportEntry(
                                e.getKey(), e.getValue()[0], e.getValue()[1]));
                    }
                }
            }
        }

        ctx.html(Templates.base(ctx,
                Templates.userReport(users, selectedUserId, reportUsername, entries)));
    }
}
