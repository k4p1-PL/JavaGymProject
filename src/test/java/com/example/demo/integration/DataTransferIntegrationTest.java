package com.example.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DataTransferIntegrationTest extends IntegrationTestBase {

    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gymData>
                <plans>
                    <plan>
                        <name>Karnet z XML</name>
                        <durationDays>30</durationDays>
                        <price>120.00</price>
                        <includesClasses>true</includesClasses>
                        <active>true</active>
                    </plan>
                </plans>
                <classes/>
            </gymData>
            """;

    @Test
    void importXmlThenExportJsonAndXml() throws Exception {
        String admin = adminToken();

        mockMvc.perform(post("/api/transfer/import").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_XML).content(XML))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plansCreated").value(1))
                .andExpect(jsonPath("$.errors").isEmpty());

        mockMvc.perform(get("/api/transfer/export").param("format", "json").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("gym-data.json")))
                .andExpect(jsonPath("$.plans[0].name").value("Karnet z XML"));

        mockMvc.perform(get("/api/transfer/export").param("format", "xml").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<name>Karnet z XML</name>")));
    }

    @Test
    void importJsonUpdatesExistingPlanByName() throws Exception {
        String admin = adminToken();
        String json = """
                {"plans": [{"name": "Open", "durationDays": 30, "price": 100, "includesClasses": true, "active": true}],
                 "classes": []}
                """;
        mockMvc.perform(post("/api/transfer/import").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(jsonPath("$.plansCreated").value(1));
        mockMvc.perform(post("/api/transfer/import").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json.replace("100", "110")))
                .andExpect(jsonPath("$.plansCreated").value(0))
                .andExpect(jsonPath("$.plansUpdated").value(1));
    }

    @Test
    void malformedXmlReturns400() throws Exception {
        mockMvc.perform(post("/api/transfer/import").header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_XML).content("<gymData><plans>"))
                .andExpect(status().isBadRequest());
    }
}
