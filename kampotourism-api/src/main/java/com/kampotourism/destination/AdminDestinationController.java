package com.kampotourism.destination;

import com.kampotourism.common.PageResponse;
import com.kampotourism.destination.AdminDestinationDtos.AdminDestinationDetail;
import com.kampotourism.destination.AdminDestinationDtos.AdminDestinationSummary;
import com.kampotourism.destination.AdminDestinationDtos.CreateDestinationRequest;
import com.kampotourism.destination.AdminDestinationDtos.ImagesRequest;
import com.kampotourism.destination.AdminDestinationDtos.TranslationBody;
import com.kampotourism.destination.AdminDestinationDtos.UpdateDestinationRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/destinations")
@SecurityRequirement(name = "bearer")
public class AdminDestinationController {

    private final AdminDestinationService service;

    public AdminDestinationController(AdminDestinationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AdminDestinationSummary> list(@RequestParam(defaultValue = "all") String status,
                                                      @RequestParam(required = false) String q,
                                                      @RequestParam(required = false) Long categoryId,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return service.list(status, q, categoryId, page, size);
    }

    @PostMapping
    public ResponseEntity<AdminDestinationDetail> create(@Valid @RequestBody CreateDestinationRequest request) {
        AdminDestinationDetail created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/destinations/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public AdminDestinationDetail get(@PathVariable long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public AdminDestinationDetail update(@PathVariable long id, @Valid @RequestBody UpdateDestinationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/translations/{lang}")
    public AdminDestinationDetail upsertTranslation(@PathVariable long id, @PathVariable String lang,
                                                    @Valid @RequestBody TranslationBody body) {
        return service.upsertTranslation(id, lang, body);
    }

    @DeleteMapping("/{id}/translations/{lang}")
    public ResponseEntity<Void> deleteTranslation(@PathVariable long id, @PathVariable String lang) {
        service.deleteTranslation(id, lang);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public AdminDestinationDetail publish(@PathVariable long id) {
        return service.setPublished(id, true);
    }

    @PostMapping("/{id}/unpublish")
    public AdminDestinationDetail unpublish(@PathVariable long id) {
        return service.setPublished(id, false);
    }

    /** Body: {"cover": 12, "gallery": [15, 14, 20]}. Replaces the whole image set. */
    @PutMapping("/{id}/images")
    public AdminDestinationDetail replaceImages(@PathVariable long id, @Valid @RequestBody ImagesRequest request) {
        return service.replaceImages(id, request);
    }
}
