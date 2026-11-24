package com.votetrace.lite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {

    private final String url;
    private final String username;
    private final String password;

    private DatabaseConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public static DatabaseConfig fromEnvironment() throws SQLException {
        String password = System.getenv("VOTETRACE_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new SQLException("Set VOTETRACE_DB_PASSWORD before starting VoteTrace.");
        }
        return new DatabaseConfig(
                environmentOrDefault("VOTETRACE_DB_URL",
                        "jdbc:mysql://localhost:3306/votetrace?serverTimezone=UTC"),
                environmentOrDefault("VOTETRACE_DB_USER", "votetrace_app"),
                password);
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
