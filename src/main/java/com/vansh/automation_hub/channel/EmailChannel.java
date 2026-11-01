package com.vansh.automation_hub.channel;

import com.vansh.automation_hub.exception.ChannelSendException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailChannel implements MessageChannel {

    // Sender ka address (Gmail mein yahi tumhara account hota hai)
    @Value("${spring.mail.username:}")
    private String from;

    @Value("${spring.mail.password:}")
    private String password;

    // Spring ka mail bhejne wala tool (spring.mail.* settings se banta hai)
    private final JavaMailSender mailSender;

    public EmailChannel(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String name() {
        return "EMAIL";
    }

    // Credentials set hain toh hi channel available
    @Override
    public boolean isAvailable() {
        return !from.isBlank() && !password.isBlank();
    }

    @Override
    public void send(String target, String message) {
        if (!isAvailable()) {
            throw new ChannelSendException("Email is not configured", false);
        }
        if (target == null || target.isBlank()) {
            throw new ChannelSendException("Email job has no target address", false);
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(target);
        mail.setSubject("Automation Hub reminder");
        mail.setText(message);

        try {
            mailSender.send(mail);
        } catch (MailAuthenticationException e) {
            // Galat app password: retry se theek nahi hoga
            throw new ChannelSendException("Email authentication failed (check app password)", false);
        } catch (MailSendException e) {
            // Server ya connection problem: thodi der baad theek ho sakti hai
            throw new ChannelSendException("Email could not be sent (network or SMTP problem)", true);
        } catch (MailException e) {
            throw new ChannelSendException("Unexpected email error: "
                    + e.getClass().getSimpleName(), false);
        }
    }
}