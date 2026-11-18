package com.kampotourism.content;

import com.kampotourism.common.PageResponse;
import com.kampotourism.common.i18n.LanguageResolver;
import com.kampotourism.content.ContentDtos.*;
import java.time.Duration;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Public routes: /experiences, /accommodations, /transportation, /articles and /travel-information. */
@RestController
@RequestMapping("/api/v1/{module:experiences|accommodations|transportation|articles|travel-information}")
public class ContentController {
    private final ContentService service; private final LanguageResolver languages;
    public ContentController(ContentService service, LanguageResolver languages){this.service=service;this.languages=languages;}
    @GetMapping public ResponseEntity<PageResponse<ContentCard>> list(@PathVariable String module,@RequestParam(required=false) String category,
            @RequestParam(required=false) Boolean featured,@RequestParam(required=false) String q,@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="12") int size,@RequestParam(required=false) String lang,@RequestHeader(value=HttpHeaders.ACCEPT_LANGUAGE,required=false) String acceptLanguage){
        String resolved=languages.resolve(lang,acceptLanguage); return cached(service.list(ContentType.fromPath(module),category,featured,q,page,size,resolved),resolved);
    }
    @GetMapping("/{slug}") public ResponseEntity<ContentDetail> get(@PathVariable String module,@PathVariable String slug,@RequestParam(required=false) String lang,@RequestHeader(value=HttpHeaders.ACCEPT_LANGUAGE,required=false) String acceptLanguage){
        ContentDetail body=service.get(ContentType.fromPath(module),slug,languages.resolve(lang,acceptLanguage)); return cached(body,body.language());
    }
    private static <T> ResponseEntity<T> cached(T body,String lang){return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).header(HttpHeaders.VARY,HttpHeaders.ACCEPT_LANGUAGE).header(HttpHeaders.CONTENT_LANGUAGE,lang).body(body);}
}
