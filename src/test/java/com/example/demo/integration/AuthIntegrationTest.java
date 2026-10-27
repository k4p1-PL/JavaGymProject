package com.example.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void registeredMemberCanLogInAndReadOwnProfile() throws Exception {
        registerMember("nowy@test.pl");
        String token = login("nowy@test.pl", "haslo123");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nowy@test.pl"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        registerMember("dubel@test.pl");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "dubel@test.pl", "password": "haslo123", "firstName": "A", "lastName": "B"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationDataReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "to-nie-email", "password": "123", "firstName": "", "lastName": "B"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "admin@test.pl", "password": "zle-haslo"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCannotAccessAdminEndpoints() throws Exception {
        String token = registerMember("klient@test.pl");

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/transfer/export").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void plansAreVisibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isOk());
    }
}
