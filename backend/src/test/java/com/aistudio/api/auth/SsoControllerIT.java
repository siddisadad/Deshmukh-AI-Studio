package com.aistudio.api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
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
class SsoControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void listProvidersStartAndCallbackCreatesSession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/sso/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("mock"));

        String email = "sso" + System.currentTimeMillis() + "@example.com";
        MvcResult startResult = mockMvc.perform(post("/api/v1/auth/sso/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"mock","redirectUri":"http://localhost:5173/auth/sso/callback","loginHint":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.authorizationUrl").isNotEmpty())
                .andExpect(jsonPath("$.state").isNotEmpty())
                .andReturn();

        JsonNode start = objectMapper.readTree(startResult.getResponse().getContentAsString());
        Map<String, String> params = queryParams(start.get("authorizationUrl").asText());

        MvcResult callbackResult = mockMvc.perform(post("/api/v1/auth/sso/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"mock","code":"%s","state":"%s","redirectUri":"http://localhost:5173/auth/sso/callback"}
                                """.formatted(params.get("code"), params.get("state"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.organization.id").isNotEmpty())
                .andReturn();

        String accessToken = objectMapper.readTree(callbackResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // Second SSO login links to same account
        MvcResult start2 = mockMvc.perform(post("/api/v1/auth/sso/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"mock","redirectUri":"http://localhost:5173/auth/sso/callback","loginHint":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode startJson2 = objectMapper.readTree(start2.getResponse().getContentAsString());
        Map<String, String> params2 = queryParams(startJson2.get("authorizationUrl").asText());

        mockMvc.perform(post("/api/v1/auth/sso/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"mock","code":"%s","state":"%s","redirectUri":"http://localhost:5173/auth/sso/callback"}
                                """.formatted(params2.get("code"), params2.get("state"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Anything1!"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SSO")));
    }

    private static Map<String, String> queryParams(String url) {
        URI uri = URI.create(url);
        Map<String, String> map = new LinkedHashMap<>();
        String query = uri.getRawQuery();
        if (query == null) {
            return map;
        }
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }
}
