package com.aistudio.infrastructure.billing;

import com.aistudio.application.billing.BillingPort;
import com.aistudio.domain.billing.PlanCode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dev/CI billing adapter. Swap for StripeBillingAdapter when keys are configured.
 */
@Component
public class MockBillingAdapter implements BillingPort {

    private final String appBaseUrl;

    public MockBillingAdapter(@Value("${aistudio.billing.app-base-url:http://localhost:5173}") String appBaseUrl) {
        this.appBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
    }

    @Override
    public String providerId() {
        return "mock";
    }

    @Override
    public CheckoutSession createCheckoutSession(
            UUID organizationId,
            PlanCode planCode,
            String successUrl,
            String cancelUrl
    ) {
        String sessionId = "mock_cs_" + organizationId.toString().replace("-", "").substring(0, 12);
        String url = (successUrl == null || successUrl.isBlank() ? appBaseUrl + "/settings/billing" : successUrl)
                + (successUrl != null && successUrl.contains("?") ? "&" : "?")
                + "mockCheckout=1&plan=" + planCode.name() + "&sessionId=" + sessionId;
        return new CheckoutSession(sessionId, url);
    }

    @Override
    public String createCustomerPortalUrl(UUID organizationId, String returnUrl) {
        String base = returnUrl == null || returnUrl.isBlank() ? appBaseUrl + "/settings/billing" : returnUrl;
        return base + (base.contains("?") ? "&" : "?") + "mockPortal=1&org=" + organizationId;
    }
}
