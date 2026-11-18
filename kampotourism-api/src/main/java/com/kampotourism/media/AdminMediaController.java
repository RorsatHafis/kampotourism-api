package com.kampotourism.media;

import com.kampotourism.common.PageResponse;
import com.kampotourism.media.MediaDtos.AdminMediaSummary;
import com.kampotourism.media.MediaDtos.AdminMediaView;
import com.kampotourism.media.MediaDtos.UpdateMediaTextsRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/media")
@SecurityRequirement(name = "bearer")
public class AdminMediaController {

    private final MediaService service;

    public AdminMediaController(MediaService service) {
        this.service = service;
    }

    /** Upload an image. Optional alt/caption are saved in the default language. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminMediaView> upload(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(required = false) String alt,
                                                 @RequestParam(required = false) String caption) {
        AdminMediaView created = service.upload(file, alt, caption);
        return ResponseEntity.created(URI.create("/api/v1/admin/media/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<AdminMediaSummary> list(@RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "24") int size) {
        return service.list(q, page, size);
    }

    @GetMapping("/{id}")
    public AdminMediaView get(@PathVariable long id) {
        return service.get(id);
    }

    /** Update alt text and captions (all languages at once). */
    @PutMapping("/{id}")
    public AdminMediaView updateTexts(@PathVariable long id, @Valid @RequestBody UpdateMediaTextsRequest request) {
        return service.updateTexts(id, request);
    }

    /** Replace the file itself; the id, texts and every place it is used stay the same. */
    @PostMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminMediaView replaceFile(@PathVariable long id, @RequestParam("file") MultipartFile file) {
        return service.replaceFile(id, file);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id, @RequestParam(defaultValue = "false") boolean force) {
        service.delete(id, force);
        return ResponseEntity.noContent().build();
    }
}
