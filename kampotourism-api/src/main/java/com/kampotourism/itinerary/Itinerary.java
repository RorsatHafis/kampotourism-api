package com.kampotourism.itinerary;
import com.kampotourism.content.ContentStatus;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
@Entity @Table(name="itinerary") public class Itinerary {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false) private String slug;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ContentStatus status=ContentStatus.DRAFT; private boolean featured; private int sortOrder; private int daysCount;
 private Instant publishedAt; @Version private long version; @CreationTimestamp private Instant createdAt; @UpdateTimestamp private Instant updatedAt;
 protected Itinerary(){} Itinerary(String slug,boolean featured,int sortOrder,int daysCount){this.slug=slug;this.featured=featured;this.sortOrder=sortOrder;this.daysCount=daysCount;}
 void edit(String slug,boolean featured,int sortOrder,int daysCount){this.slug=slug;this.featured=featured;this.sortOrder=sortOrder;this.daysCount=daysCount;} void status(ContentStatus s){status=s;publishedAt=s==ContentStatus.PUBLISHED?Instant.now():null;}
 public Long getId(){return id;} public String getSlug(){return slug;} public ContentStatus getStatus(){return status;} public boolean isFeatured(){return featured;} public int getSortOrder(){return sortOrder;} public int getDaysCount(){return daysCount;} public Instant getPublishedAt(){return publishedAt;} public long getVersion(){return version;} public Instant getUpdatedAt(){return updatedAt;}
}
