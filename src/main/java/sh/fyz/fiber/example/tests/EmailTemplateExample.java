package sh.fyz.fiber.example.tests;

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
import java.time.Year;

/**
 * Example demonstrating how to use the email template system with imports
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
        
        // Example with template imports
        Email emailTest = new Email();
        emailTest.setTo("t.benet@teleparis.fr");
        emailTest.setSubject("Welcome to Our Service");

        emailTest.setTemplatePath("src/main/resources/templates/example-with-imports.html");
        
        // Variables used in all templates (header, content, and footer)
        emailTest.addTemplateVariable("HEADER_TITLE", "Welcome to Our Service");
        emailTest.addTemplateVariable("USERNAME", "John");
        emailTest.addTemplateVariable("EMAIL", "john@example.com");
        emailTest.addTemplateVariable("SIGNUP_DATE", "2024-03-21");
        emailTest.addTemplateVariable("CTA_LINK", "https://example.com/get-started");
        emailTest.addTemplateVariable("CTA_TEXT", "Get Started");
        emailTest.addTemplateVariable("COMPANY_NAME", "Example Corp");
        emailTest.addTemplateVariable("SUPPORT_EMAIL", "support@example.com");
        emailTest.addTemplateVariable("YEAR", Year.now().toString());

        CompletableFuture<Void> completableFuture = fiberServer.getEmailService().sendEmail(emailTest);

        while (!completableFuture.isDone()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
} 