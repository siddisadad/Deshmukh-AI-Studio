package com.aistudio.application.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.mail.EmailPort;
import com.aistudio.infrastructure.persistence.entity.ContactInquiryEntity;
import com.aistudio.infrastructure.persistence.repository.ContactInquiryRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactInquiryServiceTest {

    @Mock ContactInquiryRepository contactInquiryRepository;
    @Mock EmailPort emailPort;

    ContactInquiryService service;

    @BeforeEach
    void setUp() {
        service = new ContactInquiryService(contactInquiryRepository, emailPort, "hello@deshmukh.tech");
    }

    @Test
    void submitPersistsAndNotifies() {
        when(contactInquiryRepository.countByEmailIgnoreCaseAndCreatedAtAfter(eq("ada@example.com"), any()))
                .thenReturn(0L);
        when(contactInquiryRepository.save(any(ContactInquiryEntity.class))).thenAnswer(invocation -> {
            ContactInquiryEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });

        UUID id = service.submit("Ada", "Ada@Example.com", "Partnership", "Hello there", "203.0.113.10");

        assertThat(id).isNotNull();
        ArgumentCaptor<ContactInquiryEntity> captor = ArgumentCaptor.forClass(ContactInquiryEntity.class);
        verify(contactInquiryRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(captor.getValue().getTopic()).isEqualTo("Partnership");
        verify(emailPort).send(eq("hello@deshmukh.tech"), any(), any());
    }

    @Test
    void submitRateLimitsRepeatEmail() {
        when(contactInquiryRepository.countByEmailIgnoreCaseAndCreatedAtAfter(eq("ada@example.com"), any()))
                .thenReturn(5L);

        assertThatThrownBy(() -> service.submit("Ada", "ada@example.com", "AI Studio", "Hi", "127.0.0.1"))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("RATE_LIMITED");
        verify(contactInquiryRepository, never()).save(any());
    }
}
