package com.kampotourism.destination;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Public read-only API, anonymous access. Runs the real Flyway migrations (schema + seed)
 * against a throwaway PostgreSQL container. Requires Docker to be running.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class DestinationApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Test
    void listsSeededDestinationsInDisplayOrderWithoutLogin() throws Exception {
        mvc.perform(get("/api/v1/destinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(6))
                .andExpect(jsonPath("$.totalItems").value(6))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("bokor"))
                .andExpect(jsonPath("$.items[0].category.slug").value("nature"))
                .andExpect(jsonPath("$.items[0].category.name").value("Nature"));
    }

    @Test
    void filtersByCategorySlugIgnoringCase() throws Exception {
        mvc.perform(get("/api/v1/destinations").param("category", "WATER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].slug").value("mangroves"));
    }

    @Test
    void paginatesWithOneBasedPages() throws Exception {
        mvc.perform(get("/api/v1/destinations").param("size", "4").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void featuredFilterReturnsHomePageSelection() throws Exception {
        mvc.perform(get("/api/v1/destinations").param("featured", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void rejectsUnknownCategory() throws Exception {
        mvc.perform(get("/api/v1/destinations").param("category", "beach"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void categoriesEndpointListsOneKind() throws Exception {
        mvc.perform(get("/api/v1/categories").param("kind", "destination"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].slug").value("nature"))
                .andExpect(jsonPath("$[0].name").value("Nature"))
                .andExpect(jsonPath("$[0].kind").value("destination"));
    }

    @Test
    void detailIncludesFactsHighlightsTipsAndRelated() throws Exception {
        mvc.perform(get("/api/v1/destinations/bokor"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en"))
                .andExpect(jsonPath("$.name").value("Bokor National Park"))
                .andExpect(jsonPath("$.facts.distance").value("40 km from town"))
                .andExpect(jsonPath("$.highlights.length()").value(3))
                .andExpect(jsonPath("$.highlights[0].title").value("The old hill station"))
                .andExpect(jsonPath("$.tips.length()").value(3))
                .andExpect(jsonPath("$.related.length()").value(2))
                .andExpect(jsonPath("$.cover.url").exists())
                .andExpect(jsonPath("$.gallery.length()").value(0));
    }

    @Test
    void missingTranslationFallsBackToEnglish() throws Exception {
        mvc.perform(get("/api/v1/destinations/bokor").param("lang", "km"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.name").value("Bokor National Park"));
    }

    @Test
    void unknownSlugIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/destinations/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
