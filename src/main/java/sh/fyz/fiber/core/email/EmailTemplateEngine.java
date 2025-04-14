package sh.fyz.fiber.core.email;

import sh.fyz.fiber.annotations.email.MailColumn;
import sh.fyz.fiber.core.dto.DTOConvertible;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailTemplateEngine {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");
    
    /**
     * Process a template string by replacing variables with their values
     * @param template The template string
     * @param variables Map of variable names to their values
     * @return Processed template with variables replaced
     */
    public static String processTemplate(String template, Map<String, String> variables) {
        System.out.println("--- START PROCESS\n\n\n");
        System.out.println(template);
        if (template == null || variables == null) {
            return template;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = variables.getOrDefault(variableName, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        System.out.println("\n\n\n--- PROCESS END");
        System.out.println(result.toString());
        return result.toString();
    }
    
    /**
     * Process a template file by replacing variables with their values
     * @param templatePath Path to the template file
     * @param variables Map of variable names to their values
     * @return Processed template with variables replaced
     * @throws IOException If the template file cannot be read
     */
    public static String processTemplateFile(String templatePath, Map<String, String> variables) throws IOException {
        String template = Files.readString(Path.of(templatePath));
        return processTemplate(template, variables);
    }
    
    /**
     * Generate an HTML table from a list of data
     * @param headers Table headers
     * @param rows Table rows (each row is an array of cell values)
     * @param cssClass Optional CSS class for the table
     * @return HTML table as a string
     */
    public static String generateTable(String[] headers, String[][] rows, String cssClass) {
        StringBuilder table = new StringBuilder();
        
        // Add table opening tag with optional CSS class
        if (cssClass != null && !cssClass.isEmpty()) {
            table.append("<table class=\"").append(cssClass).append("\">\n");
        } else {
            table.append("<table>\n");
        }
        
        // Add table header
        table.append("<thead>\n<tr>\n");
        for (String header : headers) {
            table.append("<th>").append(header).append("</th>\n");
        }
        table.append("</tr>\n</thead>\n");
        
        // Add table body
        table.append("<tbody>\n");
        for (String[] row : rows) {
            table.append("<tr>\n");
            for (String cell : row) {
                table.append("<td>").append(cell).append("</td>\n");
            }
            table.append("</tr>\n");
        }
        table.append("</tbody>\n");
        
        // Close table
        table.append("</table>");
        
        return table.toString();
    }
    
    /**
     * Generate a shopping cart summary table
     * @param items List of cart items with name, quantity, price, and total
     * @param totalAmount Total amount of the cart
     * @param cssClass Optional CSS class for the table
     * @return HTML table as a string
     */
    
    /**
     * Generate an HTML table from a list of DTOConvertible objects
     * @param items List of DTOConvertible objects
     * @param cssClass Optional CSS class for the table
     * @return HTML table as a string
     */
    public static <T extends DTOConvertible> String generateTableFromDTOs(List<T> items, String cssClass) {
        if (items == null || items.isEmpty()) {
            return "<p>No items to display</p>";
        }
        
        // Get the first item to determine the structure
        T firstItem = items.get(0);
        Map<String, Object> firstDTO = firstItem.asDTO();
        
        // Get all methods from the class
        Method[] methods = firstItem.getClass().getDeclaredMethods();
        
        // Create a list of column definitions
        List<ColumnDefinition> columnDefinitions = new ArrayList<>();
        
        for (Method method : methods) {
            // Only process getter methods
            if (!method.getName().startsWith("get") || method.getParameterCount() > 0) {
                continue;
            }
            
            String methodName = method.getName();
            String fieldName = methodName.substring(3).toLowerCase();
            if (fieldName.length() > 0) {
                fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
            }
            
            // Get the field with the same name
            Field field = null;
            try {
                field = firstItem.getClass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Skip if field not found
                continue;
            }
            
            // Get the MailColumn annotation from the field
            MailColumn annotation = field.getAnnotation(MailColumn.class);
            
            // Skip methods that are marked to be excluded
            if (annotation != null && !annotation.include()) {
                continue;
            }
            
            String displayName = annotation != null && !annotation.displayName().isEmpty() 
                ? annotation.displayName() 
                : fieldName;
            
            boolean bold = annotation != null && annotation.bold();
            boolean italic = annotation != null && annotation.italic();
            boolean monospace = annotation != null && annotation.monospace();
            int order = annotation != null ? annotation.order() : Integer.MAX_VALUE;
            String format = annotation != null ? annotation.format() : "";
            
            columnDefinitions.add(new ColumnDefinition(
                fieldName, displayName, bold, italic, monospace, order, format
            ));
        }
        
        // Sort columns by order
        columnDefinitions.sort(Comparator.comparingInt(ColumnDefinition::getOrder));
        
        // Extract headers and column names
        String[] headers = columnDefinitions.stream()
            .map(ColumnDefinition::getDisplayName)
            .toArray(String[]::new);
        
        String[] columnNames = columnDefinitions.stream()
            .map(ColumnDefinition::getFieldName)
            .toArray(String[]::new);
        
        // Create rows
        String[][] rows = new String[items.size()][columnNames.length];
        
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            Map<String, Object> dto = item.asDTO();
            
            for (int j = 0; j < columnNames.length; j++) {
                String columnName = columnNames[j];
                Object value = dto.get(columnName);
                String formattedValue = formatValue(value, columnDefinitions.get(j).getFormat());
                
                // Apply styling
                if (columnDefinitions.get(j).isBold()) {
                    formattedValue = "<strong>" + formattedValue + "</strong>";
                }
                if (columnDefinitions.get(j).isItalic()) {
                    formattedValue = "<em>" + formattedValue + "</em>";
                }
                if (columnDefinitions.get(j).isMonospace()) {
                    formattedValue = "<code>" + formattedValue + "</code>";
                }
                
                rows[i][j] = formattedValue;
            }
        }
        
        return generateTable(headers, rows, cssClass);
    }
    
    /**
     * Format a value according to the specified format string
     * @param value The value to format
     * @param format The format string
     * @return Formatted value as a string
     */
    private static String formatValue(Object value, String format) {
        if (value == null) {
            return "";
        }
        
        if (format.isEmpty()) {
            return value.toString();
        }
        
        if (value instanceof Number) {
            return String.format(format, value);
        }
        
        return value.toString();
    }
    
    /**
     * Internal class to hold column definition information
     */
    private static class ColumnDefinition {
        private final String fieldName;
        private final String displayName;
        private final boolean bold;
        private final boolean italic;
        private final boolean monospace;
        private final int order;
        private final String format;
        
        public ColumnDefinition(String fieldName, String displayName, boolean bold, 
                               boolean italic, boolean monospace, int order, String format) {
            this.fieldName = fieldName;
            this.displayName = displayName;
            this.bold = bold;
            this.italic = italic;
            this.monospace = monospace;
            this.order = order;
            this.format = format;
        }
        
        public String getFieldName() {
            return fieldName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isBold() {
            return bold;
        }
        
        public boolean isItalic() {
            return italic;
        }
        
        public boolean isMonospace() {
            return monospace;
        }
        
        public int getOrder() {
            return order;
        }
        
        public String getFormat() {
            return format;
        }
    }
} 