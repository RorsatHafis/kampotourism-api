package com.kampotourism.destination;

import com.kampotourism.category.CategoryDtos.CategoryDto;
import com.kampotourism.destination.DestinationDtos.DestinationCard;
import com.kampotourism.destination.DestinationDtos.DestinationDetail;
import com.kampotourism.destination.DestinationDtos.EntranceFeeDto;
import com.kampotourism.destination.DestinationDtos.FactsDto;
import com.kampotourism.destination.DestinationDtos.LocationDto;
import com.kampotourism.media.MediaDtos.ImageDto;
import com.kampotourism.media.MediaDtos.OwnerImages;
import java.util.List;

final class DestinationMapper {

    private DestinationMapper() {
    }

    static DestinationCard toCard(Destination d, DestinationTranslation t, CategoryDto category,
                                  OwnerImages images) {
        return new DestinationCard(d.getSlug(), t.getName(), category, t.getTag(), t.getSummary(),
                cover(d, t, images), t.getDistance(), t.getDuration(), t.getLang());
    }

    static DestinationDetail toDetail(Destination d, DestinationTranslation t, CategoryDto category,
                                      OwnerImages images, List<DestinationCard> related) {
        List<ImageDto> gallery = images == null ? List.of() : images.gallery();
        return new DestinationDetail(d.getSlug(), t.getName(), category, t.getTag(), t.getSummary(),
                t.getLead(), t.getDescription(), cover(d, t, images), gallery, location(d), facts(d, t),
                t.getHighlights(), t.getTips(), related, t.getLang());
    }

    /** The attached cover wins; the demo seed's external URL is only a fallback. */
    private static ImageDto cover(Destination d, DestinationTranslation t, OwnerImages images) {
        if (images != null && images.cover() != null) {
            return images.cover();
        }
        if (d.getImageUrl() != null) {
            return new ImageDto(d.getImageUrl(), t.getName(), null, null, null);
        }
        return null;
    }

    private static LocationDto location(Destination d) {
        if (d.getLatitude() == null || d.getLongitude() == null) {
            return null;
        }
        return new LocationDto(d.getLatitude(), d.getLongitude());
    }

    private static FactsDto facts(Destination d, DestinationTranslation t) {
        EntranceFeeDto fee = d.getEntranceFee() == null ? null
                : new EntranceFeeDto(d.getEntranceFee(), d.getEntranceFeeCurrency(), t.getEntranceFeeNote());
        return new FactsDto(t.getDistance(), t.getDuration(), t.getBestFor(), t.getBring(),
                t.getOpeningHours(), t.getBestSeason(), fee);
    }
}
