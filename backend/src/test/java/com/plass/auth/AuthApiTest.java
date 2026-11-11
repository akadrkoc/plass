package com.plass.auth;

import java.util.Locale;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.plass.support.IntegrationTest;
import com.plass.support.TestDataFactory;
import com.plass.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends IntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void registeringReturnsATokenAndTheNewUser() throws Exception {
        String email = "new-" + UUID.randomUUID() + "@test.dev";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName": "Ada Lovelace", "email": "%s", "password": "Secret123"}
                        """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void anEmailCanOnlyBeRegisteredOnce() throws Exception {
        User existing = data.user();

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName": "Someone", "email": "%s", "password": "Secret123"}
                        """.formatted(existing.getEmail().toUpperCase(Locale.ROOT))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void invalidRegistrationsReturnFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName": "", "email": "not-an-email", "password": "short"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                // The failed constraint per field lets the frontend translate the message.
                .andExpect(jsonPath("$.errorCodes.fullName").value("NotBlank"))
                .andExpect(jsonPath("$.errorCodes.email").value("Email"))
                .andExpect(jsonPath("$.errorCodes.password").exists());
    }

    @Test
    void loginReturnsATokenThatWorksForProtectedEndpoints() throws Exception {
        User user = data.user();

        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(user.getEmail(), TestDataFactory.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.token");

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void wrongPasswordsAreRejected() throws Exception {
        User user = data.user();

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "wrong-password1"}
                        """.formatted(user.getEmail())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsRequireTheAdminRole() throws Exception {
        mvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, data.bearerToken(data.user())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, data.bearerToken(data.admin())))
                .andExpect(status().isOk());
    }
}
