package com.example;

import com.example.config.ConfigLoader;
import com.example.config.DatabaseConfig;
import com.example.model.QueryDefinition;
import com.example.model.QueryResult;
import com.example.service.QueryExecutor;
import com.example.service.QueryLoader;
import com.example.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * Oracle Legacy vs Modern Exadata Comparison Tool
 * 
 * Runs MINUS queries to compare ALS_LEGACY_REPLICA (Golden Gate) tables
 * against local schema tables and generates a CSV report.
 * 
 * Usage:
 *   java -jar oracle-comparison-tool.jar [options]
 * 
 * Options:
 *   -q, --queries <file>    Path to SQL file containing queries
 *   -c, --config <file>     Path to config.properties file
 *   -e, --email             Send report via email
 *   -h, --help              Show this help message
 */
public class OracleComparisonTool {
    private static final Logger logger = LoggerFactory.getLogger(OracleComparisonTool.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConfigLoader config;
    private final QueryExecutor executor;
    private final QueryLoader queryLoader;
    private final EmailService emailService;

    private boolean sendEmail = false;
    private String queryFile = null;

    public OracleComparisonTool(ConfigLoader config) {
        this.config = config;
        this.executor = new QueryExecutor(config.getDatabaseConfig());
        this.queryLoader = new QueryLoader();
        this.emailService = new EmailService(config);
    }

    /**
     * Tests database connection.
     */
    public boolean testConnection() {
        logger.info("Testing database connection...");
        return executor.testConnection();
    }

    /**
     * Runs all MINUS queries and collects results.
     */
    public List<QueryResult> runQueries(List<QueryDefinition> queries) {
        List<QueryResult> results = new ArrayList<>();
        
        logger.info("Starting execution of {} queries", queries.size());
        
        for (QueryDefinition query : queries) {
            logger.info("Executing: {}", query.getName());
            QueryResult result = executor.executeQuery(query);
            results.add(result);
            
            if (result.isSuccess()) {
                if (result.getRowCount() > 0) {
                    logger.warn("DIFFERENCE FOUND - {}: {} rows", query.getName(), result.getRowCount());
                } else {
                    logger.info("OK - {}: No differences", query.getName());
                }
            } else {
                logger.error("ERROR - {}: {}", query.getName(), result.getErrorMessage());
            }
        }
        
        return results;
    }

    /**
     * Generates CSV report from query results.
     */
    public List<String> generateReports(List<QueryResult> results) throws IOException {
        List<String> reportFiles = new ArrayList<>();
        
        // Create output directory
        File outputDir = new File(config.getOutputDirectory());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        // Generate summary report
        String summaryFile = generateSummaryReport(results, timestamp);
        reportFiles.add(summaryFile);

        // Generate detailed report with all differences
        String detailFile = generateDetailedReport(results, timestamp);
        reportFiles.add(detailFile);

        return reportFiles;
    }

    /**
     * Generates summary CSV report.
     */
    private String generateSummaryReport(List<QueryResult> results, String timestamp) throws IOException {
        String filename = String.format("%s/%s_summary_%s.csv", 
                config.getOutputDirectory(), 
                config.getOutputFilenamePrefix(), 
                timestamp);

        try (FileWriter writer = new FileWriter(filename);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            // Header info
            printer.printRecord("Legacy vs Modern Exadata Comparison Report");
            printer.printRecord("Generated:", LocalDateTime.now().format(DISPLAY_FORMAT));
            printer.printRecord("");

            // Summary statistics
            long totalQueries = results.size();
            long withDifferences = results.stream().filter(r -> r.isSuccess() && r.getRowCount() > 0).count();
            long noDifferences = results.stream().filter(r -> r.isSuccess() && r.getRowCount() == 0).count();
            long errors = results.stream().filter(r -> !r.isSuccess()).count();

            printer.printRecord("SUMMARY");
            printer.printRecord("Total Queries:", totalQueries);
            printer.printRecord("Tables with Differences:", withDifferences);
            printer.printRecord("Tables Matching:", noDifferences);
            printer.printRecord("Errors:", errors);
            printer.printRecord("");

            // Column headers
            printer.printRecord(
                    "Query Name",
                    "Status",
                    "Difference Count",
                    "Execution Time (ms)",
                    "Error Message"
            );

            // Data rows
            for (QueryResult result : results) {
                String status;
                if (!result.isSuccess()) {
                    status = "ERROR";
                } else if (result.getRowCount() > 0) {
                    status = "DIFFERENCE";
                } else {
                    status = "MATCH";
                }

                printer.printRecord(
                        result.getQueryName(),
                        status,
                        result.isSuccess() ? result.getRowCount() : "N/A",
                        result.getExecutionTimeMs(),
                        result.getErrorMessage() != null ? result.getErrorMessage() : ""
                );
            }
        }

        logger.info("Generated summary report: {}", filename);
        return filename;
    }

    /**
     * Generates detailed CSV report with all differing rows.
     */
    private String generateDetailedReport(List<QueryResult> results, String timestamp) throws IOException {
        String filename = String.format("%s/%s_details_%s.csv", 
                config.getOutputDirectory(), 
                config.getOutputFilenamePrefix(), 
                timestamp);

        try (FileWriter writer = new FileWriter(filename);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            printer.printRecord("Legacy vs Modern Exadata - Detailed Differences");
            printer.printRecord("Generated:", LocalDateTime.now().format(DISPLAY_FORMAT));
            printer.printRecord("");

            for (QueryResult result : results) {
                if (!result.isSuccess()) {
                    printer.printRecord("========================================");
                    printer.printRecord("Query:", result.getQueryName());
                    printer.printRecord("Status:", "ERROR");
                    printer.printRecord("Error:", result.getErrorMessage());
                    printer.printRecord("");
                    continue;
                }

                if (result.getRowCount() == 0) {
                    // Skip tables with no differences in detail report
                    continue;
                }

                // Section header for queries with differences
                printer.printRecord("========================================");
                printer.printRecord("Query:", result.getQueryName());
                printer.printRecord("Description:", result.getDatabaseName());
                printer.printRecord("Difference Count:", result.getRowCount());
                printer.printRecord("Execution Time:", result.getExecutionTimeMs() + " ms");
                printer.printRecord("");

                // Column headers
                if (!result.getColumnNames().isEmpty()) {
                    printer.printRecord(result.getColumnNames().toArray());
                }

                // Data rows (limit to 1000 rows per query)
                int maxRows = Math.min(result.getRows().size(), 1000);
                for (int i = 0; i < maxRows; i++) {
                    printer.printRecord(result.getRows().get(i).toArray());
                }

                if (result.getRows().size() > 1000) {
                    printer.printRecord("... " + (result.getRows().size() - 1000) + " more rows truncated ...");
                }

                printer.printRecord("");
            }
        }

        logger.info("Generated detailed report: {}", filename);
        return filename;
    }

    /**
     * Prints execution summary to console.
     */
    private void printSummary(List<QueryResult> results, List<String> reportFiles) {
        System.out.println("\n========================================");
        System.out.println("COMPARISON SUMMARY");
        System.out.println("========================================\n");

        long withDifferences = results.stream().filter(r -> r.isSuccess() && r.getRowCount() > 0).count();
        long noDifferences = results.stream().filter(r -> r.isSuccess() && r.getRowCount() == 0).count();
        long errors = results.stream().filter(r -> !r.isSuccess()).count();

        System.out.println("Total Queries:           " + results.size());
        System.out.println("Tables with Differences: " + withDifferences);
        System.out.println("Tables Matching:         " + noDifferences);
        System.out.println("Errors:                  " + errors);
        System.out.println();

        // Show tables with differences
        if (withDifferences > 0) {
            System.out.println("Tables with Differences:");
            for (QueryResult result : results) {
                if (result.isSuccess() && result.getRowCount() > 0) {
                    System.out.println("  - " + result.getQueryName() + ": " + result.getRowCount() + " rows");
                }
            }
            System.out.println();
        }

        // Show errors
        if (errors > 0) {
            System.out.println("Queries with Errors:");
            for (QueryResult result : results) {
                if (!result.isSuccess()) {
                    System.out.println("  - " + result.getQueryName() + ": " + result.getErrorMessage());
                }
            }
            System.out.println();
        }

        System.out.println("Generated Reports:");
        for (String file : reportFiles) {
            System.out.println("  - " + file);
        }
        System.out.println();
    }

    /**
     * Main execution method.
     */
    public void run() {
        try {
            // Test connection
            if (!testConnection()) {
                logger.error("Aborting due to connection failure");
                return;
            }

            // Load queries
            List<QueryDefinition> queries;
            if (queryFile != null) {
                queries = queryLoader.loadFromFile(queryFile);
            } else {
                try {
                    queries = queryLoader.loadFromResource("queries.sql");
                } catch (IOException e) {
                    logger.error("No queries.sql found. Please provide a query file with -q option.");
                    return;
                }
            }

            if (queries.isEmpty()) {
                logger.error("No queries to execute");
                return;
            }

            // Run queries
            List<QueryResult> results = runQueries(queries);

            // Generate reports
            List<String> reportFiles = generateReports(results);

            // Send email if enabled
            if (sendEmail && config.isEmailEnabled()) {
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

            // Print summary
            printSummary(results, reportFiles);

        } catch (Exception e) {
            logger.error("Error during execution: {}", e.getMessage(), e);
        }
    }

    /**
     * Parses command line arguments.
     */
    private void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-q":
                case "--queries":
                    if (i + 1 < args.length) {
                        queryFile = args[++i];
                    }
                    break;
                case "-e":
                case "--email":
                    sendEmail = true;
                    break;
                case "-h":
                case "--help":
                    printHelp();
                    System.exit(0);
                    break;
            }
        }
    }

    /**
     * Prints help message.
     */
    private static void printHelp() {
        System.out.println("Oracle Legacy vs Modern Exadata Comparison Tool");
        System.out.println("================================================\n");
        System.out.println("This tool runs MINUS queries to compare ALS_LEGACY_REPLICA (Golden Gate)");
        System.out.println("tables against local schema tables and generates CSV reports.\n");
        System.out.println("Usage: java -jar oracle-comparison-tool.jar [options]\n");
        System.out.println("Options:");
        System.out.println("  -q, --queries <file>    Path to SQL file containing MINUS queries");
        System.out.println("  -c, --config <file>     Path to config.properties file");
        System.out.println("  -e, --email             Send report via email");
        System.out.println("  -h, --help              Show this help message\n");
        System.out.println("Configuration:");
        System.out.println("  Edit config.properties to set database connection and email settings.\n");
        System.out.println("Query File Format:");
        System.out.println("  Queries should be separated by semicolons.");
        System.out.println("  Optional metadata comments:");
        System.out.println("    -- @name: Query Name");
        System.out.println("    -- @description: Description");
        System.out.println("    SELECT * FROM table1 MINUS SELECT * FROM table2;");
    }

    /**
     * Setters for programmatic use.
     */
    public void setQueryFile(String queryFile) {
        this.queryFile = queryFile;
    }

    public void setSendEmail(boolean sendEmail) {
        this.sendEmail = sendEmail;
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        try {
            // Load configuration
            String configFile = "config.properties";
            for (int i = 0; i < args.length - 1; i++) {
                if (args[i].equals("-c") || args[i].equals("--config")) {
                    configFile = args[i + 1];
                    break;
                }
            }

            ConfigLoader config = new ConfigLoader(configFile);

            // Create and run tool
            OracleComparisonTool tool = new OracleComparisonTool(config);
            tool.parseArgs(args);
            tool.run();

        } catch (IOException e) {
            logger.error("Failed to load configuration: {}", e.getMessage());
            System.err.println("Error: Could not load configuration file. " + e.getMessage());
            System.exit(1);
        }
    }
}
