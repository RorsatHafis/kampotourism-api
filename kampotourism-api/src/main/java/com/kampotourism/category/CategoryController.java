package com.kampotourism.category;

import com.kampotourism.category.CategoryDtos.CategoryDto;
import com.kampotourism.common.i18n.LanguageResolver;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final CategoryService service;
    private final LanguageResolver languages;

    public CategoryController(CategoryService service, LanguageResolver languages) {
        this.service = service;
        this.languages = languages;
    }

    /** {@code ?kind=destination|experience|stay-type|transport|blog}; omit for all kinds. */
    @GetMapping
    public ResponseEntity<List<CategoryDto>> list(
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String lang,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        String resolved = languages.resolve(lang, acceptLanguage);
        CategoryKind parsed = (kind == null || kind.isBlank()) ? null : CategoryKind.fromKey(kind);
        return ResponseEntity.ok()
                .cacheControl(CACHE)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .header(HttpHeaders.CONTENT_LANGUAGE, resolved)
                .body(service.list(parsed, resolved));
    }
}
