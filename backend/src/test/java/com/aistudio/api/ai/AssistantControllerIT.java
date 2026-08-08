package com.aistudio.api.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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
class AssistantControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void listAssistantsAndChatAcrossMultipleThreads() throws Exception {
        mockMvc.perform(get("/api/v1/assistants")
                        .header("Authorization", "Bearer " + registerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistants.length()").value(4));

        JsonNode auth = register("chat" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Chat Proj","projectKey":"CH","description":"chat"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Auth","description":"Login and JWT"}
                                """))
                .andExpect(status().isCreated());

        UUID thread1 = createThread(token, projectId, "DEVELOPER", "API design");
        UUID thread2 = createThread(token, projectId, "DEVELOPER", "Password reset");

        mockMvc.perform(post("/api/v1/conversations/" + thread1 + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Suggest a REST API for password reset"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.userMessage.sender").value("USER"))
                .andExpect(jsonPath("$.assistantMessage.sender").value("ASSISTANT"));

        mockMvc.perform(post("/api/v1/conversations/" + thread2 + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Outline auth middleware"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations")
                        .param("assistantRole", "DEVELOPER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/conversations/" + thread1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations")
                        .param("assistantRole", "DEVELOPER")
                        .param("q", "middleware")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Password reset"));

        mockMvc.perform(delete("/api/v1/conversations/" + thread2)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations")
                        .param("assistantRole", "DEVELOPER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void streamChatMessageReturnsSseEvents() throws Exception {
        JsonNode auth = register("stream" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Stream Proj","projectKey":"ST","description":"stream"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID conversationId = createThread(token, projectId, "DEVELOPER", null);

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"content":"Say hello in one sentence"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult(60_000);
        String body = mvcResult.getResponse().getContentAsString();
        Assertions.assertTrue(body.contains("event:user"), body);
        Assertions.assertTrue(body.contains("event:delta"), body);
        Assertions.assertTrue(body.contains("event:done"), body);

        mockMvc.perform(get("/api/v1/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2));
    }

    @Test
    void sharedConversationReadOnlyLink() throws Exception {
        JsonNode auth = register("share" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Share Proj","projectKey":"SH","description":"share"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID threadId = createThread(token, projectId, "DEVELOPER", "Share me");

        mockMvc.perform(post("/api/v1/conversations/" + threadId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Hello shared world"}
                                """))
                .andExpect(status().isOk());

        JsonNode share = objectMapper.readTree(mockMvc.perform(post("/api/v1/conversations/" + threadId + "/share")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareEnabled").value(true))
                .andExpect(jsonPath("$.shareUrl").exists())
                .andReturn().getResponse().getContentAsString());

        String rawToken = share.get("token").asText();

        mockMvc.perform(get("/api/v1/shared/conversations/" + rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Share me"))
                .andExpect(jsonPath("$.assistantRole").value("DEVELOPER"))
                .andExpect(jsonPath("$.messages.length()").value(2));

        mockMvc.perform(delete("/api/v1/conversations/" + threadId + "/share")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/shared/conversations/" + rawToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void privateThreadsVisibleOnlyToCreator() throws Exception {
        JsonNode owner = register("owner" + System.currentTimeMillis() + "@example.com");
        String ownerToken = owner.get("accessToken").asText();
        UUID orgId = UUID.fromString(owner.get("organization").get("id").asText());

        JsonNode member = register("member" + System.currentTimeMillis() + "@example.com");
        String memberToken = member.get("accessToken").asText();
        String memberEmail = member.get("user").get("email").asText();

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","role":"MEMBER"}
                                """.formatted(memberEmail)))
                .andExpect(status().isCreated());

        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Private Proj","projectKey":"PR","description":"private"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID privateThread = createThread(ownerToken, projectId, "DEVELOPER", "Secret notes", "PRIVATE");
        UUID projectThread = createThread(ownerToken, projectId, "DEVELOPER", "Team thread", "PROJECT");

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations")
                        .param("assistantRole", "DEVELOPER")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations")
                        .param("assistantRole", "DEVELOPER")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(projectThread.toString()));

        mockMvc.perform(get("/api/v1/conversations/" + privateThread)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/conversations/" + privateThread)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Secret notes"));
    }

    @Test
    void exportConversationAsMarkdownAndJson() throws Exception {
        JsonNode auth = register("export" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Export Proj","projectKey":"EX","description":"export"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID threadId = createThread(token, projectId, "DEVELOPER", "API design");

        mockMvc.perform(post("/api/v1/conversations/" + threadId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Hello export"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/conversations/" + threadId + "/export")
                        .param("format", "markdown")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".md")))
                .andExpect(content().string(containsString("# API design")))
                .andExpect(content().string(containsString("Hello export")));

        mockMvc.perform(get("/api/v1/conversations/" + threadId + "/export")
                        .param("format", "json")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"assistantRole\" : \"DEVELOPER\"")))
                .andExpect(content().string(containsString("Hello export")));

        mockMvc.perform(get("/api/v1/conversations/" + threadId + "/export")
                        .param("format", "pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportProjectConversationsArchive() throws Exception {
        JsonNode auth = register("bulkexport" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bulk Export Proj","projectKey":"BE","description":"bulk"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID thread1 = createThread(token, projectId, "DEVELOPER", "API design");
        UUID thread2 = createThread(token, projectId, "DEVELOPER", "Password reset");

        mockMvc.perform(post("/api/v1/conversations/" + thread1 + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Bulk message one"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/conversations/" + thread2 + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Bulk message two"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations/export")
                        .param("format", "json")
                        .param("assistantRole", "DEVELOPER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("BE-threads")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"conversationCount\" : 2")))
                .andExpect(content().string(containsString("Bulk message one")))
                .andExpect(content().string(containsString("Bulk message two")));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations/export")
                        .param("format", "markdown")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("# Project archive: Bulk Export Proj")))
                .andExpect(content().string(containsString("# API design")))
                .andExpect(content().string(containsString("# Password reset")));
    }

    private UUID createThread(String token, UUID projectId, String role, String title) throws Exception {
        return createThread(token, projectId, role, title, null);
    }

    private UUID createThread(
            String token,
            UUID projectId,
            String role,
            String title,
            String visibility
    ) throws Exception {
        String payload;
        if (title == null) {
            payload = visibility == null
                    ? "{\"assistantRole\":\"%s\"}".formatted(role)
                    : "{\"assistantRole\":\"%s\",\"visibility\":\"%s\"}".formatted(role, visibility);
        } else {
            payload = visibility == null
                    ? "{\"assistantRole\":\"%s\",\"title\":\"%s\"}".formatted(role, title)
                    : "{\"assistantRole\":\"%s\",\"title\":\"%s\",\"visibility\":\"%s\"}".formatted(role, title, visibility);
        }
        MvcResult result = mockMvc.perform(post("/api/v1/projects/" + projectId + "/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private String registerToken() throws Exception {
        return register("assist" + System.currentTimeMillis() + "@example.com").get("accessToken").asText();
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"Chat User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
