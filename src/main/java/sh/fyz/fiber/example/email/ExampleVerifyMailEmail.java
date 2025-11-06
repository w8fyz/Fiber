package sh.fyz.fiber.example.email;

import sh.fyz.fiber.core.email.Email;

public class ExampleVerifyMailEmail extends Email {

    public ExampleVerifyMailEmail(String to, String verificationToken) {
        super(to, "Email Verification");
        setTemplatePath("src/main/resources/templates/verify_email.html");
        addTemplateVariable("VERIFICATION_TOKEN", verificationToken);
    }

}
