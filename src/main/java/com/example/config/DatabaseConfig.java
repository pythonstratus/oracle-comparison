package com.example.config;

/**
 * Holds database connection configuration.
 */
public class DatabaseConfig {
    private final String name;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseConfig(String name, String jdbcUrl, String username, String password) {
        this.name = name;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "name='" + name + '\'' +
                ", jdbcUrl='" + jdbcUrl + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
