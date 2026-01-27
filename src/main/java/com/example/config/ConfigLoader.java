package com.example.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration from properties file.
 */
public class ConfigLoader {
    private final Properties properties;

    public ConfigLoader() throws IOException {
        this("config.properties");
    }

    public ConfigLoader(String configFile) throws IOException {
        properties = new Properties();
        
        // Try loading from classpath first, then from file system
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
        
        if (inputStream == null) {
            inputStream = new FileInputStream(configFile);
        }
        
        properties.load(inputStream);
        inputStream.close();
    }

    public DatabaseConfig getLegacyConfig() {
        // Support both old dual-database config and new single-database config
        String url = properties.getProperty("legacy.jdbc.url");
        if (url == null || url.isEmpty()) {
            url = properties.getProperty("jdbc.url");
        }
        String user = properties.getProperty("legacy.jdbc.user");
        if (user == null || user.isEmpty()) {
            user = properties.getProperty("jdbc.user");
        }
        String password = properties.getProperty("legacy.jdbc.password");
        if (password == null || password.isEmpty()) {
            password = properties.getProperty("jdbc.password");
        }
        
        return new DatabaseConfig(
                "Legacy Exadata",
                url,
                user,
                password
        );
    }

    public DatabaseConfig getModernConfig() {
        // Support both old dual-database config and new single-database config
        String url = properties.getProperty("modern.jdbc.url");
        if (url == null || url.isEmpty()) {
            url = properties.getProperty("jdbc.url");
        }
        String user = properties.getProperty("modern.jdbc.user");
        if (user == null || user.isEmpty()) {
            user = properties.getProperty("jdbc.user");
        }
        String password = properties.getProperty("modern.jdbc.password");
        if (password == null || password.isEmpty()) {
            password = properties.getProperty("jdbc.password");
        }
        
        return new DatabaseConfig(
                "Modern Exadata",
                url,
                user,
                password
        );
    }

    /**
     * Gets the single database configuration (for MINUS queries on same DB).
     */
    public DatabaseConfig getDatabaseConfig() {
        return new DatabaseConfig(
                "Database",
                properties.getProperty("jdbc.url"),
                properties.getProperty("jdbc.user"),
                properties.getProperty("jdbc.password")
        );
    }
    
    /**
     * Checks if single database mode is configured.
     */
    public boolean isSingleDatabaseMode() {
        String singleUrl = properties.getProperty("jdbc.url");
        return singleUrl != null && !singleUrl.isEmpty();
    }

    public String getOutputDirectory() {
        return properties.getProperty("output.directory", "./reports");
    }

    public String getOutputFilenamePrefix() {
        return properties.getProperty("output.filename.prefix", "comparison_report");
    }

    public boolean isEmailEnabled() {
        return Boolean.parseBoolean(properties.getProperty("email.enabled", "false"));
    }

    public Properties getEmailProperties() {
        Properties emailProps = new Properties();
        emailProps.put("mail.smtp.host", properties.getProperty("email.smtp.host", ""));
        emailProps.put("mail.smtp.port", properties.getProperty("email.smtp.port", "587"));
        emailProps.put("mail.smtp.auth", properties.getProperty("email.smtp.auth", "true"));
        emailProps.put("mail.smtp.starttls.enable", properties.getProperty("email.smtp.starttls", "true"));
        return emailProps;
    }

    public String getEmailFrom() {
        return properties.getProperty("email.from", "");
    }

    public String getEmailTo() {
        return properties.getProperty("email.to", "");
    }

    public String getEmailCc() {
        return properties.getProperty("email.cc", "");
    }

    public String getEmailSubject() {
        return properties.getProperty("email.subject", "Oracle Exadata Comparison Report");
    }

    public String getEmailUsername() {
        return properties.getProperty("email.username", "");
    }

    public String getEmailPassword() {
        return properties.getProperty("email.password", "");
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
