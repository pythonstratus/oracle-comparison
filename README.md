# Oracle Legacy vs Modern Exadata Comparison Tool

A Java Maven tool to run MINUS queries comparing `ALS_LEGACY_REPLICA` (Golden Gate replicated) tables against local schema tables, generate CSV reports showing differences, and optionally email the results.

## Use Case

This tool is designed for comparing data between:
- **ALS_LEGACY_REPLICA schema**: Tables replicated via Oracle Golden Gate from the legacy Exadata
- **Local schema**: Tables in the modern Exadata

The tool runs MINUS queries in both directions for each table to identify:
- Rows that exist in the local table but not in the legacy replica
- Rows that exist in the legacy replica but not in the local table

## Features

- Execute MINUS queries against a single Oracle database
- Compare multiple tables in one run
- Generate CSV reports showing all differences
- Send HTML email reports with CSV attachments
- Support for custom query files with metadata annotations

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Oracle JDBC connectivity to your Exadata database
- Read access to both local schema and `ALS_LEGACY_REPLICA` schema

## Quick Start

### 1. Build the Project

```bash
cd oracle-comparison-tool
mvn clean package
```

This creates an executable JAR at `target/oracle-comparison-tool-1.0-SNAPSHOT.jar`

### 2. Configure Database Connection

Edit `src/main/resources/config.properties`:

```properties
# Database Connection
jdbc.url=jdbc:oracle:thin:@//your-exadata-host:1521/your_service
jdbc.user=your_username
jdbc.password=your_password

# Output Configuration
output.directory=./reports
output.filename.prefix=legacy_comparison_report
```

### 3. Run the Tool

```bash
# Using default queries (queries.sql in resources)
java -jar target/oracle-comparison-tool-1.0-SNAPSHOT.jar

# With custom query file
java -jar target/oracle-comparison-tool-1.0-SNAPSHOT.jar -q /path/to/my_queries.sql

# With custom config file
java -jar target/oracle-comparison-tool-1.0-SNAPSHOT.jar -c /path/to/config.properties

# Send email report
java -jar target/oracle-comparison-tool-1.0-SNAPSHOT.jar -e
```

## Command Line Options

| Option | Description |
|--------|-------------|
| `-q, --queries <file>` | Path to SQL file containing MINUS queries |
| `-c, --config <file>` | Path to config.properties file |
| `-e, --email` | Send report via email |
| `-h, --help` | Show help message |

## Included Tables

The default `queries.sql` includes MINUS queries for these tables:

| Table | Description |
|-------|-------------|
| ACTDELETE | Action delete records |
| ACTUNDO | Action undo records |
| ENT | Entity records |
| ENTACT | Entity action records |
| ENTEMP | Entity employee records |
| ENTEMP2 | Entity employee backup (may not exist in GG) |
| ENTMOD | Entity modification records |
| EOM | End of month records |
| LOGLOAD | Log load records |
| TIMENON | Time non records |
| TIMETIN | Time tin records |
| TRANTRAIL | Transaction trail records |

## Query File Format

Queries are separated by semicolons. Use optional annotations for better reports:

```sql
-- @name: ACTDELETE_Local_Only
-- @description: Rows in local ACTDELETE but not in ALS_LEGACY_REPLICA.ACTDELETE
SELECT * FROM ACTDELETE
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.ACTDELETE;

-- @name: ACTDELETE_Legacy_Only  
-- @description: Rows in ALS_LEGACY_REPLICA.ACTDELETE but not in local ACTDELETE
SELECT * FROM ALS_LEGACY_REPLICA.ACTDELETE
MINUS
SELECT * FROM ACTDELETE;
```

## Output Reports

The tool generates two CSV reports in the `reports/` directory:

### 1. Summary Report (`*_summary_*.csv`)
Overview of all query results:
- Query name
- Status (MATCH / DIFFERENCE / ERROR)
- Number of differing rows
- Execution time
- Error messages (if any)

### 2. Detailed Report (`*_details_*.csv`)
Full data for all queries that found differences:
- Column headers from the query
- All rows returned by the MINUS query
- Limited to 1000 rows per query for readability

## Email Configuration

To enable email reports, configure these settings in `config.properties`:

```properties
email.enabled=true
email.smtp.host=smtp.example.com
email.smtp.port=587
email.smtp.auth=true
email.smtp.starttls=true
email.from=sender@example.com
email.to=recipient@example.com
email.cc=another@example.com
email.subject=Legacy vs Modern Exadata Comparison Report
email.username=your_email_username
email.password=your_email_password
```

## Sample Output

```
========================================
COMPARISON SUMMARY
========================================

Total Queries:           24
Tables with Differences: 2
Tables Matching:         21
Errors:                  1

Tables with Differences:
  - ENTACT_Local_Only: 15 rows
  - LOGLOAD_Legacy_Only: 3 rows

Queries with Errors:
  - ENTEMP2_Local_Only: ORA-00942: table or view does not exist

Generated Reports:
  - ./reports/legacy_comparison_report_summary_2024-01-15_10-30-00.csv
  - ./reports/legacy_comparison_report_details_2024-01-15_10-30-00.csv
```

## Adding New Tables

To add a new table to compare, add two MINUS queries to `queries.sql`:

```sql
-- @name: NEWTABLE_Local_Only
-- @description: Rows in local NEWTABLE but not in legacy replica
SELECT * FROM NEWTABLE
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.NEWTABLE;

-- @name: NEWTABLE_Legacy_Only
-- @description: Rows in legacy replica but not in local NEWTABLE
SELECT * FROM ALS_LEGACY_REPLICA.NEWTABLE
MINUS
SELECT * FROM NEWTABLE;
```

## Project Structure

```
oracle-comparison-tool/
├── pom.xml
├── src/main/java/com/example/
│   ├── OracleComparisonTool.java    # Main entry point
│   ├── config/
│   │   ├── ConfigLoader.java        # Configuration loading
│   │   └── DatabaseConfig.java      # DB config holder
│   ├── model/
│   │   ├── QueryDefinition.java     # Query definition
│   │   ├── QueryResult.java         # Query execution result
│   │   └── ComparisonResult.java    # Comparison result (for email)
│   └── service/
│       ├── QueryExecutor.java       # Executes queries
│       ├── QueryLoader.java         # Loads queries from files
│       ├── CsvReportGenerator.java  # Generates CSV reports
│       └── EmailService.java        # Sends email reports
└── src/main/resources/
    ├── config.properties            # Configuration file
    └── queries.sql                  # MINUS queries for all tables
```

## Troubleshooting

### ORA-00942: table or view does not exist
- Verify you have SELECT privileges on both schemas
- Check if the table exists in `ALS_LEGACY_REPLICA` (some tables like ENTEMP2 may not be replicated)

### Connection timeout
- Check network connectivity to the Exadata
- Verify firewall rules allow connections on port 1521
- Try using the full JDBC URL with SID instead of service name

### Large result sets
- The detail report limits output to 1000 rows per query
- For very large differences, consider adding WHERE clauses to narrow down

## License

MIT License
