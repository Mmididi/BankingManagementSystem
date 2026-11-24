package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SystemAdministrator extends User {
    private final String adminId;
    private final String role;
    private final Map<String, User> users;
    private final Set<String> deactivatedUsers;

    public SystemAdministrator(String userId, String username, String password,
                               String adminId, String role) {
        super(userId, username, password);

        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("Administrator ID is required");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Administrator role is required");
        }

        this.adminId = adminId;
        this.role = role;
        this.users = new HashMap<>();
        this.deactivatedUsers = new HashSet<>();
    }

    public String getAdminId() {
        return adminId;
    }

    public String getRole() {
        return role;
    }

    public void createUser(User user) {
        Objects.requireNonNull(user, "User is required");
        String userId = user.getUserId();

        if (users.containsKey(userId)) {
            throw new IllegalArgumentException("User ID already exists");
        }
        users.put(userId, user);
    }

    public User createUser(String userId, String username, String password) {
        User user = new User(userId, username, password);
        createUser(user);
        return user;
    }

    public void updateUser(String userId, String newPassword) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        user.updatePassword(newPassword);
    }

    public void deactivateUser(String userId) {
        if (!users.containsKey(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        deactivatedUsers.add(userId);
    }

    public void manageUserAccess(String userId, boolean allowAccess) {
        if (!users.containsKey(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        if (allowAccess) {
            deactivatedUsers.remove(userId);
        } else {
            deactivatedUsers.add(userId);
        }
    }

    public boolean isUserActive(String userId) {
        return users.containsKey(userId) && !deactivatedUsers.contains(userId);
    }
}