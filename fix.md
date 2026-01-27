# Fix for "DIFFERENT" Status Showing When No Differences Exist

## Problem
The email report shows "DIFFERENT" status for queries that return 0 rows (meaning no actual differences were found). This happens because the code is comparing the MINUS query result against an empty dummy result.

## Root Cause
In `OracleComparisonTool.java`, when creating `ComparisonResult` objects for the email, the code passes:
- **legacyResult**: The actual MINUS query result (which has row count from the query)
- **modernResult**: An empty dummy `QueryResult` with 0 rows

The `ComparisonResult.computeDifferences()` method then compares row counts and finds a mismatch, marking it as "DIFFERENT".

## Solution
For MINUS queries, the logic should be:
- **0 rows returned** = Tables are IDENTICAL (no differences found)
- **>0 rows returned** = Tables are DIFFERENT (rows exist in one but not the other)

---

## Fix Location

### File: `src/main/java/com/example/OracleComparisonTool.java`

### Lines to change: ~326-344

### Find this code:
```java
            // Send email if enabled (either via -e flag or email.enabled=true in config)
            if (config.isEmailEnabled()) {
                logger.info("Sending email report...");
                // Convert QueryResults to ComparisonResults for email service
                List<com.example.model.ComparisonResult> comparisonResults = new ArrayList<>();
                for (QueryResult result : results) {
                    // Create a dummy comparison result for email
                    comparisonResults.add(new com.example.model.ComparisonResult(
                            result.getQueryName(),
                            result,
                            new QueryResult.Builder()
                                    .queryName(result.getQueryName())
                                    .databaseName("Expected Empty")
                                    .success(true)
                                    .build()
                    ));
                }
                emailService.sendReport(comparisonResults, reportFiles);
            }
```

### Replace with:
```java
            // Send email if enabled (either via -e flag or email.enabled=true in config)
            if (config.isEmailEnabled()) {
                logger.info("Sending email report...");
                // Convert QueryResults to ComparisonResults for email service
                // For MINUS queries: 0 rows = IDENTICAL, >0 rows = DIFFERENT
                List<com.example.model.ComparisonResult> comparisonResults = new ArrayList<>();
                for (QueryResult result : results) {
                    // For MINUS queries, create matching results so 0 rows = identical
                    // The "legacy" result represents what the MINUS query found
                    // We create a "modern" result that matches when there are no differences
                    QueryResult modernResult;
                    if (result.isSuccess() && result.getRowCount() == 0) {
                        // No differences found - create matching empty result
                        modernResult = new QueryResult.Builder()
                                .queryName(result.getQueryName())
                                .databaseName("Expected")
                                .columnNames(result.getColumnNames())
                                .rows(new java.util.ArrayList<>())
                                .executionTimeMs(0)
                                .success(true)
                                .build();
                    } else {
                        // Differences found or error - create mismatched result
                        modernResult = new QueryResult.Builder()
                                .queryName(result.getQueryName())
                                .databaseName("Expected Empty")
                                .columnNames(result.getColumnNames())
                                .rows(new java.util.ArrayList<>())
                                .executionTimeMs(0)
                                .success(true)
                                .build();
                    }
                    comparisonResults.add(new com.example.model.ComparisonResult(
                            result.getQueryName(),
                            result,
                            modernResult
                    ));
                }
                emailService.sendReport(comparisonResults, reportFiles);
            }
```

---

## Expected Result After Fix

| Query Name | Rows Found | Status Should Be |
|------------|------------|------------------|
| ACTDELETE_Legacy_Only | 0 | MATCH |
| ACTUNDO_Legacy_Only | 0 | MATCH |
| ENT_Legacy_Only | 27 | DIFFERENT |
| ENTACT_Legacy_Only | 30 | DIFFERENT |
| ENTEMP_Legacy_Only | 0 | MATCH |
| ENTMOD_Legacy_Only | 0 | MATCH |
| EOM_Legacy_Only | 0 | MATCH |
| TIMENON_Legacy_Only | 0 | MATCH |
| TIMETIN_Legacy_Only | 1 | DIFFERENT |
| TRANTRAIL_Legacy_Only | 0 | MATCH |

---

## After Making the Change

1. Save the file
2. Rebuild: `mvn clean package`
3. Run the tool again

The email should now correctly show "MATCH" (green) for queries with 0 rows and "DIFFERENT" (red) only for queries that actually found differences.
