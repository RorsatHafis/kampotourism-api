package com.kampotourism.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.kampotourism.content.ContentDtos.CreateRequest;
import com.kampotourism.content.ContentDtos.TranslationBody;
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

/** Verifies the shared lifecycle/search/fallback contract used by all new content modules. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ContentApiTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ContentService content;

    @Test
    void publicEndpointHidesDraftThenServesPublishedContentWithLanguageFallback() throws Exception {
        var json = JsonNodeFactory.instance;
        var created = content.create(ContentType.ARTICLE, new CreateRequest(
                "testing-kampot-content", null, true, 0, null, null, null, null, null,
                null, null, null, null, null, null, null, null, json.objectNode(),
                new TranslationBody("Testing Kampot Content", "A test article", json.arrayNode().add(json.objectNode().put("type", "paragraph")),
                        json.arrayNode(), json.arrayNode(), null, null, true)));

        mvc.perform(get("/api/v1/articles/testing-kampot-content"))
                .andExpect(status().isNotFound());

        content.lifecycle(ContentType.ARTICLE, created.id(), ContentStatus.PUBLISHED);

        mvc.perform(get("/api/v1/articles").param("q", "testing kampot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("testing-kampot-content"));
        mvc.perform(get("/api/v1/articles/testing-kampot-content").param("lang", "km"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.title").value("Testing Kampot Content"));
    }
}
