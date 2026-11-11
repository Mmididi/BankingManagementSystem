
package com.banking.model;

public abstract class User {

    private final String userId;
    private String username;
    private String passwordHash;
    private boolean active;

    public User(String userId, String username,
                String passwordHash) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                "User ID is required."
            );
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                "Username is required."
            );
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException(
                "Password hash is required."
            );
        }

        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = true;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public boolean isActive() {
        return active;
    }

    public void updateUsername(String newUsername) {

        if (newUsername == null ||
                newUsername.isBlank()) {
            throw new IllegalArgumentException(
                "Username cannot be empty."
            );
        }

        this.username = newUsername;
    }

    public void updatePasswordHash(String newPasswordHash) {

        if (newPasswordHash == null ||
                newPasswordHash.isBlank()) {
            throw new IllegalArgumentException(
                "Password hash cannot be empty."
            );
        }

        this.passwordHash = newPasswordHash;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public abstract String getRole();
}
