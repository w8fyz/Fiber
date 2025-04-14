package sh.fyz.fiber.core.email;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Email {
    private String to;
    private String subject;
    private String content = "";
    private String htmlContent;
    private final List<EmailAttachment> attachments;
    private String templatePath;
    private Map<String, String> templateVariables;
    private Map<String, TableData> tables;

    public Email() {
        this.attachments = new ArrayList<>();
        this.templateVariables = new HashMap<>();
        this.tables = new HashMap<>();
    }

    public Email(String to, String subject) {
        this();
        this.to = to;
        this.subject = subject;
    }


    public Email(String to, String subject, String content) {
        this();
        this.to = to;
        this.subject = subject;
        this.content = content;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public List<EmailAttachment> getAttachments() {
        return attachments;
    }

    public void addAttachment(EmailAttachment attachment) {
        this.attachments.add(attachment);
    }
    
    public String getTemplatePath() {
        return templatePath;
    }
    
    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }
    
    public Map<String, String> getTemplateVariables() {
        return templateVariables;
    }
    
    public void setTemplateVariables(Map<String, String> templateVariables) {
        this.templateVariables = templateVariables;
    }
    
    public void addTemplateVariable(String key, String value) {
        this.templateVariables.put(key, value);
    }
    
    /**
     * Add a table to the email template
     * @param variableName The variable name in the template where the table should be inserted
     * @param items List of DTOConvertible objects to display in the table
     * @param cssClass Optional CSS class for the table
     */
    public void setTable(String variableName, List<?> items, String cssClass) {
        this.tables.put(variableName, new TableData(items, cssClass));
    }
    
    /**
     * Add a table to the email template with default CSS class
     * @param variableName The variable name in the template where the table should be inserted
     * @param items List of DTOConvertible objects to display in the table
     */
    public void setTable(String variableName, List<?> items) {
        this.setTable(variableName, items, null);
    }
    
    /**
     * Get all tables configured for this email
     * @return Map of table variable names to table data
     */
    public Map<String, TableData> getTables() {
        return tables;
    }
    
    /**
     * Internal class to hold table data
     */
    public static class TableData {
        private final List<?> items;
        private final String cssClass;
        
        public TableData(List<?> items, String cssClass) {
            this.items = items;
            this.cssClass = cssClass;
        }
        
        public List<?> getItems() {
            return items;
        }
        
        public String getCssClass() {
            return cssClass;
        }
    }
} 