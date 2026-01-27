package com.example.model;

/**
 * Defines a query to be executed against both databases.
 */
public class QueryDefinition {
    private final String name;
    private final String sql;
    private final String description;

    public QueryDefinition(String name, String sql) {
        this(name, sql, "");
    }

    public QueryDefinition(String name, String sql, String description) {
        this.name = name;
        this.sql = sql;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getSql() {
        return sql;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "QueryDefinition{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
