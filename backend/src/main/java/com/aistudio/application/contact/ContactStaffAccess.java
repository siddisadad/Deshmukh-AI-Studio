package com.aistudio.application.contact;

import com.aistudio.domain.common.DomainException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ContactStaffAccess {

    private final Set<String> staffEmails;

    public ContactStaffAccess(@Value("${aistudio.contact.staff-emails:}") String staffEmailsCsv) {
        this.staffEmails = Arrays.stream(staffEmailsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean canAccessInbox(String email) {
        if (email == null || email.isBlank() || staffEmails.isEmpty()) {
            return false;
        }
        return staffEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    public void requireStaff(String email) {
        if (!canAccessInbox(email)) {
            throw new DomainException("FORBIDDEN", "Contact inbox is limited to Deshmukh Technology staff");
        }
    }
}
