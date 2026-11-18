package com.kampotourism.settings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.kampotourism.content.ContentStatus;
import com.kampotourism.itinerary.ItineraryDtos.ActivityBody;
import com.kampotourism.itinerary.ItineraryDtos.CreateRequest;
import com.kampotourism.itinerary.ItineraryDtos.DayBody;
import com.kampotourism.itinerary.ItineraryDtos.TranslationBody;
import com.kampotourism.itinerary.ItineraryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Regression: requesting the default language itself (no lang, ?lang=en, Accept-Language: en)
 * used to fail with 500 because the requested and default language were put into Set.of(...)
 * twice ("duplicate element: en").
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class DefaultLanguageApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ItineraryService itineraries;
    @Autowired JdbcTemplate jdbc;

    @Test
    void homepageServesTheDefaultLanguage() throws Exception {
        mvc.perform(get("/api/v1/homepage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"));
        mvc.perform(get("/api/v1/homepage").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"));
        mvc.perform(get("/api/v1/homepage").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/homepage").param("lang", "km"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("km"));
    }

    @Test
    void publishedItineraryIsListedInTheDefaultLanguage() throws Exception {
        long destinationId = jdbc.queryForObject("select id from destination where slug = 'bokor'", Long.class);
        var json = JsonNodeFactory.instance;
        var created = itineraries.create(new CreateRequest("default-language-trip", false, 0,
                List.of(new DayBody(List.of(new ActivityBody(destinationId, null)))),
                new TranslationBody("Default language trip", json.arrayNode(), null, null)));
        itineraries.status(created.id(), ContentStatus.PUBLISHED);

        mvc.perform(get("/api/v1/itineraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("default-language-trip"));
        mvc.perform(get("/api/v1/itineraries").param("lang", "en"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/itineraries").param("lang", "km"))
                .andExpect(status().isOk());
    }
}
