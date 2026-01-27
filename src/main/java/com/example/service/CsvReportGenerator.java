package com.example.service;

import com.example.model.ComparisonResult;
import com.example.model.QueryResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates CSV reports from comparison results.
 */
public class CsvReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CsvReportGenerator.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final String outputDirectory;
    private final String filenamePrefix;

    public CsvReportGenerator(String outputDirectory, String filenamePrefix) {
        this.outputDirectory = outputDirectory;
        this.filenamePrefix = filenamePrefix;
    }

    /**
     * Generates all reports for the given comparison results.
     * Returns the list of generated file paths.
     */
    public List<String> generateReports(List<ComparisonResult> results) throws IOException {
        List<String> generatedFiles = new ArrayList<>();
        
        // Create output directory if it doesn't exist
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        // Generate summary report
        String summaryFile = generateSummaryReport(results, timestamp);
        generatedFiles.add(summaryFile);

        // Generate detailed report for each query
        for (ComparisonResult result : results) {
            String detailFile = generateDetailReport(result, timestamp);
            generatedFiles.add(detailFile);
        }

        // Generate combined data report
        String dataFile = generateCombinedDataReport(results, timestamp);
        generatedFiles.add(dataFile);

        return generatedFiles;
    }

    /**
     * Generates a summary CSV with overview of all comparisons.
     */
    private String generateSummaryReport(List<ComparisonResult> results, String timestamp) throws IOException {
        String filename = String.format("%s/%s_summary_%s.csv", outputDirectory, filenamePrefix, timestamp);
        
        try (FileWriter writer = new FileWriter(filename);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            // Header
            printer.printRecord(
                    "Query Name",
                    "Status",
                    "Legacy Rows",
                    "Modern Rows",
                    "Row Difference",
                    "Legacy Time (ms)",
                    "Modern Time (ms)",
                    "Time Difference (ms)",
                    "Differences Found"
            );

            // Data rows
            for (ComparisonResult result : results) {
                QueryResult legacy = result.getLegacyResult();
                QueryResult modern = result.getModernResult();
                
                int rowDiff = modern.getRowCount() - legacy.getRowCount();
                long timeDiff = modern.getExecutionTimeMs() - legacy.getExecutionTimeMs();
                
                printer.printRecord(
                        result.getQueryName(),
                        result.isIdentical() ? "IDENTICAL" : "DIFFERENT",
                        legacy.isSuccess() ? legacy.getRowCount() : "ERROR",
                        modern.isSuccess() ? modern.getRowCount() : "ERROR",
                        rowDiff,
                        legacy.getExecutionTimeMs(),
                        modern.getExecutionTimeMs(),
                        timeDiff,
                        String.join("; ", result.getDifferences())
                );
            }
        }

        logger.info("Generated summary report: {}", filename);
        return filename;
    }

    /**
     * Generates a detailed CSV for a single query comparison.
     */
    private String generateDetailReport(ComparisonResult result, String timestamp) throws IOException {
        String safeName = result.getQueryName().replaceAll("[^a-zA-Z0-9]", "_");
        String filename = String.format("%s/%s_%s_%s.csv", outputDirectory, filenamePrefix, safeName, timestamp);

        try (FileWriter writer = new FileWriter(filename);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            // Metadata section
            printer.printRecord("Query Name", result.getQueryName());
            printer.printRecord("Comparison Status", result.isIdentical() ? "IDENTICAL" : "DIFFERENT");
            printer.printRecord("Generated At", LocalDateTime.now().toString());
            printer.printRecord("");

            // Legacy Results Section
            printer.printRecord("=== LEGACY EXADATA RESULTS ===");
            printQueryResultSection(printer, result.getLegacyResult());
            printer.printRecord("");

            // Modern Results Section
            printer.printRecord("=== MODERN EXADATA RESULTS ===");
            printQueryResultSection(printer, result.getModernResult());
            printer.printRecord("");

            // Differences Section
            if (!result.isIdentical()) {
                printer.printRecord("=== DIFFERENCES ===");
                for (String diff : result.getDifferences()) {
                    printer.printRecord(diff);
                }
            }
        }

        logger.info("Generated detail report: {}", filename);
        return filename;
    }

    /**
     * Prints a query result section to the CSV.
     */
    private void printQueryResultSection(CSVPrinter printer, QueryResult result) throws IOException {
        printer.printRecord("Database", result.getDatabaseName());
        printer.printRecord("Execution Time (ms)", result.getExecutionTimeMs());
        printer.printRecord("Row Count", result.getRowCount());
        
        if (!result.isSuccess()) {
            printer.printRecord("Error", result.getErrorMessage());
            return;
        }

        // Column headers
        printer.printRecord(result.getColumnNames().toArray());

        // Data rows (limit to first 1000 rows for readability)
        int maxRows = Math.min(result.getRows().size(), 1000);
        for (int i = 0; i < maxRows; i++) {
            printer.printRecord(result.getRows().get(i).toArray());
        }
        
        if (result.getRows().size() > 1000) {
            printer.printRecord("... " + (result.getRows().size() - 1000) + " more rows truncated ...");
        }
    }

    /**
     * Generates a combined report showing side-by-side data comparison.
     */
    private String generateCombinedDataReport(List<ComparisonResult> results, String timestamp) throws IOException {
        String filename = String.format("%s/%s_combined_%s.csv", outputDirectory, filenamePrefix, timestamp);

        try (FileWriter writer = new FileWriter(filename);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            for (ComparisonResult result : results) {
                // Section header
                printer.printRecord("========================================");
                printer.printRecord("Query: " + result.getQueryName());
                printer.printRecord("Status: " + (result.isIdentical() ? "IDENTICAL" : "DIFFERENT"));
                printer.printRecord("========================================");
                printer.printRecord("");

                QueryResult legacy = result.getLegacyResult();
                QueryResult modern = result.getModernResult();

                if (!legacy.isSuccess() || !modern.isSuccess()) {
                    if (!legacy.isSuccess()) {
                        printer.printRecord("Legacy Error: " + legacy.getErrorMessage());
                    }
                    if (!modern.isSuccess()) {
                        printer.printRecord("Modern Error: " + modern.getErrorMessage());
                    }
                    printer.printRecord("");
                    continue;
                }

                // Create side-by-side header
                List<String> combinedHeader = new ArrayList<>();
                combinedHeader.add("Row#");
                combinedHeader.add("Source");
                combinedHeader.addAll(legacy.getColumnNames());
                printer.printRecord(combinedHeader.toArray());

                // Interleave rows from both sources
                int maxRows = Math.max(legacy.getRowCount(), modern.getRowCount());
                maxRows = Math.min(maxRows, 500); // Limit for readability

                for (int i = 0; i < maxRows; i++) {
                    // Legacy row
                    if (i < legacy.getRowCount()) {
                        List<Object> legacyRow = new ArrayList<>();
                        legacyRow.add(i + 1);
                        legacyRow.add("LEGACY");
                        legacyRow.addAll(legacy.getRows().get(i));
                        printer.printRecord(legacyRow.toArray());
                    }

                    // Modern row
                    if (i < modern.getRowCount()) {
                        List<Object> modernRow = new ArrayList<>();
                        modernRow.add(i + 1);
                        modernRow.add("MODERN");
                        modernRow.addAll(modern.getRows().get(i));
                        printer.printRecord(modernRow.toArray());
                    }
                }

                printer.printRecord("");
            }
        }

        logger.info("Generated combined report: {}", filename);
        return filename;
    }
}
