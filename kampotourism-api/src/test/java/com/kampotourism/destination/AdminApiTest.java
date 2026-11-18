package com.kampotourism.destination;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/**
 * Auth, categories, media and destination management end to end.
 * The bootstrap admin comes from src/test/resources/application-test.yml. Requires Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AdminApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static byte[] validPng() throws Exception {
        BufferedImage image = new BufferedImage(
                1,
                1,
                BufferedImage.TYPE_INT_RGB
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        if (!ImageIO.write(image, "png", output)) {
            throw new IllegalStateException("PNG writer is unavailable");
        }

        return output.toByteArray();
    }

    @Autowired
    MockMvc mvc;

    // ------------------------------------------------------------------ auth

    @Test
    void adminAndAuthEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/v1/admin/destinations")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicApiCannotBeWrittenTo() throws Exception {
        mvc.perform(post("/api/v1/destinations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.local\",\"password\":\"definitely-wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsTheSignedInUser() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(login())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.local"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        String token = login();
        mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ categories

    @Test
    void categoryCanBeCreatedTranslatedAndDeleted() throws Exception {
        String token = login();
        String created = mvc.perform(post("/api/v1/admin/categories").headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"destination","slug":"heritage","sortOrder":9,
                                 "translation":{"name":"Heritage","description":"Old buildings and stories"}}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = idOf(created);

        mvc.perform(put("/api/v1/admin/categories/" + id + "/translations/km").headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"បេតិកភណ្ឌ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translations.km.name").value("បេតិកភណ្ឌ"));

        mvc.perform(get("/api/v1/categories").param("kind", "destination").param("lang", "km"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug=='heritage')].name").value("បេតិកភណ្ឌ"));

        mvc.perform(delete("/api/v1/admin/categories/" + id).headers(auth(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void categoryInUseCannotBeDeleted() throws Exception {
        String token = login();
        long natureId = categoryId(token, "nature");
        mvc.perform(delete("/api/v1/admin/categories/" + natureId).headers(auth(token)))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ destinations

    @Test
    void destinationLifecycleFromDraftToPublishedToDeleted() throws Exception {
        String token = login();
        long categoryId = categoryId(token, "culture");

        String created = mvc.perform(post("/api/v1/admin/destinations").headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(destinationJson("lifecycle-place", categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = idOf(created);

        // drafts are invisible to visitors
        mvc.perform(get("/api/v1/destinations/lifecycle-place")).andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/admin/destinations/" + id + "/publish").headers(auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
        mvc.perform(get("/api/v1/destinations/lifecycle-place"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lifecycle Place"))
                .andExpect(jsonPath("$.category.slug").value("culture"));

        // add a Khmer translation: visitors asking for km now get it
        mvc.perform(put("/api/v1/admin/destinations/" + id + "/translations/km").headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON).content(translationJson("ទីតាំងសាកល្បង")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/destinations/lifecycle-place").param("lang", "km"))
                .andExpect(jsonPath("$.language").value("km"))
                .andExpect(jsonPath("$.name").value("ទីតាំងសាកល្បង"));

        // publishing bumped the version, so an update carrying the old one is a conflict
        mvc.perform(put("/api/v1/admin/destinations/" + id).headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON).content(updateJson("lifecycle-place", categoryId, 0)))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/admin/destinations/" + id).headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON).content(updateJson("lifecycle-place", categoryId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(true));

        // a live destination cannot be deleted
        mvc.perform(delete("/api/v1/admin/destinations/" + id).headers(auth(token)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/admin/destinations/" + id + "/unpublish").headers(auth(token)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/admin/destinations/" + id).headers(auth(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/destinations/lifecycle-place")).andExpect(status().isNotFound());
    }

    @Test
    void validationErrorsNameTheFields() throws Exception {
        String token = login();
        mvc.perform(post("/api/v1/admin/destinations").headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"Bad Slug\",\"categoryId\":1,\"sortOrder\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.slug").exists())
                .andExpect(jsonPath("$.errors.translation").exists());
    }

    @Test
    void duplicateSlugIsAConflict() throws Exception {
        String token = login();
        long categoryId = categoryId(token, "nature");
        mvc.perform(post("/api/v1/admin/destinations").headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON).content(destinationJson("bokor", categoryId)))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ images

    @Test
    void imagesCoverGalleryAltTextAndDeletion() throws Exception {
        String token = login();
        long categoryId = categoryId(token, "culture");
        long destinationId = idOf(mvc.perform(post("/api/v1/admin/destinations").headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON).content(destinationJson("image-place", categoryId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/v1/admin/destinations/" + destinationId + "/publish").headers(auth(token)))
                .andExpect(status().isOk());

        // only real images are accepted
        mvc.perform(multipart("/api/v1/admin/media")
                        .file(new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes()))
                        .headers(auth(token)))
                .andExpect(status().isBadRequest());

        long cover = upload(token, "cover.png", "Sunrise over the river", "Taken at 6am");
        long second = upload(token, "second.png", "Salt pans at dusk", null);
        long noAlt = upload(token, "noalt.png", null, null);

        // alt text is mandatory before an image can be used
        mvc.perform(put("/api/v1/admin/destinations/" + destinationId + "/images").headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cover\":%d,\"gallery\":[%d]}".formatted(cover, noAlt)))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/admin/destinations/" + destinationId + "/images").headers(auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cover\":%d,\"gallery\":[%d]}".formatted(cover, second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.cover.mediaId").value(cover))
                .andExpect(jsonPath("$.images.gallery.length()").value(1));

        mvc.perform(get("/api/v1/destinations/image-place"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cover.alt").value("Sunrise over the river"))
                .andExpect(jsonPath("$.cover.caption").value("Taken at 6am"))
                .andExpect(jsonPath("$.cover.url").value(org.hamcrest.Matchers.startsWith("http://localhost:8080/media/")))
                .andExpect(jsonPath("$.gallery[0].alt").value("Salt pans at dusk"));

        // an image that is in use is protected unless the editor insists
        mvc.perform(delete("/api/v1/admin/media/" + cover).headers(auth(token)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/admin/media/" + cover).param("force", "true").headers(auth(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/destinations/image-place"))
                .andExpect(jsonPath("$.gallery.length()").value(1));
        mvc.perform(get("/api/v1/admin/media/" + cover).headers(auth(token)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ helpers

    private String login() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.local\",\"password\":\"correct-horse-battery-staple\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static HttpHeaders auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long categoryId(String token, String slug) throws Exception {
        String body = mvc.perform(get("/api/v1/admin/categories").param("kind", "destination").headers(auth(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        java.util.List<Number> ids = JsonPath.read(body, "$[?(@.slug=='" + slug + "')].id");
        return ids.get(0).longValue();
    }

    private long upload(String token, String filename, String alt, String caption) throws Exception {
        var request = multipart("/api/v1/admin/media")
                .file(new MockMultipartFile("file", filename, "image/png", validPng()))
                .headers(auth(token));
        if (alt != null) {
            request.param("alt", alt);
        }
        if (caption != null) {
            request.param("caption", caption);
        }
        ResultActions result = mvc.perform(request).andExpect(status().isCreated());
        return idOf(result.andReturn().getResponse().getContentAsString());
    }

    private static String translationJson(String name) {
        return """
                {"name":"%s","tag":"a tag","summary":"A short summary","lead":"A longer lead sentence.",
                 "description":["First paragraph"],"highlights":[{"title":"One","text":"Highlight text"}],
                 "tips":["A tip"]}
                """.formatted(name);
    }

    private static String destinationJson(String slug, long categoryId) {
        return """
                {"slug":"%s","categoryId":%d,"featured":false,"sortOrder":99,
                 "translation":%s}
                """.formatted(slug, categoryId, translationJson("Lifecycle Place"));
    }

    private static String updateJson(String slug, long categoryId, long version) {
        return """
                {"slug":"%s","categoryId":%d,"featured":true,"sortOrder":99,"version":%d}
                """.formatted(slug, categoryId, version);
    }
}
