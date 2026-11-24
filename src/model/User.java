package model;

import java.util.Objects;

public class User {
    private String userId;
    private String username;
    private String password;

    public User(String userId, String username, String password) {
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.username = Objects.requireNonNull(username, "Username is required");

        if (userId.isBlank() || username.isBlank()) {
            throw new IllegalArgumentException("User ID and username cannot be empty");
        }

        updatePassword(password);
    }

    public String getUserId() {
        return userId;
    }

    public void updatePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.password = newPassword;
    }
}