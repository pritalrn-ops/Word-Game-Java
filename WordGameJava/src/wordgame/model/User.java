package wordgame.model;

import java.io.Serializable;

/**
 * User model for both Admin and Player users.
 * Mirrors the User model from the original Python models.py.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public String username;
    public String passwordHash;
    public boolean isAdmin;

    public User() {
    }

    public User(String username, boolean isAdmin) {
        this.username = username;
        this.isAdmin = isAdmin;
    }

    @Override
    public String toString() {
        return "<User " + username + ">";
    }
}
