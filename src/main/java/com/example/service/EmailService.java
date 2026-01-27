package com.example.service;

import com.example.config.ConfigLoader;
import com.example.model.ComparisonResult;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

/**
 * Sends comparison reports via email.
 */
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConfigLoader config;

    public EmailService(ConfigLoader config) {
        this.config = config;
    }

    /**
     * Sends the comparison report via email.
     */
    public boolean sendReport(List<ComparisonResult> results, List<String> attachmentPaths) {
        if (!config.isEmailEnabled()) {
            logger.info("Email is disabled in configuration");
            return false;
        }

        try {
            Properties props = config.getEmailProperties();
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            config.getEmailUsername(),
                            config.getEmailPassword()
                    );
                }
            });

            Message message = createMessage(session, results, attachmentPaths);
            Transport.send(message);
            
            logger.info("Email sent successfully to: {}", config.getEmailTo());
            return true;

        } catch (MessagingException e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Creates the email message with attachments.
     */
    private Message createMessage(Session session, List<ComparisonResult> results, 
                                   List<String> attachmentPaths) throws MessagingException {
        
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getEmailFrom()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getEmailTo()));
        
        String cc = config.getEmailCc();
        if (cc != null && !cc.isEmpty()) {
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
        }
        
        message.setSubject(config.getEmailSubject() + " - " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        // Create multipart message
        Multipart multipart = new MimeMultipart();

        // Add text body
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(createEmailBody(results), "utf-8", "plain");
        multipart.addBodyPart(textPart);

        // Add HTML body
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(createHtmlBody(results), "text/html; charset=utf-8");
        multipart.addBodyPart(htmlPart);

        // Add attachments
        for (String filePath : attachmentPaths) {
            File file = new File(filePath);
            if (file.exists()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new FileDataSource(file);
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setFileName(file.getName());
                multipart.addBodyPart(attachmentPart);
                logger.debug("Added attachment: {}", file.getName());
            }
        }

        message.setContent(multipart);
        return message;
    }

    /**
     * Creates the plain text email body.
     */
    private String createEmailBody(List<ComparisonResult> results) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Oracle Exadata Comparison Report\n");
        sb.append("================================\n\n");
        sb.append("Report Generated: ").append(LocalDateTime.now().format(DATE_FORMAT)).append("\n\n");

        // Summary
        long identicalCount = results.stream().filter(ComparisonResult::isIdentical).count();
        long differentCount = results.size() - identicalCount;
        
        sb.append("SUMMARY\n");
        sb.append("-------\n");
        sb.append("Total Queries: ").append(results.size()).append("\n");
        sb.append("Identical Results: ").append(identicalCount).append("\n");
        sb.append("Different Results: ").append(differentCount).append("\n\n");

        // Details
        sb.append("QUERY DETAILS\n");
        sb.append("-------------\n\n");
        
        for (ComparisonResult result : results) {
            sb.append(result.getSummary()).append("\n");
        }

        sb.append("\n---\n");
        sb.append("This is an automated report. Please review the attached CSV files for detailed data.\n");

        return sb.toString();
    }

    /**
     * Creates an HTML email body.
     */
    private String createHtmlBody(List<ComparisonResult> results) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html><head><style>\n");
        sb.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        sb.append("h1 { color: #333; }\n");
        sb.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        sb.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
        sb.append("th { background-color: #4472C4; color: white; }\n");
        sb.append("tr:nth-child(even) { background-color: #f9f9f9; }\n");
        sb.append(".identical { color: green; font-weight: bold; }\n");
        sb.append(".different { color: red; font-weight: bold; }\n");
        sb.append(".summary-box { background-color: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0; }\n");
        sb.append("</style></head><body>\n");

        sb.append("<h1>Oracle Exadata Comparison Report</h1>\n");
        sb.append("<p>Report Generated: ").append(LocalDateTime.now().format(DATE_FORMAT)).append("</p>\n");

        // Summary box
        long identicalCount = results.stream().filter(ComparisonResult::isIdentical).count();
        long differentCount = results.size() - identicalCount;
        
        sb.append("<div class='summary-box'>\n");
        sb.append("<h2>Summary</h2>\n");
        sb.append("<p><strong>Total Queries:</strong> ").append(results.size()).append("</p>\n");
        sb.append("<p><strong>Identical Results:</strong> <span class='identical'>").append(identicalCount).append("</span></p>\n");
        sb.append("<p><strong>Different Results:</strong> <span class='different'>").append(differentCount).append("</span></p>\n");
        sb.append("</div>\n");

        // Results table
        sb.append("<h2>Query Results</h2>\n");
        sb.append("<table>\n");
        sb.append("<tr><th>Query Name</th><th>Status</th><th>Legacy Rows</th><th>Modern Rows</th>");
        sb.append("<th>Legacy Time</th><th>Modern Time</th><th>Differences</th></tr>\n");

        for (ComparisonResult result : results) {
            String statusClass = result.isIdentical() ? "identical" : "different";
            String status = result.isIdentical() ? "IDENTICAL" : "DIFFERENT";
            
            sb.append("<tr>");
            sb.append("<td>").append(escapeHtml(result.getQueryName())).append("</td>");
            sb.append("<td class='").append(statusClass).append("'>").append(status).append("</td>");
            sb.append("<td>").append(result.getLegacyResult().getRowCount()).append("</td>");
            sb.append("<td>").append(result.getModernResult().getRowCount()).append("</td>");
            sb.append("<td>").append(result.getLegacyResult().getExecutionTimeMs()).append(" ms</td>");
            sb.append("<td>").append(result.getModernResult().getExecutionTimeMs()).append(" ms</td>");
            sb.append("<td>").append(escapeHtml(String.join("; ", result.getDifferences()))).append("</td>");
            sb.append("</tr>\n");
        }
        
        sb.append("</table>\n");

        sb.append("<p><em>Please review the attached CSV files for detailed data comparison.</em></p>\n");
        sb.append("</body></html>");

        return sb.toString();
    }

    /**
     * Escapes HTML special characters.
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
