package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.service.AttractionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.service.RewardsService;

@Configuration
public class TourGuideModule {
	
	@Bean
	public GpsUtil getGpsUtil() {
		return new GpsUtil();
	}
	
	@Bean
	public RewardsService getRewardsService() {
		return new RewardsService(getRewardCentral(), new AttractionService(getGpsUtil()));
	}
	
	@Bean
	public RewardCentral getRewardCentral() {
		return new RewardCentral();
	}
	
}
