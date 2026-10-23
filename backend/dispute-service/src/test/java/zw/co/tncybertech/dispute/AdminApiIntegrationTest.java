package zw.co.tncybertech.dispute;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Value("${app.jwt.secret}") String secret;
  @Value("${app.jwt.issuer}") String issuer;

  private String bearer(String role) {
    return "Bearer " + Jwts.builder().subject("101").issuer(issuer)
        .claim("role", role).claim("email", "api-test@casepilot.local").claim("name", "API Test")
        .expiration(Date.from(Instant.now().plusSeconds(120)))
        .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
  }

  @Test
  void adminReadEndpointsReturnData() throws Exception {
    String auth = bearer("ADMIN");
    for (String endpoint : new String[]{"/admin/training/policies", "/admin/training/quizzes", "/admin/sla",
        "/admin/audit", "/training/policies", "/training/quizzes", "/training/metrics", "/training/attempts/me",
        "/metrics/dashboard", "/metrics/bank", "/notifications", "/disputes/tasks", "/disputes/queue"}) {
      mvc.perform(get("/api/v1" + endpoint).header("Authorization", auth)).andExpect(status().isOk());
    }
    for (String endpoint : new String[]{"/metrics/bank/export", "/metrics/dashboard/export", "/training/metrics/export"}) {
      mvc.perform(get("/api/v1" + endpoint).header("Authorization", auth))
          .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"));
    }
  }

  @Test
  void policiesAndQuizzesCanBeCreatedAndListed() throws Exception {
    String auth = bearer("ADMIN");
    String policy = mvc.perform(post("/api/v1/admin/training/policies").header("Authorization", auth)
        .contentType(MediaType.APPLICATION_JSON).content("""
          {"title":"API Test Policy","circularReference":"TEST-001","summary":"Test summary",
           "effectiveDate":"2026-01-01","keyRequirements":"Verify evidence","active":true}
          """)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    long id = json.readTree(policy).get("id").asLong();
    mvc.perform(post("/api/v1/admin/training/quizzes").header("Authorization", auth)
        .contentType(MediaType.APPLICATION_JSON).content("""
          {"policyId":%d,"title":"API Test Quiz","description":"Test assessment",
           "questions":[{"question":"Which action is required?","explanation":"Check evidence",
             "options":[{"text":"Check evidence","correct":true},{"text":"Skip review","correct":false}]}]}
          """.formatted(id))).andExpect(status().isOk());
    mvc.perform(get("/api/v1/admin/training/policies").header("Authorization", auth))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("API Test Policy"));
    mvc.perform(get("/api/v1/admin/training/quizzes").header("Authorization", auth))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].questions[0].options.length()").value(2));
  }

  @Test
  void customerCannotReadAdminPolicies() throws Exception {
    mvc.perform(get("/api/v1/admin/training/policies").header("Authorization", bearer("CUSTOMER")))
        .andExpect(status().isForbidden());
  }
}
