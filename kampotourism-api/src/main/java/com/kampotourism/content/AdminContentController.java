package com.kampotourism.content;

import com.kampotourism.common.PageResponse;
import com.kampotourism.content.ContentDtos.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/{module:experiences|accommodations|transportation|articles|travel-information}")
@SecurityRequirement(name="bearer")
public class AdminContentController {
    private final ContentService service;
    public AdminContentController(ContentService service){this.service=service;}
    @GetMapping public PageResponse<AdminSummary> list(@PathVariable String module,@RequestParam(defaultValue="all") String status,@RequestParam(required=false) String q,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return service.adminList(ContentType.fromPath(module),status,q,page,size);}
    @PostMapping public ResponseEntity<AdminDetail> create(@PathVariable String module,@Valid @RequestBody CreateRequest request){ContentType type=ContentType.fromPath(module); AdminDetail item=service.create(type,request);return ResponseEntity.created(URI.create("/api/v1/admin/"+type.path()+"/"+item.id())).body(item);}
    @GetMapping("/{id}") public AdminDetail get(@PathVariable String module,@PathVariable long id){return service.adminGet(ContentType.fromPath(module),id);}
    @PutMapping("/{id}") public AdminDetail update(@PathVariable String module,@PathVariable long id,@Valid @RequestBody UpdateRequest request){return service.update(ContentType.fromPath(module),id,request);}
    @PutMapping("/{id}/translations/{lang}") public AdminDetail translation(@PathVariable String module,@PathVariable long id,@PathVariable String lang,@Valid @RequestBody TranslationBody request){return service.translation(ContentType.fromPath(module),id,lang,request);}
    @DeleteMapping("/{id}/translations/{lang}") public ResponseEntity<Void> deleteTranslation(@PathVariable String module,@PathVariable long id,@PathVariable String lang){service.deleteTranslation(ContentType.fromPath(module),id,lang);return ResponseEntity.noContent().build();}
    @PutMapping("/{id}/images") public AdminDetail images(@PathVariable String module,@PathVariable long id,@Valid @RequestBody ImagesRequest request){return service.replaceImages(ContentType.fromPath(module),id,request);}
    @PostMapping("/{id}/publish") public AdminDetail publish(@PathVariable String module,@PathVariable long id){return service.lifecycle(ContentType.fromPath(module),id,ContentStatus.PUBLISHED);}
    @PostMapping("/{id}/unpublish") public AdminDetail unpublish(@PathVariable String module,@PathVariable long id){return service.lifecycle(ContentType.fromPath(module),id,ContentStatus.DRAFT);}
    @PostMapping("/{id}/archive") public AdminDetail archive(@PathVariable String module,@PathVariable long id){return service.lifecycle(ContentType.fromPath(module),id,ContentStatus.ARCHIVED);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String module,@PathVariable long id){service.delete(ContentType.fromPath(module),id);return ResponseEntity.noContent().build();}
}
