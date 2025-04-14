package sh.fyz.fiber.core.email;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import sh.fyz.fiber.core.dto.DTOConvertible;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

public class EmailService {
    private final jakarta.mail.Session session;
    private final String from;
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final boolean useSSL;
    private final boolean useTLS;
    private final boolean convertCssToInline;

    public EmailService(String host, String from, int port, boolean useSSL, boolean useTLS) {
        this(host, from, port, null, null, useSSL, useTLS, true);
    }

    public EmailService(String host, String from, int port, String username, String password, boolean useSSL, boolean useTLS) {
        this(host, from, port, username, password, useSSL, useTLS, true);
    }

    public EmailService(String host, String from, int port, String username, String password, boolean useSSL, boolean useTLS, boolean convertCssToInline) {
        this.host = host;
        this.from = from;
        this.port = port;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
        this.useTLS = useTLS;
        this.convertCssToInline = convertCssToInline;
        this.session = createSession();
    }

    private jakarta.mail.Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        
        if (useSSL) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        if (useTLS) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        if(username == null || password == null) {
            return jakarta.mail.Session.getInstance(props);
        }

        return jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    public CompletableFuture<Void> sendEmail(Email email) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Process template if specified
                if (email.getTemplatePath() != null) {
                    processTemplate(email);
                }
                
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.getTo()));
                message.setSubject(email.getSubject());

                MimeMultipart multipart = new MimeMultipart();
                
                // Add text content
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(email.getContent());
                multipart.addBodyPart(textPart);

                // Add HTML content if available
                if (email.getHtmlContent() != null) {
                    MimeBodyPart htmlPart = new MimeBodyPart();
                    
                    // Convert CSS classes to inline styles if enabled
                    String htmlContent = email.getHtmlContent();
                    if (convertCssToInline) {
                        try {
                            htmlContent = EmailCssUtils.convertCssToInline(htmlContent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
                    multipart.addBodyPart(htmlPart);
                }

                // Add attachments
                if (email.getAttachments() != null && !email.getAttachments().isEmpty()) {
                    for (EmailAttachment attachment : email.getAttachments()) {
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        attachmentPart.attachFile(attachment.getFile());
                        attachmentPart.setFileName(attachment.getFileName());
                        multipart.addBodyPart(attachmentPart);
                    }
                }

                message.setContent(multipart);
                Transport.send(message);
            } catch (MessagingException | IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Process the email template and set the HTML content
     * @param email The email with template information
     * @throws IOException If the template file cannot be read
     */
    private void processTemplate(Email email) throws IOException {
        String templatePath = email.getTemplatePath();
        Map<String, String> variables = email.getTemplateVariables();
        
        // Process all tables
        if (email.getTables() != null) {
            for (Map.Entry<String, Email.TableData> entry : email.getTables().entrySet()) {
                String variableName = entry.getKey();
                Email.TableData tableData = entry.getValue();
                
                if (tableData.getItems() != null && !tableData.getItems().isEmpty()) {
                    String table = EmailTemplateEngine.generateTableFromDTOs(
                        (List<DTOConvertible>) tableData.getItems(), 
                        tableData.getCssClass()
                    );
                    variables.put(variableName, table);
                }
            }
        }
        
        // Process the template with variables
        String htmlContent = EmailTemplateEngine.processTemplateFile(templatePath, variables);
        // Convert CSS to inline styles if enabled
        if (convertCssToInline) {
            try {
                htmlContent = EmailCssUtils.convertCssToInline(htmlContent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        email.setHtmlContent(htmlContent);
    }
} 