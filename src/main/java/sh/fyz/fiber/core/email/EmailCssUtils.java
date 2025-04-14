package sh.fyz.fiber.core.email;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Utility class for handling CSS in emails, including converting CSS to inline styles.
 */
public class EmailCssUtils {
    private static final Logger LOGGER = Logger.getLogger(EmailCssUtils.class.getName());
    
    /**
     * Converts CSS from a style tag to inline styles in the HTML content.
     * 
     * @param htmlContent The HTML content containing a style tag
     * @return The HTML content with CSS converted to inline styles
     */
    public static String convertCssToInline(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            LOGGER.warning("HTML content is null or empty");
            return htmlContent;
        }
        
        try {
            // Extract CSS from style tag
            String cssContent = extractCssFromStyleTag(htmlContent);
            if (cssContent == null || cssContent.isEmpty()) {
                return htmlContent;
            }
            
            // Parse CSS rules
            Map<String, Map<String, String>> cssRules = parseCssRules(cssContent);
            
            // Apply CSS rules to HTML elements
            String result = applyCssRules(htmlContent, cssRules);
            
            // Remove the style tag
            result = removeStyleTag(result);
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return htmlContent;
        }
    }
    
    /**
     * Extracts CSS content from all style tags in the HTML.
     * 
     * @param htmlContent The HTML content containing style tags
     * @return The concatenated CSS content from all style tags, or null if none found
     */
    private static String extractCssFromStyleTag(String htmlContent) {
        Pattern stylePattern = Pattern.compile("<style[^>]*>(.*?)</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = stylePattern.matcher(htmlContent);
        StringBuilder cssContent = new StringBuilder();
        boolean found = false;
        
        while (matcher.find()) {
            found = true;
            cssContent.append(matcher.group(1).trim()).append("\n");
        }
        
        return found ? cssContent.toString() : null;
    }
    
    /**
     * Parses CSS rules into a map of selectors to style properties.
     * 
     * @param cssContent The CSS content to parse
     * @return A map of CSS selectors to style properties
     */
    private static Map<String, Map<String, String>> parseCssRules(String cssContent) {
        Map<String, Map<String, String>> cssRules = new HashMap<>();
        
        // Split CSS content into rule blocks
        Pattern ruleBlockPattern = Pattern.compile("([^{]+)\\s*\\{([^}]+)\\}");
        Matcher ruleBlockMatcher = ruleBlockPattern.matcher(cssContent);
        
        while (ruleBlockMatcher.find()) {
            String selector = ruleBlockMatcher.group(1).trim();
            String properties = ruleBlockMatcher.group(2).trim();
            
            // Parse properties
            Map<String, String> propertyMap = new HashMap<>();
            Pattern propertyPattern = Pattern.compile("([^:]+):\\s*([^;]+);");
            Matcher propertyMatcher = propertyPattern.matcher(properties);
            
            while (propertyMatcher.find()) {
                String property = propertyMatcher.group(1).trim();
                String value = propertyMatcher.group(2).trim();
                propertyMap.put(property, value);
            }
            
            cssRules.put(selector, propertyMap);
        }
        
        return cssRules;
    }
    
    /**
     * Applies CSS rules to HTML elements.
     * 
     * @param htmlContent The HTML content
     * @param cssRules The CSS rules to apply
     * @return The HTML content with CSS rules applied as inline styles
     */
    private static String applyCssRules(String htmlContent, Map<String, Map<String, String>> cssRules) {
        String result = htmlContent;
        
        for (Map.Entry<String, Map<String, String>> entry : cssRules.entrySet()) {
            String selector = entry.getKey();
            Map<String, String> properties = entry.getValue();
            
            // Convert CSS properties to inline style string
            StringBuilder styleBuilder = new StringBuilder();
            for (Map.Entry<String, String> prop : properties.entrySet()) {
                styleBuilder.append(prop.getKey()).append(":").append(prop.getValue()).append(";");
            }
            String styleString = styleBuilder.toString();
            
            // Apply styles to elements matching the selector
            if (selector.startsWith(".")) {
                // Class selector
                String className = selector.substring(1);
                result = applyClassStyles(result, className, styleString);
            } else if (selector.startsWith("#")) {
                // ID selector
                String id = selector.substring(1);
                result = applyIdStyles(result, id, styleString);
            } else {
                // Element selector
                result = applyElementStyles(result, selector, styleString);
            }
        }
        
        return result;
    }
    
    /**
     * Applies styles to elements with a specific class.
     * 
     * @param htmlContent The HTML content
     * @param className The class name
     * @param styleString The style string to apply
     * @return The HTML content with styles applied
     */
    private static String applyClassStyles(String htmlContent, String className, String styleString) {
        Pattern classPattern = Pattern.compile("class=[\"']([^\"']*\\b" + Pattern.quote(className) + "\\b[^\"']*)[\"']");
        Matcher matcher = classPattern.matcher(htmlContent);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String classAttr = matcher.group(1);
            String replacement = "class=\"" + classAttr + "\" style=\"" + styleString + "\"";
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Applies styles to elements with a specific ID.
     * 
     * @param htmlContent The HTML content
     * @param id The ID
     * @param styleString The style string to apply
     * @return The HTML content with styles applied
     */
    private static String applyIdStyles(String htmlContent, String id, String styleString) {
        Pattern idPattern = Pattern.compile("id=[\"']" + Pattern.quote(id) + "[\"']");
        Matcher matcher = idPattern.matcher(htmlContent);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = "id=\"" + id + "\" style=\"" + styleString + "\"";
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Applies styles to elements with a specific tag name.
     * 
     * @param htmlContent The HTML content
     * @param tagName The tag name
     * @param styleString The style string to apply
     * @return The HTML content with styles applied
     */
    private static String applyElementStyles(String htmlContent, String tagName, String styleString) {
        Pattern tagPattern = Pattern.compile("<" + Pattern.quote(tagName) + "(\\s[^>]*)?>");
        Matcher matcher = tagPattern.matcher(htmlContent);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String tag = matcher.group(0);
            String attributes = matcher.group(1);
            
            if (attributes == null || !attributes.contains("style=")) {
                String replacement = "<" + tagName + (attributes != null ? attributes : "") + " style=\"" + styleString + "\">";
                matcher.appendReplacement(result, replacement);
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Removes the style tag from the HTML content.
     * 
     * @param htmlContent The HTML content
     * @return The HTML content with the style tag removed
     */
    private static String removeStyleTag(String htmlContent) {
        return htmlContent.replaceAll("<style[^>]*>.*?</style>", "");
    }
} 