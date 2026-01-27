package com.example.service;

import com.example.model.QueryDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads query definitions from files.
 * 
 * Supported formats:
 * 1. Simple SQL file with queries separated by semicolons
 * 2. Named queries with comments:
 *    -- @name: Query Name
 *    -- @description: Optional description
 *    SELECT * FROM table;
 */
public class QueryLoader {
    private static final Logger logger = LoggerFactory.getLogger(QueryLoader.class);
    
    private static final Pattern NAME_PATTERN = Pattern.compile("--\\s*@name:\\s*(.+)");
    private static final Pattern DESC_PATTERN = Pattern.compile("--\\s*@description:\\s*(.+)");

    /**
     * Loads queries from a file.
     */
    public List<QueryDefinition> loadFromFile(String filePath) throws IOException {
        logger.info("Loading queries from: {}", filePath);
        
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return parseQueries(content.toString());
    }

    /**
     * Loads queries from classpath resource.
     */
    public List<QueryDefinition> loadFromResource(String resourcePath) throws IOException {
        logger.info("Loading queries from resource: {}", resourcePath);
        
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return parseQueries(content.toString());
    }

    /**
     * Parses query content into QueryDefinition objects.
     */
    private List<QueryDefinition> parseQueries(String content) {
        List<QueryDefinition> queries = new ArrayList<>();
        
        // Split by semicolon, but preserve comments with metadata
        String[] segments = content.split(";");
        
        int queryIndex = 1;
        String currentName = null;
        String currentDescription = "";
        StringBuilder currentSql = new StringBuilder();
        
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            
            // Check for name annotation
            Matcher nameMatcher = NAME_PATTERN.matcher(trimmed);
            if (nameMatcher.find()) {
                currentName = nameMatcher.group(1).trim();
            }
            
            // Check for description annotation
            Matcher descMatcher = DESC_PATTERN.matcher(trimmed);
            if (descMatcher.find()) {
                currentDescription = descMatcher.group(1).trim();
            }
            
            // Extract the actual SQL (remove metadata comments)
            String sql = trimmed
                    .replaceAll("--\\s*@name:.*", "")
                    .replaceAll("--\\s*@description:.*", "")
                    .trim();
            
            if (!sql.isEmpty() && !sql.startsWith("--")) {
                // Generate name if not provided
                if (currentName == null || currentName.isEmpty()) {
                    currentName = "Query_" + queryIndex;
                }
                
                queries.add(new QueryDefinition(currentName, sql, currentDescription));
                logger.debug("Loaded query: {}", currentName);
                
                // Reset for next query
                currentName = null;
                currentDescription = "";
                queryIndex++;
            }
        }
        
        logger.info("Loaded {} queries", queries.size());
        return queries;
    }

    /**
     * Creates a list of queries from strings.
     */
    public List<QueryDefinition> createQueries(String... sqlStatements) {
        List<QueryDefinition> queries = new ArrayList<>();
        for (int i = 0; i < sqlStatements.length; i++) {
            queries.add(new QueryDefinition("Query_" + (i + 1), sqlStatements[i]));
        }
        return queries;
    }

    /**
     * Creates a single query definition.
     */
    public QueryDefinition createQuery(String name, String sql) {
        return new QueryDefinition(name, sql);
    }

    /**
     * Creates a single query definition with description.
     */
    public QueryDefinition createQuery(String name, String sql, String description) {
        return new QueryDefinition(name, sql, description);
    }
}
