package com.kampotourism.itinerary;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ItineraryActivityRepository extends JpaRepository<ItineraryActivity,Long>{ List<ItineraryActivity> findByDayIdInOrderBySortOrderAscIdAsc(Collection<Long> ids); void deleteByDayIdIn(Collection<Long> ids); boolean existsByDestinationId(Long destinationId); boolean existsByExperienceId(Long experienceId); }
