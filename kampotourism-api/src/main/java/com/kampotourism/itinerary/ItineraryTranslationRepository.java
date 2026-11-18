package com.kampotourism.itinerary;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ItineraryTranslationRepository extends JpaRepository<ItineraryTranslation,Long>{ List<ItineraryTranslation> findByItineraryIdInAndLangIn(Collection<Long> ids,Collection<String> langs); List<ItineraryTranslation> findByItineraryId(Long id); Optional<ItineraryTranslation> findByItineraryIdAndLang(Long id,String lang); void deleteByItineraryIdAndLang(Long id,String lang); }
