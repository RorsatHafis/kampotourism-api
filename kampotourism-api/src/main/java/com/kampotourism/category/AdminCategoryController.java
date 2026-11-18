package com.kampotourism.category;

import com.kampotourism.category.CategoryDtos.AdminCategoryDetail;
import com.kampotourism.category.CategoryDtos.CategoryTextBody;
import com.kampotourism.category.CategoryDtos.CreateCategoryRequest;
import com.kampotourism.category.CategoryDtos.UpdateCategoryRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
@RequestMapping("/api/v1/admin/categories")
@SecurityRequirement(name = "bearer")
public class AdminCategoryController {

    private final CategoryService service;

    public AdminCategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminCategoryDetail> list(@RequestParam(required = false) String kind) {
        return service.adminList((kind == null || kind.isBlank()) ? null : CategoryKind.fromKey(kind));
    }

    @PostMapping
    public ResponseEntity<AdminCategoryDetail> create(@Valid @RequestBody CreateCategoryRequest request) {
        AdminCategoryDetail created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/categories/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public AdminCategoryDetail get(@PathVariable long id) {
        return service.adminGet(id);
    }

    @PutMapping("/{id}")
    public AdminCategoryDetail update(@PathVariable long id, @Valid @RequestBody UpdateCategoryRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/translations/{lang}")
    public AdminCategoryDetail upsertTranslation(@PathVariable long id, @PathVariable String lang,
                                                 @Valid @RequestBody CategoryTextBody body) {
        return service.upsertTranslation(id, lang, body);
    }

    @DeleteMapping("/{id}/translations/{lang}")
    public ResponseEntity<Void> deleteTranslation(@PathVariable long id, @PathVariable String lang) {
        service.deleteTranslation(id, lang);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
