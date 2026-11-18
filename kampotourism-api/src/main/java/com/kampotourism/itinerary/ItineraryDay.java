package com.kampotourism.itinerary;
import jakarta.persistence.*;
@Entity @Table(name="itinerary_day") public class ItineraryDay { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false) private Long itineraryId; private int dayNumber; protected ItineraryDay(){} ItineraryDay(Long itineraryId,int dayNumber){this.itineraryId=itineraryId;this.dayNumber=dayNumber;} public Long getId(){return id;} public Long getItineraryId(){return itineraryId;} public int getDayNumber(){return dayNumber;} }
