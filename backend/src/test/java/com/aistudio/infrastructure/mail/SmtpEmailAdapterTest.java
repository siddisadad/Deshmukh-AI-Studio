package com.aistudio.infrastructure.mail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpEmailAdapterTest {

    @Mock JavaMailSender mailSender;

    @Test
    void sendDelegatesToJavaMailSender() {
        SmtpEmailAdapter adapter = new SmtpEmailAdapter(mailSender, "noreply@example.com");
        adapter.send("user@example.com", "Subject", "Body text");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
