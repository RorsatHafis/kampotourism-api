package com.kampotourism.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kampotourism.config.MediaProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Minimal signed client for Cloudinary's Upload API (upload + destroy), using only the JDK
 * HTTP client so the image stays small for a 512 MB instance. The API secret is used only to
 * compute request signatures and is never sent, logged or included in exception messages.
 */
class CloudinaryClient {

    static final URI DEFAULT_API_BASE = URI.create("https://api.cloudinary.com/v1_1/");

    private final MediaProperties.Cloudinary config;
    private final URI apiBase;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    CloudinaryClient(MediaProperties.Cloudinary config) {
        this(config, DEFAULT_API_BASE);
    }

    CloudinaryClient(MediaProperties.Cloudinary config, URI apiBase) {
        this.config = config;
        this.apiBase = apiBase;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** Uploads the file under the given public id. Never overwrites an existing asset. */
    void upload(Path file, String publicId) {
        Map<String, String> signed = new TreeMap<>();
        signed.put("overwrite", "false");
        signed.put("public_id", publicId);
        signed.put("timestamp", Long.toString(Instant.now().getEpochSecond()));

        Map<String, String> fields = new LinkedHashMap<>(signed);
        fields.put("api_key", config.apiKey());
        fields.put("signature", sign(signed, config.apiSecret()));

        String boundary = "----kampotourism" + UUID.randomUUID().toString().replace("-", "");
        byte[] body;
        try {
            body = multipart(boundary, fields, file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read upload for Cloudinary", e);
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint("image/upload"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        JsonNode response = send(request, "upload");
        if (!publicId.equals(response.path("public_id").asText(null))) {
            throw new IllegalStateException("Cloudinary upload returned an unexpected public_id");
        }
    }

    /** Deletes the asset and invalidates CDN caches. Returns false when it did not exist. */
    boolean destroy(String publicId) {
        Map<String, String> signed = new TreeMap<>();
        signed.put("invalidate", "true");
        signed.put("public_id", publicId);
        signed.put("timestamp", Long.toString(Instant.now().getEpochSecond()));

        Map<String, String> fields = new LinkedHashMap<>(signed);
        fields.put("api_key", config.apiKey());
        fields.put("signature", sign(signed, config.apiSecret()));

        String form = fields.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(endpoint("image/destroy"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        return "ok".equals(send(request, "destroy").path("result").asText());
    }

    /**
     * Cloudinary signature: parameters sorted by name, joined as {@code k=v&k=v}, secret appended,
     * SHA-1 hex. See https://cloudinary.com/documentation/authentication_signatures
     */
    static String sign(Map<String, String> params, String apiSecret) {
        String toSign = new TreeMap<>(params).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(sha1.digest((toSign + apiSecret).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private URI endpoint(String action) {
        return apiBase.resolve(URLEncoder.encode(config.cloudName(), StandardCharsets.UTF_8) + "/" + action);
    }

    private JsonNode send(HttpRequest request, String operation) {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException("Cloudinary " + operation + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cloudinary " + operation + " was interrupted", e);
        }
        JsonNode body;
        try {
            body = json.readTree(response.body() == null || response.body().isBlank() ? "{}" : response.body());
        } catch (IOException e) {
            body = json.createObjectNode();
        }
        if (response.statusCode() / 100 != 2) {
            String message = body.path("error").path("message").asText("no error message");
            throw new IllegalStateException("Cloudinary " + operation + " failed with HTTP "
                    + response.statusCode() + ": " + message);
        }
        return body;
    }

    private static byte[] multipart(String boundary, Map<String, String> fields, Path file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) Files.size(file) + 1024);
        for (Map.Entry<String, String> field : fields.entrySet()) {
            write(out, "--" + boundary + "\r\n");
            write(out, "Content-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n");
            write(out, field.getValue() + "\r\n");
        }
        write(out, "--" + boundary + "\r\n");
        write(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getFileName() + "\"\r\n");
        write(out, "Content-Type: application/octet-stream\r\n\r\n");
        Files.copy(file, out);
        write(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }
}
