package com.aistudio.api.contact;

import com.aistudio.api.contact.dto.ContactInquiryResponse;
import com.aistudio.api.contact.dto.CreateContactInquiryRequest;
import com.aistudio.application.contact.ContactInquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
