package com.example.demo.integration;

import com.example.demo.model.User;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Baza testów integracyjnych: pełna aplikacja Spring Boot + baza H2 w pamięci + MockMvc.
 * Przed każdym testem baza jest czyszczona i zakładane jest konto administratora.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    static final String ADMIN_EMAIL = "admin@test.pl";
    static final String ADMIN_PASSWORD = "admin123";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired MembershipPlanRepository planRepository;
    @Autowired MembershipRepository membershipRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired GymClassRepository classRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired CheckInRepository checkInRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        checkInRepository.deleteAll();
        bookingRepository.deleteAll();
        paymentRepository.deleteAll();
        membershipRepository.deleteAll();
        classRepository.deleteAll();
        planRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(User.builder().email(ADMIN_EMAIL).password(passwordEncoder.encode(ADMIN_PASSWORD))
                .firstName("Admin").lastName("Testowy").role(Role.ADMIN).active(true).build());
    }

    String login(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /** Rejestruje klubowicza i zwraca jego token. */
    String registerMember(String email) throws Exception {
        String body = """
                {"email": "%s", "password": "haslo123", "firstName": "Jan", "lastName": "Testowy"}
                """.formatted(email);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }

    static long idFrom(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
