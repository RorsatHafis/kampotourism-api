package com.kampotourism.itinerary;
import com.fasterxml.jackson.databind.JsonNode; import com.kampotourism.content.ContentStatus; import com.kampotourism.media.MediaDtos.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.time.Instant; import java.util.*;
public final class ItineraryDtos { private ItineraryDtos(){}
 public record ActivityBody(Long destinationId,Long experienceId){}
 public record DayBody(@NotNull @Size(max=30) List<@Valid ActivityBody> activities){}
 public record TranslationBody(@NotBlank @Size(max=250) String title,@NotNull JsonNode description,@Size(max=250) String seoTitle,@Size(max=500) String seoDescription){}
 public record CreateRequest(@NotBlank @Pattern(regexp="^[a-z0-9]+(?:-[a-z0-9]+)*$") @Size(max=120) String slug,boolean featured,@Min(0) @Max(100000) int sortOrder,@NotNull @Size(min=1,max=60) List<@Valid DayBody> days,@NotNull @Valid TranslationBody translation){}
 public record UpdateRequest(@NotBlank @Pattern(regexp="^[a-z0-9]+(?:-[a-z0-9]+)*$") @Size(max=120) String slug,boolean featured,@Min(0) @Max(100000) int sortOrder,@Min(1) @Max(60) int daysCount,@NotNull Long version){}
 public record ImagesRequest(Long cover,@Size(max=30) List<Long> gallery){}
 public record Activity(long id,int order,String targetType,long targetId){}
 public record Day(int dayNumber,List<Activity> activities){}
 public record Card(String slug,String title,int days,boolean featured,ImageDto cover,String language){}
 public record Detail(String slug,String title,JsonNode description,int days,ImageDto cover,List<ImageDto> gallery,List<Day> itinerary,String language){}
 public record AdminDetail(long id,long version,String slug,ContentStatus status,boolean featured,int sortOrder,int daysCount,Instant publishedAt,Instant updatedAt,AdminImages images,Map<String,TranslationBody> translations,List<Day> days){}
}
