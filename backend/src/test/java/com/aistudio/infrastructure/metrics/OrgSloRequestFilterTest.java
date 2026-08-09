package com.aistudio.infrastructure.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

@ExtendWith(MockitoExtension.class)
class OrgSloRequestFilterTest {

    @Mock OrgSloProjectResolver projectResolver;
    @InjectMocks OrgSloRequestFilter filter;

    @AfterEach
    void tearDown() {
        OrgSloContext.clear();
    }

    @Test
    void extractsOrganizationFromOrgScopedPath() throws Exception {
        UUID orgId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/organizations/" + orgId + "/billing");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(OrgSloContext.organizationId()).isEqualTo(orgId.toString());
        });
    }

    @Test
    void resolvesOrganizationFromProjectScopedPath() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(projectResolver.resolveOrganizationId(projectId)).thenReturn(Optional.of(orgId));

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/projects/" + projectId + "/conversations");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(OrgSloContext.organizationId()).isEqualTo(orgId.toString());
        });
    }

    @Test
    void clearsContextAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> assertThat(OrgSloContext.organizationId()).isNull());
        assertThat(OrgSloContext.organizationId()).isNull();
    }
}
