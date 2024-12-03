package org.nickas21.smart.data.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;


@Slf4j
@Service
public class GmailService {

    @Value("${gmail.username}")
    private String gmailUsername;

    @Value("${gmail.password}")
    private String gmailPassword;

    @Value("${gmail.recipient}")
    private String gmailRecipient;

    private final static String host = "smtp.gmail.com";
    private final static String port = "587";
    private final static String subject = "Email from floor";
    private Properties properties;


    @PostConstruct
    public void preInstall() {
        // Start settings SMTP
        this.properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);

    }

    public void sendMessage(String msg) {
        // Create Session
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(gmailUsername, gmailPassword);
            }
        });

        try {
            // Create Message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(gmailUsername)); // Sender
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(gmailRecipient)); // Recipient
            message.setSubject(subject);
            message.setText(msg);

            // Send
            Transport.send(message);

            log.info("Email with message: [{}] sent successfully!", msg);

        } catch (AuthenticationFailedException e) {
            log.error("AuthenticationFailedException", e);
        } catch (SendFailedException e) {
            log.error("SendFailedException", e);
        } catch (MessagingException e) {
            log.error("MessagingException", e);
        }
    }

}
