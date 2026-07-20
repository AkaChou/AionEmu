package com.aionemu.gameserver.model.instance.playerreward;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData;

class PvPArenaPlayerRewardTest {
	@BeforeAll
	static void initializeBuffData() {
		if (DataManager.RETAIL_INSTANCE_DATA == null) {
			DataManager.RETAIL_INSTANCE_DATA = RetailInstanceData.load(
				new File("src/main/resources/aion/definitions/compact/instance"),
				new File("src/main/resources/aion/definitions/schemas/retail-instance-data.xsd"));
		}
	}

	@Test
	void enforcesScoreFloorAndCalculatesExactPlaytimeBonus() {
		PvPArenaPlayerReward reward = new PvPArenaPlayerReward(1, 13_000, 10_000, 12_000, (byte) 7);
		reward.addPoints(-5_000);
		assertEquals(10_000, reward.getPoints());

		reward.beginAbsence(1_000);
		reward.endAbsence(2_500);
		reward.finalizePlaytimeBonus(6_000, 7_000);
		assertEquals(9_000, reward.getTimeBonus());
		assertEquals(75, reward.getParticipationPercent());
		assertEquals(19_000, reward.getScorePoints());
	}

	@Test
	void includesAnOpenAbsenceAtSettlement() {
		PvPArenaPlayerReward reward = new PvPArenaPlayerReward(1, 0, 0, 12_000, (byte) 7);
		reward.beginAbsence(1_000);
		reward.finalizePlaytimeBonus(6_000, 4_000);
		assertEquals(6_000, reward.getTimeBonus());
		assertEquals(50, reward.getParticipationPercent());
	}
}
