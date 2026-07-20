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

	@Test
	void restoresPersistentArenaState() {
		PvPArenaPlayerReward reward = new PvPArenaPlayerReward(1, 0, 0, 12_000, (byte) 7);
		reward.restore(8_000, 3, 4);
		reward.restoreAbsence(1_500, 2_000);
		reward.restoreFinalScore(6_000, 50);

		assertEquals(8_000, reward.getPoints());
		assertEquals(3, reward.getPvPKills());
		assertEquals(4, reward.getMonsterKills());
		assertEquals(1_500, reward.getAbsentMillis());
		assertEquals(2_000, reward.getAbsenceStartedAt());
		assertEquals(14_000, reward.getScorePoints());
		assertEquals(50, reward.getParticipationPercent());
	}
}
