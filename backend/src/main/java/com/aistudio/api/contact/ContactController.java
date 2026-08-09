package com.aistudio.api.contact;

import com.aistudio.api.contact.dto.ContactInboxAccessResponse;
import com.aistudio.api.contact.dto.ContactInquiryListItemResponse;
import com.aistudio.api.contact.dto.ContactInquiryResponse;
import com.aistudio.api.contact.dto.CreateContactInquiryRequest;
import com.aistudio.api.contact.dto.MarkAllReadResponse;
import com.aistudio.application.contact.ContactInquiryService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Contact")
public class ContactController {

    private final ContactInquiryService contactInquiryService;

    public ContactController(ContactInquiryService contactInquiryService) {
        this.contactInquiryService = contactInquiryService;
    }

    @PostMapping("/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a public contact inquiry from the marketing site")
    public ContactInquiryResponse create(
            @Valid @RequestBody CreateContactInquiryRequest request,
            HttpServletRequest http
    ) {
        return new ContactInquiryResponse(contactInquiryService.submit(
                request.name(),
                request.email(),
                request.topic(),
                request.message(),
                clientIp(http)
        ));
    }

    @GetMapping("/access")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Whether the current user can open the staff contact inbox")
    public ContactInboxAccessResponse access(@AuthenticationPrincipal AuthenticatedUser user) {
        boolean canAccess = contactInquiryService.canAccessInbox(user.getUsername());
        long unread = canAccess ? contactInquiryService.unreadCountForStaff(user.getUsername()) : 0L;
        return new ContactInboxAccessResponse(canAccess, unread);
    }

    @GetMapping("/inquiries")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List contact inquiries (Deshmukh Technology staff)")
    public List<ContactInquiryListItemResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return contactInquiryService.listForStaff(user.getUsername());
    }

    @PostMapping("/inquiries/{id}/read")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mark a contact inquiry as read (Deshmukh Technology staff)")
    public ContactInquiryListItemResponse markRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id
    ) {
        return contactInquiryService.markRead(user.getUsername(), id);
    }

    @PostMapping("/inquiries/read-all")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mark all contact inquiries as read (Deshmukh Technology staff)")
    public MarkAllReadResponse markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        return new MarkAllReadResponse(contactInquiryService.markAllRead(user.getUsername()));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
