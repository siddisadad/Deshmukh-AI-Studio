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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    ContactStaffAccess staffAccess;
    ContactInquiryService service;

    @BeforeEach
    void setUp() {
        staffAccess = new ContactStaffAccess("staff@deshmukh.tech");
        service = new ContactInquiryService(
                contactInquiryRepository, emailPort, staffAccess, "hello@deshmukh.tech");
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

    @Test
    void listForStaffRequiresAllowlist() {
        assertThatThrownBy(() -> service.listForStaff("stranger@example.com"))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void markReadSetsTimestamp() {
        UUID id = UUID.randomUUID();
        ContactInquiryEntity entity = new ContactInquiryEntity();
        entity.setId(id);
        entity.setName("Ada");
        entity.setEmail("ada@example.com");
        entity.setTopic("Partnership");
        entity.setMessage("Hello");
        entity.setCreatedAt(Instant.parse("2026-08-09T12:00:00Z"));
        when(contactInquiryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(contactInquiryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.markRead("staff@deshmukh.tech", id);

        assertThat(response.readAt()).isNotNull();
        assertThat(entity.getReadAt()).isNotNull();
        verify(contactInquiryRepository).save(entity);
    }

    @Test
    void listForStaffReturnsNewestFirst() {
        ContactInquiryEntity entity = new ContactInquiryEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Ada");
        entity.setEmail("ada@example.com");
        entity.setTopic("AI Studio");
        entity.setMessage("Hi");
        entity.setCreatedAt(Instant.parse("2026-08-09T12:00:00Z"));
        when(contactInquiryRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(entity));

        var items = service.listForStaff("staff@deshmukh.tech");

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().email()).isEqualTo("ada@example.com");
    }

    @Test
    void unreadCountForStaffReturnsZeroForNonStaff() {
        assertThat(service.unreadCountForStaff("stranger@example.com")).isZero();
        verify(contactInquiryRepository, never()).countByReadAtIsNull();
    }

    @Test
    void unreadCountForStaffUsesRepository() {
        when(contactInquiryRepository.countByReadAtIsNull()).thenReturn(3L);
        assertThat(service.unreadCountForStaff("staff@deshmukh.tech")).isEqualTo(3L);
    }
}
