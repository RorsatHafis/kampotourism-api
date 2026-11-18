package com.kampotourism.media;

import com.kampotourism.common.PageResponse;
import com.kampotourism.common.error.BadRequestException;
import com.kampotourism.common.error.ConflictException;
import com.kampotourism.common.error.NotFoundException;
import com.kampotourism.common.i18n.LanguageResolver;
import com.kampotourism.media.MediaDtos.AdminMediaSummary;
import com.kampotourism.media.MediaDtos.AdminMediaView;
import com.kampotourism.media.MediaDtos.MediaTextBody;
import com.kampotourism.media.MediaDtos.UpdateMediaTextsRequest;
import com.kampotourism.media.MediaDtos.UsageDto;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** Admin operations on image assets: upload, list, read, edit alt text/captions, replace file, delete. */
@Service
@Transactional
public class MediaService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MediaStorage storage;
    private final MediaUrls urls;
    private final MediaAssetRepository assets;
    private final MediaAssetTranslationRepository texts;
    private final MediaAttachmentRepository attachments;
    private final LanguageResolver languages;

    public MediaService(MediaStorage storage, MediaUrls urls, MediaAssetRepository assets,
                        MediaAssetTranslationRepository texts, MediaAttachmentRepository attachments,
                        LanguageResolver languages) {
        this.storage = storage;
        this.urls = urls;
        this.assets = assets;
        this.texts = texts;
        this.attachments = attachments;
        this.languages = languages;
    }

    public AdminMediaView upload(MultipartFile file, String alt, String caption) {
        if ((alt != null && alt.length() > 300) || (caption != null && caption.length() > 500)) {
            throw new BadRequestException("Alt text is limited to 300 characters and captions to 500.");
        }
        MediaStorage.StoredFile stored = storage.store(file);
        deleteFileIfRolledBack(stored.key());

        MediaAsset asset = assets.saveAndFlush(new MediaAsset(stored));
        if ((alt != null && !alt.isBlank()) || (caption != null && !caption.isBlank())) {
            MediaAssetTranslation text = new MediaAssetTranslation(asset.getId(), languages.defaultLang());
            text.edit(alt, caption);
            texts.save(text);
        }
        return view(asset);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminMediaSummary> list(String q, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
        Page<MediaAsset> result = (q == null || q.isBlank())
                ? assets.findAll(pageable)
                : assets.findByOriginalFilenameContainingIgnoreCase(q.trim(), pageable);

        List<Long> ids = result.getContent().stream().map(MediaAsset::getId).toList();
        Map<Long, String> alt = new HashMap<>();
        Map<Long, Long> usage = new HashMap<>();
        if (!ids.isEmpty()) {
            texts.findByAssetIdInAndLangIn(ids, Set.of(languages.defaultLang()))
                    .forEach(t -> alt.put(t.getAssetId(), t.getAltText()));
            for (Object[] row : attachments.countByAssetIds(ids)) {
                usage.put((Long) row[0], (Long) row[1]);
            }
        }
        List<AdminMediaSummary> items = result.getContent().stream()
                .map(a -> new AdminMediaSummary(a.getId(), urls.url(a.getStorageKey()), a.getOriginalFilename(),
                        a.getMimeType(), a.getSizeBytes(), a.getWidth(), a.getHeight(), alt.get(a.getId()),
                        usage.getOrDefault(a.getId(), 0L), a.getUpdatedAt()))
                .toList();
        return new PageResponse<>(items, safePage, safeSize, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminMediaView get(long id) {
        return view(find(id));
    }

    /** Replaces every alt text / caption of the asset. Alt text in the default language is mandatory. */
    public AdminMediaView updateTexts(long id, UpdateMediaTextsRequest request) {
        MediaAsset asset = find(id);
        String defaultLang = languages.defaultLang();

        Map<String, MediaTextBody> wanted = new LinkedHashMap<>();
        request.translations().forEach((lang, body) -> wanted.put(languages.requireSupported(lang), body));

        MediaTextBody primary = wanted.get(defaultLang);
        if (primary == null || primary.altText() == null || primary.altText().isBlank()) {
            throw new BadRequestException("Alt text in the default language (%s) is required.".formatted(defaultLang));
        }

        Map<String, MediaAssetTranslation> existing = new HashMap<>();
        texts.findByAssetId(id).forEach(t -> existing.put(t.getLang(), t));

        existing.forEach((lang, row) -> {
            if (!wanted.containsKey(lang)) {
                texts.delete(row);
            }
        });
        wanted.forEach((lang, body) -> {
            MediaAssetTranslation row = existing.getOrDefault(lang, new MediaAssetTranslation(id, lang));
            row.edit(body.altText(), body.caption());
            texts.save(row);
        });
        return view(asset);
    }

    /** Swaps the file but keeps the id, the alt text and every place the image is used. */
    public AdminMediaView replaceFile(long id, MultipartFile file) {
        MediaAsset asset = find(id);
        String oldKey = asset.getStorageKey();
        MediaStorage.StoredFile stored = storage.store(file);
        deleteFileIfRolledBack(stored.key());
        asset.replaceFile(stored);
        assets.saveAndFlush(asset);
        deleteFileAfterCommit(oldKey);
        return view(asset);
    }

    /** Refuses when the image is still in use, unless force is set (which detaches it first). */
    public void delete(long id, boolean force) {
        MediaAsset asset = find(id);
        long used = attachments.countByAssetId(id);
        if (used > 0 && !force) {
            throw new ConflictException(("This image is used by %d item(s). Detach it first, "
                    + "or delete with force=true to detach it everywhere.").formatted(used));
        }
        attachments.deleteByAssetId(id);
        assets.delete(asset);
        assets.flush();
        deleteFileAfterCommit(asset.getStorageKey());
    }

    private AdminMediaView view(MediaAsset a) {
        Map<String, MediaTextBody> translations = new TreeMap<>();
        texts.findByAssetId(a.getId())
                .forEach(t -> translations.put(t.getLang(), new MediaTextBody(t.getAltText(), t.getCaption())));
        List<UsageDto> usages = attachments.findByAssetId(a.getId()).stream()
                .map(u -> new UsageDto(u.getOwnerType().name(), u.getOwnerId(), u.getRole().name()))
                .toList();
        return new AdminMediaView(a.getId(), a.getVersion(), urls.url(a.getStorageKey()), a.getMimeType(),
                a.getSizeBytes(), a.getWidth(), a.getHeight(), a.getOriginalFilename(), a.getUpdatedAt(),
                translations, usages);
    }

    private MediaAsset find(long id) {
        return assets.findById(id)
                .orElseThrow(() -> new NotFoundException("Media %d was not found".formatted(id)));
    }

    private void deleteFileAfterCommit(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.delete(key);
            }
        });
    }

    private void deleteFileIfRolledBack(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storage.delete(key);
                }
            }
        });
    }
}
