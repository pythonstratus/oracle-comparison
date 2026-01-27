package com.example.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the results of a query execution.
 */
public class QueryResult {
    private final String queryName;
    private final String databaseName;
    private final List<String> columnNames;
    private final List<List<Object>> rows;
    private final long executionTimeMs;
    private final String errorMessage;
    private final boolean success;

    private QueryResult(Builder builder) {
        this.queryName = builder.queryName;
        this.databaseName = builder.databaseName;
        this.columnNames = builder.columnNames;
        this.rows = builder.rows;
        this.executionTimeMs = builder.executionTimeMs;
        this.errorMessage = builder.errorMessage;
        this.success = builder.success;
    }

    public String getQueryName() {
        return queryName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getRowCount() {
        return rows != null ? rows.size() : 0;
    }

    public static class Builder {
        private String queryName;
        private String databaseName;
        private List<String> columnNames = new ArrayList<>();
        private List<List<Object>> rows = new ArrayList<>();
        private long executionTimeMs;
        private String errorMessage;
        private boolean success = true;

        public Builder queryName(String queryName) {
            this.queryName = queryName;
            return this;
        }

        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder columnNames(List<String> columnNames) {
            this.columnNames = columnNames;
            return this;
        }

        public Builder rows(List<List<Object>> rows) {
            this.rows = rows;
            return this;
        }

        public Builder executionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            this.success = false;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public QueryResult build() {
            return new QueryResult(this);
        }
    }
}
