package com.aionemu.gameserver.services.toypet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.RateConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PetFeedData;
import com.aionemu.gameserver.utils.rates.PremiumRates;
import com.aionemu.gameserver.utils.rates.RegularRates;
import com.aionemu.gameserver.utils.rates.VipRates;

class PetFeedCalculatorTest {

	@Test
	void petFeedingRateSpeedsUpRegularFoodProgress() {
		DataManager.PET_FEED_DATA = new PetFeedData();
		PetFeedProgress regular = feed(5, 1);
		PetFeedProgress vip = feed(5, 3);

		assertEquals(PetHungryLevel.HUNGRY, regular.getHungryLevel());
		assertEquals(PetHungryLevel.FULL, vip.getHungryLevel());
	}

	@Test
	void membershipRatesExposePetFeedingConfig() {
		RateConfig.PET_FEEDING_RATE = 1;
		RateConfig.PREMIUM_PET_FEEDING_RATE = 2;
		RateConfig.VIP_PET_FEEDING_RATE = 3;

		assertEquals(1, new RegularRates().getPetFeedingRate());
		assertEquals(2, new PremiumRates().getPetFeedingRate());
		assertEquals(3, new VipRates().getPetFeedingRate());
	}

	private PetFeedProgress feed(int times, float rate) {
		PetFeedProgress progress = new PetFeedProgress((short) 0);
		for (int i = 0; i < times; i++) {
			PetFeedCalculator.updatePetFeedProgress(progress, 30, 10, rate);
		}
		return progress;
	}
}
