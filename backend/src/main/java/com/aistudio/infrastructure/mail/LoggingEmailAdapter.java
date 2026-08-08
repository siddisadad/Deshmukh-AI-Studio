package com.aistudio.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aistudio.mail.provider", havingValue = "logging", matchIfMissing = true)
public class LoggingEmailAdapter implements EmailPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailAdapter.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("EMAIL to={} subject={} body={}", to, subject, body);
    }
}
