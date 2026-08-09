package com.aistudio.application.contact;

import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.mail.EmailPort;
import com.aistudio.infrastructure.persistence.entity.ContactInquiryEntity;
import com.aistudio.infrastructure.persistence.repository.ContactInquiryRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactInquiryService {

    private static final Logger log = LoggerFactory.getLogger(ContactInquiryService.class);
    private static final int MAX_PER_EMAIL_PER_HOUR = 5;

    private final ContactInquiryRepository contactInquiryRepository;
    private final EmailPort emailPort;
    private final String notifyEmail;

    public ContactInquiryService(
            ContactInquiryRepository contactInquiryRepository,
            EmailPort emailPort,
            @Value("${aistudio.contact.notify-email:hello@deshmukh.tech}") String notifyEmail
    ) {
        this.contactInquiryRepository = contactInquiryRepository;
        this.emailPort = emailPort;
        this.notifyEmail = notifyEmail;
    }

    @Transactional
    public UUID submit(String name, String email, String topic, String message, String sourceIp) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        long recent = contactInquiryRepository.countByEmailIgnoreCaseAndCreatedAtAfter(normalizedEmail, since);
        if (recent >= MAX_PER_EMAIL_PER_HOUR) {
            throw new DomainException("RATE_LIMITED", "Too many contact messages from this email. Try again later.");
        }

        ContactInquiryEntity entity = new ContactInquiryEntity();
        entity.setName(name.trim());
        entity.setEmail(normalizedEmail);
        entity.setTopic(topic.trim());
        entity.setMessage(message.trim());
        entity.setSourceIp(sourceIp);
        contactInquiryRepository.save(entity);

        String subject = "Deshmukh Technology contact — " + entity.getTopic();
        String body = """
                New contact inquiry

                Name: %s
                Email: %s
                Topic: %s
                IP: %s
                Id: %s

                Message:
                %s
                """.formatted(
                entity.getName(),
                entity.getEmail(),
                entity.getTopic(),
                entity.getSourceIp() == null ? "-" : entity.getSourceIp(),
                entity.getId(),
                entity.getMessage()
        );
        try {
            emailPort.send(notifyEmail, subject, body);
        } catch (RuntimeException ex) {
            log.warn("Contact notify email failed id={} error={}", entity.getId(), ex.getMessage());
        }

        log.info("Contact inquiry stored id={} topic={}", entity.getId(), entity.getTopic());
        return entity.getId();
    }
}
