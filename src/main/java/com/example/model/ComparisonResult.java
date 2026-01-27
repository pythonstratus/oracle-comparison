package com.example.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the comparison results between legacy and modern database query results.
 */
public class ComparisonResult {
    private final String queryName;
    private final QueryResult legacyResult;
    private final QueryResult modernResult;
    private final List<String> differences;
    private final boolean identical;

    public ComparisonResult(String queryName, QueryResult legacyResult, QueryResult modernResult) {
        this.queryName = queryName;
        this.legacyResult = legacyResult;
        this.modernResult = modernResult;
        this.differences = new ArrayList<>();
        this.identical = computeDifferences();
    }

    private boolean computeDifferences() {
        // Check for errors
        if (!legacyResult.isSuccess()) {
            differences.add("Legacy query failed: " + legacyResult.getErrorMessage());
        }
        if (!modernResult.isSuccess()) {
            differences.add("Modern query failed: " + modernResult.getErrorMessage());
        }
        
        if (!legacyResult.isSuccess() || !modernResult.isSuccess()) {
            return false;
        }

        // Compare row counts
        if (legacyResult.getRowCount() != modernResult.getRowCount()) {
            differences.add(String.format("Row count mismatch: Legacy=%d, Modern=%d",
                    legacyResult.getRowCount(), modernResult.getRowCount()));
        }

        // Compare column names
        if (!legacyResult.getColumnNames().equals(modernResult.getColumnNames())) {
            differences.add("Column names differ between legacy and modern results");
        }

        // Compare execution times
        long timeDiff = modernResult.getExecutionTimeMs() - legacyResult.getExecutionTimeMs();
        if (Math.abs(timeDiff) > 1000) { // More than 1 second difference
            String faster = timeDiff < 0 ? "Modern" : "Legacy";
            differences.add(String.format("Execution time difference: %dms (%s is faster)",
                    Math.abs(timeDiff), faster));
        }

        // Compare data (row by row comparison for matching rows)
        int minRows = Math.min(legacyResult.getRowCount(), modernResult.getRowCount());
        int dataDiscrepancies = 0;
        
        for (int i = 0; i < minRows && dataDiscrepancies < 10; i++) {
            List<Object> legacyRow = legacyResult.getRows().get(i);
            List<Object> modernRow = modernResult.getRows().get(i);
            
            if (!rowsEqual(legacyRow, modernRow)) {
                dataDiscrepancies++;
                if (dataDiscrepancies <= 5) {
                    differences.add(String.format("Data mismatch at row %d", i + 1));
                }
            }
        }
        
        if (dataDiscrepancies > 5) {
            differences.add(String.format("... and %d more data mismatches", dataDiscrepancies - 5));
        }

        return differences.isEmpty();
    }

    private boolean rowsEqual(List<Object> row1, List<Object> row2) {
        if (row1.size() != row2.size()) {
            return false;
        }
        
        for (int i = 0; i < row1.size(); i++) {
            Object val1 = row1.get(i);
            Object val2 = row2.get(i);
            
            if (val1 == null && val2 == null) {
                continue;
            }
            if (val1 == null || val2 == null) {
                return false;
            }
            if (!val1.toString().equals(val2.toString())) {
                return false;
            }
        }
        return true;
    }

    public String getQueryName() {
        return queryName;
    }

    public QueryResult getLegacyResult() {
        return legacyResult;
    }

    public QueryResult getModernResult() {
        return modernResult;
    }

    public List<String> getDifferences() {
        return differences;
    }

    public boolean isIdentical() {
        return identical;
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Query: ").append(queryName).append("\n");
        sb.append("Status: ").append(identical ? "IDENTICAL" : "DIFFERENCES FOUND").append("\n");
        sb.append("Legacy - Rows: ").append(legacyResult.getRowCount())
          .append(", Time: ").append(legacyResult.getExecutionTimeMs()).append("ms\n");
        sb.append("Modern - Rows: ").append(modernResult.getRowCount())
          .append(", Time: ").append(modernResult.getExecutionTimeMs()).append("ms\n");
        
        if (!identical) {
            sb.append("Differences:\n");
            for (String diff : differences) {
                sb.append("  - ").append(diff).append("\n");
            }
        }
        return sb.toString();
    }
}
