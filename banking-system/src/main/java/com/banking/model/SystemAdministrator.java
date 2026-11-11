
package com.banking.model;

public class SystemAdministrator extends User {

    private final String adminId;
    private String role;

    public SystemAdministrator(
            String userId,
            String username,
            String passwordHash,
            String adminId,
            String role) {

        super(userId, username, passwordHash);

        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException(
                "Administrator ID is required."
            );
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                "Administrator role is required."
            );
        }

        this.adminId = adminId;
        this.role = role;
    }

    public String getAdminId() {
        return adminId;
    }

    @Override
    public String getRole() {
        return role;
    }

    public void updateRole(String newRole) {

        if (newRole == null || newRole.isBlank()) {
            throw new IllegalArgumentException(
                "Role cannot be empty."
            );
        }

        this.role = newRole;
    }

    private void checkAdminAccess() {

        if (!isActive()) {
            throw new IllegalStateException(
                "Administrator account is inactive."
            );
        }
    }

    public void manageUserAccess(
            User user,
            boolean active) {

        checkAdminAccess();

        if (user == null) {
            throw new IllegalArgumentException(
                "User cannot be null."
            );
        }

        user.setActive(active);
    }

    public void updateUser(
            User user,
            String newUsername) {

        checkAdminAccess();

        if (user == null) {
            throw new IllegalArgumentException(
                "User cannot be null."
            );
        }

        user.updateUsername(newUsername);
    }

    public void deactivateUser(User user) {
        manageUserAccess(user, false);
    }

    public void activateUser(User user) {
        manageUserAccess(user, true);
    }
}
