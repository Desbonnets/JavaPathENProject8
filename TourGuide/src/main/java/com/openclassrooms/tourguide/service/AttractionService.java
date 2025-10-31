package com.openclassrooms.tourguide.service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AttractionService {

    private final GpsUtil gpsUtil;
    private volatile List<Attraction> cachedAttractions;

    public AttractionService(GpsUtil gpsUtil) {
        this.gpsUtil = gpsUtil;
    }

    @Cacheable("attractions")
    public List<Attraction> getAttractions() {
        if (cachedAttractions == null) {
            synchronized (this) {
                if (cachedAttractions == null) {
                    cachedAttractions = gpsUtil.getAttractions();
                }
            }
        }
        return cachedAttractions;
    }

    // Rafraîchit les attractions toutes les 6 heures en arrière-plan
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
    public void refreshCache() {
        CompletableFuture.runAsync(() -> {
            cachedAttractions = gpsUtil.getAttractions();
        });
    }
}
