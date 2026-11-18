package com.kampotourism.itinerary;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ItineraryDayRepository extends JpaRepository<ItineraryDay,Long>{ List<ItineraryDay> findByItineraryIdOrderByDayNumberAsc(Long id); void deleteByItineraryId(Long id); }
