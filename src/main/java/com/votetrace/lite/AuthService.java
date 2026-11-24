package com.votetrace.lite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

public final class AuthService {

    private static final String FIND_USER =
            "SELECT id, username, password_hash, enabled FROM app_user WHERE username = ?";
    private static final String INSERT_USER =
            "INSERT INTO app_user (username, password_hash) VALUES (?, ?)";

    private final DatabaseConfig databaseConfig;
    private final ActivityLogger activityLogger;

    public AuthService(DatabaseConfig databaseConfig, ActivityLogger activityLogger) {
        this.databaseConfig = databaseConfig;
        this.activityLogger = activityLogger;
    }

    public User authenticate(String usernameInput, char[] password) throws SQLException {
        String username = usernameInput.trim();
        try (Connection connection = databaseConfig.openConnection()) {
            connection.setAutoCommit(false);
            try {
                User user = null;
                boolean valid = false;
                try (PreparedStatement statement = connection.prepareStatement(FIND_USER)) {
                    statement.setString(1, username);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            long id = result.getLong("id");
                            String storedUsername = result.getString("username");
                            boolean enabled = result.getBoolean("enabled");
                            valid = enabled && PasswordHasher.matches(password, result.getString("password_hash"));
                            if (valid) {
                                user = new User(id, storedUsername);
                            }
                        }
                    }
                }

                activityLogger.log(connection, valid ? user.id() : null, username,
                        "LOGIN", valid ? "SUCCESS" : "FAILURE", null,
                        valid ? "Authentication succeeded." : "Invalid credentials or disabled account.");
                connection.commit();
                return user;
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public User createUser(String usernameInput, char[] password) throws SQLException {
        String username = usernameInput.trim();
        validateNewUser(username, password);
        String passwordHash = PasswordHasher.hash(password);
        try (Connection connection = databaseConfig.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement statement = connection.prepareStatement(
                        INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, username);
                    statement.setString(2, passwordHash);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("MySQL did not return the new user ID.");
                        }
                        id = keys.getLong(1);
                    }
                }
                activityLogger.log(connection, id, username, "ACCOUNT_CREATED", "SUCCESS",
                        null, "Account created using the local provisioning command.");
                connection.commit();
                return new User(id, username);
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static void validateNewUser(String username, char[] password) {
        if (username.length() < 3 || username.length() > 100) {
            throw new IllegalArgumentException("Username must contain between 3 and 100 characters.");
        }
        if (password.length < 8 || PasswordHasher.utf8Length(password) > 72) {
            throw new IllegalArgumentException("Password must contain at least 8 characters and at most 72 UTF-8 bytes.");
        }
    }

    private static void rollback(Connection connection, Exception originalError) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            originalError.addSuppressed(rollbackError);
        }
    }

    public record User(long id, String username) {
    }
}
