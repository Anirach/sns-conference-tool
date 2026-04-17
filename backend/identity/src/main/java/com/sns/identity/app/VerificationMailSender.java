package com.sns.identity.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class VerificationMailSender {

    private static final Logger log = LoggerFactory.getLogger(VerificationMailSender.class);

    private final JavaMailSender sender;
    private final String from;

    public VerificationMailSender(JavaMailSender sender, @Value("${sns.mail.from:no-reply@sns.local}") String from) {
        this.sender = sender;
        this.from = from;
    }

    public void sendTan(String email, String tan) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(email);
            msg.setSubject("Your SNS verification cipher");
            msg.setText("Your six-digit cipher is: " + tan + "\n\nIt expires in 15 minutes.");
            sender.send(msg);
        } catch (Exception e) {
            // Mail failures should not block registration in dev. Log loudly.
            log.warn("Failed to send verification email to {} (tan unsent): {}", email, e.getMessage());
        }
    }
}
