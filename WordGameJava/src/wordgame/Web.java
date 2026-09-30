package wordgame;

import com.sun.net.httpserver.HttpExchange;
import wordgame.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Per-request context plus a tiny server-side session store.
 *
 * Together these replace Flask's request object, session, flash() messages and
 * Flask-Login's current_user. A random session id is kept in a cookie; the server
 * maps that id to a logged-in user and to any pending flash messages.
 */
public class Web {

    // Server-side session state, keyed by the cookie's session id.
    private static final Map<String, Integer> SESSION_USER = new HashMap<>();
    private static final Map<String, List<String[]>> SESSION_FLASH = new HashMap<>();

    /** Request/response context for a single HTTP exchange. */
    public static class Ctx {
        public final HttpExchange ex;
        public final String method;
        public final String path;
        public final Map<String, String> form = new HashMap<>();
        public final Map<String, String> query = new HashMap<>();
        public final Db db;

        private final String sessionId;
        private final boolean newSession;

        public Ctx(HttpExchange ex) throws IOException {
            this.ex = ex;
            this.db = Db.get();
            this.method = ex.getRequestMethod();
            this.path = ex.getRequestURI().getPath();

            // Query string
            String rawQuery = ex.getRequestURI().getRawQuery();
            if (rawQuery != null) {
                parseUrlEncoded(rawQuery, query);
            }

            // Form body (application/x-www-form-urlencoded)
            if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(ex.getRequestBody());
                parseUrlEncoded(body, form);
            }

            // Session cookie
            String existing = readSessionCookie();
            if (existing == null) {
                this.sessionId = UUID.randomUUID().toString();
                this.newSession = true;
            } else {
                this.sessionId = existing;
                this.newSession = false;
            }
        }

        // ----- Session / auth (mirrors Flask-Login current_user) -----

        public User currentUser() {
            Integer uid = SESSION_USER.get(sessionId);
            return uid == null ? null : db.userById(uid);
        }

        public boolean isAuthenticated() {
            return currentUser() != null;
        }

        public void login(User user) {
            SESSION_USER.put(sessionId, user.id);
        }

        public void logout() {
            SESSION_USER.remove(sessionId);
        }

        // ----- Flash messages -----

        public void flash(String message, String category) {
            SESSION_FLASH.computeIfAbsent(sessionId, k -> new ArrayList<>())
                    .add(new String[]{category, message});
        }

        public List<String[]> popFlashes() {
            List<String[]> msgs = SESSION_FLASH.remove(sessionId);
            return msgs == null ? Collections.emptyList() : msgs;
        }

        // ----- Responses -----

        public void html(String body) throws IOException {
            send(200, body, "text/html; charset=utf-8");
        }

        public void redirect(String location) throws IOException {
            setSessionCookie();
            ex.getResponseHeaders().set("Location", location);
            ex.sendResponseHeaders(302, -1);
            ex.close();
        }

        public void send(int status, String body, String contentType) throws IOException {
            setSessionCookie();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", contentType);
            ex.sendResponseHeaders(status, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        }

        // ----- Cookie helpers -----

        private void setSessionCookie() {
            if (newSession && ex.getResponseHeaders().get("Set-Cookie") == null) {
                ex.getResponseHeaders().add("Set-Cookie",
                        "session_id=" + sessionId + "; Path=/; HttpOnly; SameSite=Lax");
            }
        }

        private String readSessionCookie() {
            List<String> cookies = ex.getRequestHeaders().get("Cookie");
            if (cookies == null) {
                return null;
            }
            for (String header : cookies) {
                for (String part : header.split(";")) {
                    String[] kv = part.trim().split("=", 2);
                    if (kv.length == 2 && kv[0].equals("session_id")) {
                        return kv[1];
                    }
                }
            }
            return null;
        }
    }

    // ----- Static parsing utilities -----

    private static void parseUrlEncoded(String data, Map<String, String> into) {
        if (data == null || data.isEmpty()) {
            return;
        }
        for (String pair : data.split("&")) {
            int idx = pair.indexOf('=');
            String key;
            String value;
            if (idx >= 0) {
                key = decode(pair.substring(0, idx));
                value = decode(pair.substring(idx + 1));
            } else {
                key = decode(pair);
                value = "";
            }
            into.put(key, value);
        }
    }

    private static String decode(String s) {
        return URLDecoder.decode(s.replace("+", " "), StandardCharsets.UTF_8);
    }

    private static String readBody(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
}
