package com.kampotourism.category;

import com.kampotourism.category.CategoryDtos.AdminCategoryDetail;
import com.kampotourism.category.CategoryDtos.CategoryDto;
import com.kampotourism.category.CategoryDtos.CategoryTextBody;
import com.kampotourism.category.CategoryDtos.CreateCategoryRequest;
import com.kampotourism.category.CategoryDtos.UpdateCategoryRequest;
import com.kampotourism.common.error.BadRequestException;
import com.kampotourism.common.error.ConflictException;
import com.kampotourism.common.error.NotFoundException;
import com.kampotourism.common.i18n.LanguageResolver;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;
    private final CategoryTranslationRepository translations;
    private final LanguageResolver languages;

    public CategoryService(CategoryRepository categories, CategoryTranslationRepository translations,
                           LanguageResolver languages) {
        this.categories = categories;
        this.translations = translations;
        this.languages = languages;
    }

    // ------------------------------------------------------------------ public

    public List<CategoryDto> list(CategoryKind kind, String lang) {
        List<Category> items = kind == null
                ? categories.findAllByOrderByKindAscSortOrderAscIdAsc()
                : categories.findByKindOrderBySortOrderAscIdAsc(kind);
        Map<Long, CategoryTranslation> picked = pick(items.stream().map(Category::getId).toList(), lang);
        return items.stream()
                .filter(c -> picked.containsKey(c.getId()))
                .map(c -> toDto(c, picked.get(c.getId())))
                .toList();
    }

    /** Used by other modules to label their items; missing ids are simply absent from the result. */
    public Map<Long, CategoryDto> byIds(Collection<Long> ids, String lang) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, CategoryTranslation> picked = pick(ids, lang);
        Map<Long, CategoryDto> result = new HashMap<>();
        for (Category category : categories.findAllById(ids)) {
            CategoryTranslation text = picked.get(category.getId());
            if (text != null) {
                result.put(category.getId(), toDto(category, text));
            }
        }
        return result;
    }

    public Long idForSlug(CategoryKind kind, String slug) {
        return categories.findByKindAndSlug(kind, slug.trim().toLowerCase(Locale.ROOT))
                .map(Category::getId)
                .orElseThrow(() -> new BadRequestException("Unknown %s category '%s'".formatted(kind.key(), slug)));
    }

    /** Guards admin writes: the id must exist and belong to the expected kind. */
    public void requireKind(Long id, CategoryKind kind) {
        categories.findByIdAndKind(id, kind).orElseThrow(() ->
                new BadRequestException("categoryId %d is not a %s category".formatted(id, kind.key())));
    }

    // ------------------------------------------------------------------ admin

    public List<AdminCategoryDetail> adminList(CategoryKind kind) {
        List<Category> items = kind == null
                ? categories.findAllByOrderByKindAscSortOrderAscIdAsc()
                : categories.findByKindOrderBySortOrderAscIdAsc(kind);
        return items.stream().map(this::detail).toList();
    }

    public AdminCategoryDetail adminGet(long id) {
        return detail(find(id));
    }

    @Transactional
    public AdminCategoryDetail create(CreateCategoryRequest request) {
        CategoryKind kind = CategoryKind.fromKey(request.kind());
        String slug = request.slug();
        if (categories.existsByKindAndSlug(kind, slug)) {
            throw new ConflictException("A %s category with slug '%s' already exists".formatted(kind.key(), slug));
        }
        Category category = categories.saveAndFlush(new Category(kind, slug, request.sortOrder()));
        CategoryTranslation text = new CategoryTranslation(category.getId(), languages.defaultLang());
        text.edit(request.translation().name(), request.translation().description());
        translations.save(text);
        return detail(category);
    }

    @Transactional
    public AdminCategoryDetail update(long id, UpdateCategoryRequest request) {
        Category category = find(id);
        if (category.getVersion() != request.version()) {
            throw new ConflictException("This category was changed by someone else. Reload it and try again.");
        }
        if (categories.existsByKindAndSlugAndIdNot(category.getKind(), request.slug(), id)) {
            throw new ConflictException("Slug '%s' is already used by another category".formatted(request.slug()));
        }
        category.edit(request.slug(), request.sortOrder());
        categories.saveAndFlush(category);
        return detail(category);
    }

    @Transactional
    public AdminCategoryDetail upsertTranslation(long id, String lang, CategoryTextBody body) {
        Category category = find(id);
        String normalized = languages.requireSupported(lang);
        CategoryTranslation text = translations.findByCategoryIdAndLang(id, normalized)
                .orElseGet(() -> new CategoryTranslation(id, normalized));
        text.edit(body.name(), body.description());
        translations.save(text);
        return detail(category);
    }

    @Transactional
    public void deleteTranslation(long id, String lang) {
        find(id);
        String normalized = languages.requireSupported(lang);
        if (normalized.equals(languages.defaultLang())) {
            throw new ConflictException("The default-language (%s) name cannot be removed.".formatted(normalized));
        }
        translations.deleteByCategoryIdAndLang(id, normalized);
    }

    /** The database refuses (409) if any content still uses the category. */
    @Transactional
    public void delete(long id) {
        Category category = find(id);
        categories.delete(category);
        categories.flush();
    }

    // ------------------------------------------------------------------ helpers

    private AdminCategoryDetail detail(Category c) {
        Map<String, CategoryTextBody> texts = new TreeMap<>();
        translations.findByCategoryId(c.getId())
                .forEach(t -> texts.put(t.getLang(), new CategoryTextBody(t.getName(), t.getDescription())));
        return new AdminCategoryDetail(c.getId(), c.getVersion(), c.getKind().key(), c.getSlug(), c.getSortOrder(), texts);
    }

    private Category find(long id) {
        return categories.findById(id)
                .orElseThrow(() -> new NotFoundException("Category %d was not found".formatted(id)));
    }

    private static CategoryDto toDto(Category c, CategoryTranslation t) {
        return new CategoryDto(c.getKind().key(), c.getSlug(), t.getName(), t.getDescription());
    }

    private Map<Long, CategoryTranslation> pick(Collection<Long> ids, String lang) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Set<String> wanted = Set.copyOf(List.of(lang, languages.defaultLang()));
        Map<Long, CategoryTranslation> picked = new HashMap<>();
        for (CategoryTranslation t : translations.findByCategoryIdInAndLangIn(ids, wanted)) {
            if (t.getLang().equals(lang) || !picked.containsKey(t.getCategoryId())) {
                picked.put(t.getCategoryId(), t);
            }
        }
        return picked;
    }
}
