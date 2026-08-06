package com.aistudio.infrastructure.mail;

public interface EmailPort {
    void send(String to, String subject, String body);
}
