package com.kampotourism.destination;

import com.kampotourism.category.CategoryDtos.CategoryDto;
import com.kampotourism.media.MediaDtos.ImageDto;
import java.math.BigDecimal;
import java.util.List;

/** Public API shapes. Null fields are omitted from the JSON. */
public final class DestinationDtos {

    private DestinationDtos() {
    }

    public record LocationDto(double lat, double lng) {
    }

    public record EntranceFeeDto(BigDecimal amount, String currency, String note) {
    }

    public record FactsDto(String distance, String duration, String bestFor, String bring,
                           String openingHours, String bestSeason, EntranceFeeDto entranceFee) {
    }

    public record DestinationCard(String slug, String name, CategoryDto category, String tag,
                                  String summary, ImageDto cover, String distance, String duration,
                                  String language) {
    }

    public record DestinationDetail(String slug, String name, CategoryDto category, String tag,
                                    String summary, String lead, List<String> description,
                                    ImageDto cover, List<ImageDto> gallery, LocationDto location,
                                    FactsDto facts, List<Highlight> highlights, List<String> tips,
                                    List<DestinationCard> related, String language) {
    }
}
