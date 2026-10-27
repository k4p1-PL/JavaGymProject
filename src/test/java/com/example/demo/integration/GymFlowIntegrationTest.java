package com.example.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pełny scenariusz: admin tworzy ofertę i trenera, trener dodaje zajęcia, klubowicz kupuje karnet i się zapisuje. */
class GymFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void fullMembershipAndBookingFlow() throws Exception {
        String admin = adminToken();

        // 1. Admin dodaje rodzaj karnetu (CRUD – create)
        String planJson = mockMvc.perform(post("/api/plans").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "Open testowy", "durationDays": 30, "price": 149.00,
                                 "includesClasses": true, "active": true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long planId = idFrom(planJson);

        // 2. Admin zakłada konto trenera
        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email": "trener@test.pl", "password": "trener123", "firstName": "Tomasz",
                                 "lastName": "Trener", "role": "TRAINER"}
                                """))
                .andExpect(status().isCreated());
        String trainer = login("trener@test.pl", "trener123");

        // 3. Trener dodaje zajęcia (jedno miejsce)
        LocalDateTime start = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.HOURS);
        String classJson = mockMvc.perform(post("/api/classes").header(HttpHeaders.AUTHORIZATION, bearer(trainer))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "Joga", "startTime": "%s", "endTime": "%s", "capacity": 1, "room": "Sala 2"}
                                """.formatted(start, start.plusHours(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trainerName").value("Tomasz Trener"))
                .andReturn().getResponse().getContentAsString();
        long classId = idFrom(classJson);

        // 4. Klubowicz bez karnetu nie może się zapisać
        String member = registerMember("jan@test.pl");
        mockMvc.perform(post("/api/bookings").header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gymClassId\": " + classId + "}"))
                .andExpect(status().isConflict());

        // 5. Klubowicz kupuje karnet
        mockMvc.perform(post("/api/memberships/purchase").header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"planId": %d, "paymentMethod": "CARD"}
                                """.formatted(planId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.validToday").value(true));

        // 6. ...i zapisuje się na zajęcia
        mockMvc.perform(post("/api/bookings").header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gymClassId\": " + classId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));

        // 7. Drugi klubowicz z karnetem – brak miejsc
        String member2 = registerMember("ola@test.pl");
        mockMvc.perform(post("/api/memberships/purchase").header(HttpHeaders.AUTHORIZATION, bearer(member2))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"planId\": " + planId + ", \"paymentMethod\": \"CASH\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/bookings").header(HttpHeaders.AUTHORIZATION, bearer(member2))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gymClassId\": " + classId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Brak wolnych miejsc")));

        // 8. Trener widzi listę uczestników
        mockMvc.perform(get("/api/classes/" + classId + "/bookings").header(HttpHeaders.AUTHORIZATION, bearer(trainer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // 9. Recepcja rejestruje wejście klubowicza
        mockMvc.perform(post("/api/checkins").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\": \"jan@test.pl\"}"))
                .andExpect(status().isCreated());

        // 10. Statystyki admina
        mockMvc.perform(get("/api/stats").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeMembers").value(2))
                .andExpect(jsonPath("$.activeMemberships").value(2))
                .andExpect(jsonPath("$.currentlyInGym").value(1));
    }

    @Test
    void planCrudUpdateAndDelete() throws Exception {
        String admin = adminToken();
        String json = mockMvc.perform(post("/api/plans").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "Do usunięcia", "durationDays": 7, "price": 30, "includesClasses": false, "active": true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = idFrom(json);

        mockMvc.perform(put("/api/plans/" + id).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "Tygodniowy", "durationDays": 7, "price": 35, "includesClasses": false, "active": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tygodniowy"));

        mockMvc.perform(delete("/api/plans/" + id).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/plans/" + id))
                .andExpect(status().isNotFound());
    }
}
