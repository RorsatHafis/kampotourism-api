package com.kampotourism.destination;

import com.kampotourism.common.PageResponse;
import com.kampotourism.common.i18n.LanguageResolver;
import com.kampotourism.destination.DestinationDtos.DestinationCard;
import com.kampotourism.destination.DestinationDtos.DestinationDetail;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/destinations")
public class DestinationController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final DestinationService service;
    private final LanguageResolver languages;

    public DestinationController(DestinationService service, LanguageResolver languages) {
        this.service = service;
        this.languages = languages;
    }

    /** {@code category} is a category slug, see GET /api/v1/categories?kind=destination. */
    @GetMapping
    public ResponseEntity<PageResponse<DestinationCard>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String lang,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {

        String resolved = languages.resolve(lang, acceptLanguage);
        return cached(service.list(category, featured, q, page, size, resolved), resolved);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<DestinationDetail> get(
            @PathVariable String slug,
            @RequestParam(required = false) String lang,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {

        DestinationDetail detail = service.get(slug, languages.resolve(lang, acceptLanguage));
        return cached(detail, detail.language());
    }

    private static <T> ResponseEntity<T> cached(T body, String language) {
        return ResponseEntity.ok()
                .cacheControl(CACHE)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .header(HttpHeaders.CONTENT_LANGUAGE, language)
                .body(body);
    }
}
