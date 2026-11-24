package com.votetrace.lite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class ActivityLogger {

    private static final String INSERT_ACTIVITY = """
            INSERT INTO activity_log (user_id, username, action, outcome, file_name, details)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final DatabaseConfig databaseConfig;

    public ActivityLogger(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void log(Long userId, String username, String action, String outcome,
                    String fileName, String details) throws SQLException {
        try (Connection connection = databaseConfig.openConnection()) {
            log(connection, userId, username, action, outcome, fileName, details);
        }
    }

    void log(Connection connection, Long userId, String username, String action, String outcome,
             String fileName, String details) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_ACTIVITY)) {
            if (userId == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, userId);
            }
            statement.setString(2, username);
            statement.setString(3, action);
            statement.setString(4, outcome);
            statement.setString(5, fileName);
            statement.setString(6, details);
            statement.executeUpdate();
        }
    }
}
