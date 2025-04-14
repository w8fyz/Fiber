package sh.fyz.fiber.example;

import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.email.MailColumn;
import sh.fyz.fiber.core.email.Email;
import sh.fyz.fiber.core.email.EmailService;
import sh.fyz.fiber.core.dto.DTOConvertible;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Example demonstrating how to use the email template system with the updated Email class
 */
public class EmailTemplateExample {
    
    public static void main(String[] args) {
        // Initialize FiberServer
        FiberServer fiberServer = new FiberServer(8080);
        
        // Initialize EmailService
        EmailService emailService = new EmailService(
                "smtp.sendgrid.net",
                "thibeau.benet@freshperf.fr",
                465,
                "apikey","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",
                true,
                true
        );
        
        // Set the email service in FiberServer
        fiberServer.setEmailService(emailService);
        
        // Example with multiple tables
        Email emailWithTables = new Email();
        emailWithTables.setTo("fyzdesignyt@gmail.com");
        emailWithTables.setSubject("Your Order Summary");
        
        // Set the template path
        emailWithTables.setTemplatePath("src/main/resources/templates/order-summary-multiple.html");
        
        // Add template variables
        emailWithTables.addTemplateVariable("USER_NAME", "John Doe");
        emailWithTables.addTemplateVariable("ORDER_ID", "12345");
        
        // Create sample order items
        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(new OrderItem("Product 1", 2, 19.99));
        orderItems.add(new OrderItem("Product 2", 1, 49.99));
        
        // Create sample shipping items
        List<ShippingItem> shippingItems = new ArrayList<>();
        shippingItems.add(new ShippingItem("Standard Shipping", 5.99, 3));
        shippingItems.add(new ShippingItem("Express Shipping", 12.99, 1));
        
        // Set multiple tables
        emailWithTables.setTable("ORDER_ITEMS", orderItems, "order-table");
        emailWithTables.setTable("SHIPPING_OPTIONS", shippingItems, "shipping-table");
        
        // Send the email with tables
        CompletableFuture<Void> futureWithTables = emailService.sendEmail(emailWithTables);
        
        // Wait for the email to be sent
        while(!futureWithTables.isDone()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
        System.out.println("Email with multiple tables sent successfully!");
    }
    
    // Sample DTO class for the order items table
    public static class OrderItem extends DTOConvertible {
        @MailColumn(displayName = "Product", bold = true)
        private final String name;
        
        @MailColumn(displayName = "Qty", order = 1)
        private final int quantity;
        
        @MailColumn(displayName = "Price", format = "$%.2f", order = 2)
        private final double price;
        
        @MailColumn(displayName = "Total", format = "$%.2f", bold = true, order = 3)
        private final double total;
        
        public OrderItem(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
            this.total = quantity * price;
        }
        
        public String getName() {
            return name;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public double getPrice() {
            return price;
        }
        
        public double getTotal() {
            return total;
        }
    }
    
    // Sample DTO class for the shipping options table
    public static class ShippingItem extends DTOConvertible {
        @MailColumn(displayName = "Shipping Method", bold = true, order = 0)
        private final String method;
        
        @MailColumn(displayName = "Cost", format = "$%.2f", order = 1)
        private final double cost;
        
        @MailColumn(displayName = "Delivery Time", order = 2)
        private final int days;
        
        @MailColumn(displayName = "Estimated Arrival", italic = true, order = 3)
        private final String estimatedArrival;
        
        public ShippingItem(String method, double cost, int days) {
            this.method = method;
            this.cost = cost;
            this.days = days;
            this.estimatedArrival = days + " business days";
        }
        
        public String getMethod() {
            return method;
        }
        
        public double getCost() {
            return cost;
        }
        
        public int getDays() {
            return days;
        }
        
        public String getEstimatedArrival() {
            return estimatedArrival;
        }
    }
} 