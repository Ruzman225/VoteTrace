package com.votetrace.lite;

import java.io.Console;
import java.sql.SQLException;
import java.util.Arrays;

public final class UserProvisioner {

    private UserProvisioner() {
    }

    public static void main(String[] args) throws SQLException {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Run this command in an interactive terminal.");
        }

        String username = console.readLine("New username: ").trim();
        char[] password = console.readPassword("New password (8-72 characters): ");
        char[] confirmation = console.readPassword("Confirm password: ");
        ActivityLogger activityLogger = null;
        try {
            if (!Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("The passwords do not match.");
            }
            DatabaseConfig databaseConfig = DatabaseConfig.fromEnvironment();
            activityLogger = new ActivityLogger(databaseConfig);
            activityLogger.log(null, username, "ACCOUNT_CREATE", "STARTED", null,
                    "Account creation started from the local provisioning command.");
            AuthService.User user = new AuthService(databaseConfig, activityLogger)
                    .createUser(username, password);
            console.printf("Created account '%s'.%n", user.username());
        } catch (SQLException | IllegalArgumentException error) {
            if (activityLogger != null) {
                try {
                    activityLogger.log(null, username, "ACCOUNT_CREATE", "FAILURE", null,
                            error.getClass().getSimpleName());
                } catch (SQLException auditError) {
                    error.addSuppressed(auditError);
                }
            }
            throw error;
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}
