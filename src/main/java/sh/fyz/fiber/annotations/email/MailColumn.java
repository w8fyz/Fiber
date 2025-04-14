package sh.fyz.fiber.annotations.email;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for customizing how a field is displayed in email tables
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface MailColumn {
    /**
     * The display name for the column. If not provided, the field name will be used.
     */
    String displayName() default "";
    
    /**
     * Whether the column values should be displayed in bold
     */
    boolean bold() default false;
    
    /**
     * Whether the column values should be displayed in italic
     */
    boolean italic() default false;
    
    /**
     * Whether the column values should be displayed in a monospace font
     */
    boolean monospace() default false;
    
    /**
     * The order of the column in the table. Lower values appear first.
     */
    int order() default Integer.MAX_VALUE;
    
    /**
     * Whether to include this column in the table
     */
    boolean include() default true;
    
    /**
     * Format string for numeric values (e.g., "$%.2f" for currency)
     */
    String format() default "";
} 