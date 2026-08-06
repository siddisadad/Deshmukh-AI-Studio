package com.aistudio.api.plugin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import com.aistudio.support.IntegrationTestProperties;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PluginControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void catalogOrgToggleAndInvokeTool() throws Exception {
        String email = "plug" + System.currentTimeMillis() + "@example.com";
        JsonNode user = register(email, "Plugin User");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        mockMvc.perform(get("/api/v1/plugins").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='core.assistant.business_analyst')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id=='sample.tool.echo')]").isNotEmpty());

        mockMvc.perform(get("/api/v1/assistants").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistants[0].pluginId").isNotEmpty())
                .andExpect(jsonPath("$.assistants[0].tools").isArray());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/plugins")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.plugin.id=='sample.tool.echo')].enabled").value(org.hamcrest.Matchers.contains(true)))
                .andExpect(jsonPath("$[?(@.plugin.id=='sample.tool.echo')].canDisable").value(org.hamcrest.Matchers.contains(true)));

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/plugins/core.assistant.business_analyst")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/plugins/sample.tool.echo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        MvcResult projectResult = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Plugin Demo","projectKey":"PLG","description":"spi"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID projectId = UUID.fromString(objectMapper.readTree(projectResult.getResponse().getContentAsString())
                .get("id").asText());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/tools/sample.tool.echo/invoke")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"arguments":{"message":"hello"}}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/plugins/sample.tool.echo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/tools/sample.tool.echo/invoke")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"arguments":{"message":"hello"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.output").value("echo: hello"));

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/tools/core.tool.project_snapshot/invoke")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"arguments":{"query":"overview"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.output").isNotEmpty());
    }

    private JsonNode register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"%s"}
                                """.formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
