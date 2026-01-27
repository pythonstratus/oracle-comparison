package com.example.service;

import com.example.config.DatabaseConfig;
import com.example.model.QueryDefinition;
import com.example.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes SQL queries against Oracle databases.
 */
public class QueryExecutor {
    private static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);

    private final DatabaseConfig config;

    public QueryExecutor(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Tests the database connection.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            logger.info("Successfully connected to: {}", config.getName());
            return true;
        } catch (SQLException e) {
            logger.error("Failed to connect to {}: {}", config.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Executes a query and returns the result.
     */
    public QueryResult executeQuery(QueryDefinition query) {
        logger.info("Executing query '{}' on {}", query.getName(), config.getName());
        
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query.getSql())) {

            // Get column metadata
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }

            // Get all rows
            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Query '{}' completed in {}ms with {} rows", 
                    query.getName(), executionTime, rows.size());

            return new QueryResult.Builder()
                    .queryName(query.getName())
                    .databaseName(query.getDescription()) // Store description here
                    .columnNames(columnNames)
                    .rows(rows)
                    .executionTimeMs(executionTime)
                    .success(true)
                    .build();

        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Query '{}' failed on {}: {}", query.getName(), config.getName(), e.getMessage());
            
            return new QueryResult.Builder()
                    .queryName(query.getName())
                    .databaseName(config.getName())
                    .executionTimeMs(executionTime)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Creates a database connection.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getUsername(),
                config.getPassword()
        );
    }

    /**
     * Gets the database configuration.
     */
    public DatabaseConfig getConfig() {
        return config;
    }
}
