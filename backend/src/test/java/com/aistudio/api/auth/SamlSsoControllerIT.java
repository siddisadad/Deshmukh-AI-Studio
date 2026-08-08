package com.aistudio.api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aistudio.support.IntegrationTestProperties;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SamlSsoControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
        registry.add("aistudio.sso.provider", () -> "saml");
        registry.add("aistudio.sso.saml.stub-mode", () -> "true");
    }

    @Test
    void samlStubStartAndCallbackCreatesSession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/sso/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("saml"));

        String email = "saml" + System.currentTimeMillis() + "@example.com";
        MvcResult startResult = mockMvc.perform(post("/api/v1/auth/sso/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"saml","redirectUri":"http://localhost:5173/auth/sso/callback","loginHint":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("saml"))
                .andExpect(jsonPath("$.authorizationUrl").isNotEmpty())
                .andReturn();

        JsonNode start = objectMapper.readTree(startResult.getResponse().getContentAsString());
        Map<String, String> params = queryParams(start.get("authorizationUrl").asText());

        mockMvc.perform(post("/api/v1/auth/sso/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"saml","code":"%s","state":"%s","redirectUri":"http://localhost:5173/auth/sso/callback"}
                                """.formatted(params.get("code"), params.get("state"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
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
