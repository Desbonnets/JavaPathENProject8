package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final RewardCentral rewardsCentral;
    private final AttractionService attractionService;
    private Executor executor;

	public RewardsService(RewardCentral rewardCentral, AttractionService attractionService) {
		this.rewardsCentral = rewardCentral;
        this.attractionService = attractionService;
        this.executor = Executors.newFixedThreadPool(100);
	}

    public Executor getExecutor() {
        return executor;
    }
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

    public void calculateRewards(User user) {
        List<VisitedLocation> userLocations = user.getVisitedLocations();
        List<Attraction> attractions = attractionService.getAttractions();

        // Cache local pour éviter les doublons
        Set<String> rewardedAttractions = user.getUserRewards().stream()
                .map(r -> r.attraction.attractionName)
                .collect(Collectors.toSet());

        List<CompletableFuture<Void>> futures = userLocations.stream()
                .flatMap(visitedLocation -> attractions.stream()
                        .filter(a -> !rewardedAttractions.contains(a.attractionName))
                        .filter(a -> nearAttraction(visitedLocation, a))
                        .map(a -> getRewardPoints(a, user)
                                .thenApply(points -> new UserReward(visitedLocation, a, points))
                                .thenAccept(reward -> {
                                    synchronized(user) {
                                        user.addUserReward(reward);
                                        rewardedAttractions.add(a.attractionName);
                                    }
                                })
                        )
                ).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();


//        for (VisitedLocation visitedLocation : userLocations) {
//            // Filtrage : on ne garde que les attractions proches pour réduire la charge CPU
//            List<Attraction> nearbyAttractions = attractions.stream()
//                    .filter(attraction -> nearAttraction(visitedLocation, attraction))
//                    .toList();
//
//
//
//            for (Attraction attraction : nearbyAttractions) {
//                if (!rewardedAttractions.contains(attraction.attractionName)) {
//                    CompletableFuture<Integer> points = getRewardPoints(attraction, user);
//                    user.addUserReward(new UserReward(visitedLocation, attraction, points.join()));
//                    rewardedAttractions.add(attraction.attractionName);
//                }
//            }
//        }
    }


    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

    private CompletableFuture<Integer> getRewardPoints(Attraction attraction, User user) {
        return CompletableFuture.supplyAsync(() ->
                rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId())
        );
    }
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
