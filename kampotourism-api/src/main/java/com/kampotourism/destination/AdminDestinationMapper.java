package com.kampotourism.destination;

import com.kampotourism.destination.AdminDestinationDtos.HighlightBody;
import com.kampotourism.destination.AdminDestinationDtos.TranslationBody;
import java.util.List;

final class AdminDestinationMapper {

    private AdminDestinationMapper() {
    }

    static TranslationBody toBody(DestinationTranslation t) {
        List<HighlightBody> highlights = t.getHighlights().stream()
                .map(h -> new HighlightBody(h.title(), h.text()))
                .toList();
        return new TranslationBody(t.getName(), t.getTag(), t.getSummary(), t.getLead(), t.getDescription(),
                highlights, t.getTips(), t.getDistance(), t.getDuration(), t.getBestFor(), t.getBring(),
                t.getOpeningHours(), t.getBestSeason(), t.getEntranceFeeNote());
    }

    /** Trims text and turns blank optional fields into null so empty strings never reach the database. */
    static void apply(DestinationTranslation t, TranslationBody b) {
        t.edit(b.name().trim(), clean(b.tag()), b.summary().trim(), b.lead().trim(),
                b.description().stream().map(String::trim).toList(),
                b.highlights().stream().map(h -> new Highlight(h.title().trim(), h.text().trim())).toList(),
                b.tips().stream().map(String::trim).toList(),
                clean(b.distance()), clean(b.duration()), clean(b.bestFor()), clean(b.bring()),
                clean(b.openingHours()), clean(b.bestSeason()), clean(b.entranceFeeNote()));
    }

    private static String clean(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
