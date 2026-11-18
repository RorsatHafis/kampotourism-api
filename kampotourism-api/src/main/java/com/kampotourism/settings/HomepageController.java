package com.kampotourism.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.kampotourism.common.i18n.LanguageResolver;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class HomepageController {
    private final HomepageService service; private final LanguageResolver languages;
    public HomepageController(HomepageService service, LanguageResolver languages) { this.service=service; this.languages=languages; }
    @GetMapping("/api/v1/homepage") public ResponseEntity<HomepageService.Home> get(@RequestParam(required=false) String lang,
            @RequestHeader(value=HttpHeaders.ACCEPT_LANGUAGE,required=false) String accept) { String resolved=languages.resolve(lang,accept); return ResponseEntity.ok().header(HttpHeaders.CONTENT_LANGUAGE,resolved).body(service.view(resolved)); }
    @GetMapping("/api/v1/admin/settings/homepage") @PreAuthorize("hasRole('ADMIN')") @SecurityRequirement(name="bearer")
    public HomepageService.AdminHome admin(@RequestParam(defaultValue="en") String lang) { return service.admin(languages.requireSupported(lang)); }
    @PutMapping("/api/v1/admin/settings/homepage") @PreAuthorize("hasRole('ADMIN')") @SecurityRequirement(name="bearer")
    public HomepageService.AdminHome update(@RequestParam(defaultValue="en") String lang, @Valid @RequestBody UpdateHomepage request) { return service.update(request.version(),languages.requireSupported(lang),request.configuration()); }
    public record UpdateHomepage(long version, @NotNull JsonNode configuration) { }
}
