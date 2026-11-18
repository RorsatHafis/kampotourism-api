package com.kampotourism.destination;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Everything a visitor reads about a destination, in one language. */
@Entity
@Table(name = "destination_translation")
public class DestinationTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destination_id", nullable = false)
    private Long destinationId;

    @Column(nullable = false)
    private String lang;

    @Column(nullable = false)
    private String name;

    private String tag;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private String lead;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> description;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Highlight> highlights;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> tips;

    private String distance;
    private String duration;
    private String bestFor;
    private String bring;
    private String openingHours;
    private String bestSeason;
    private String entranceFeeNote;

    protected DestinationTranslation() {
    }

    DestinationTranslation(Long destinationId, String lang) {
        this.destinationId = destinationId;
        this.lang = lang;
    }

    void edit(String name, String tag, String summary, String lead, List<String> description,
              List<Highlight> highlights, List<String> tips, String distance, String duration,
              String bestFor, String bring, String openingHours, String bestSeason, String entranceFeeNote) {
        this.name = name;
        this.tag = tag;
        this.summary = summary;
        this.lead = lead;
        this.description = List.copyOf(description);
        this.highlights = List.copyOf(highlights);
        this.tips = List.copyOf(tips);
        this.distance = distance;
        this.duration = duration;
        this.bestFor = bestFor;
        this.bring = bring;
        this.openingHours = openingHours;
        this.bestSeason = bestSeason;
        this.entranceFeeNote = entranceFeeNote;
    }

    public Long getDestinationId() { return destinationId; }
    public String getLang() { return lang; }
    public String getName() { return name; }
    public String getTag() { return tag; }
    public String getSummary() { return summary; }
    public String getLead() { return lead; }
    public List<String> getDescription() { return description; }
    public List<Highlight> getHighlights() { return highlights; }
    public List<String> getTips() { return tips; }
    public String getDistance() { return distance; }
    public String getDuration() { return duration; }
    public String getBestFor() { return bestFor; }
    public String getBring() { return bring; }
    public String getOpeningHours() { return openingHours; }
    public String getBestSeason() { return bestSeason; }
    public String getEntranceFeeNote() { return entranceFeeNote; }
}
