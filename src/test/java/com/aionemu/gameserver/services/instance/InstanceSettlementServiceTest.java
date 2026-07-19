package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.ArenaReward;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.BattleResult;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardItem;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;

class InstanceSettlementServiceTest {
	private static final Pattern CHECK_RANK = Pattern.compile(
			"private int checkRank\\(int totalPoints(?:, long startAt, long finishAt)?\\) \\{(.*?)\\n\\s*}",
			Pattern.DOTALL);
	private static final List<String> TIME_ATTACK_HANDLERS = List.of(
			"SealedArgentManorInstance.java",
			"TheEternalBastionInstance.java",
			"SmolderingFireTempleInstance.java",
			"FissureOfOblivionInstance.java",
			"StonespearReachInstance.java",
			"event/Event_ContaminatedUnderpathInstance.java",
			"event/Opportunity_FissureOfOblivionInstance.java");

	@BeforeAll
	static void loadRetailData() {
		DataManager.RETAIL_INSTANCE_DATA = RetailInstanceData.load(
				new File("src/main/resources/aion/definitions/compact/instance"),
				new File("src/main/resources/aion/definitions/schemas/retail-instance-data.xsd"));
	}

	@Test
	void buildsTimeAttackRewardsFromRetailTable() {
		RewardPlan rankS = InstanceSettlementService.timeAttackPlan(301510000, 1);
		assertEquals(14_000, rankS.ap());
		assertEquals(List.of(new RewardItem(188054114, 1)), rankS.items());

		RewardPlan rankA = InstanceSettlementService.timeAttackPlan(301510000, 2);
		assertEquals(12_000, rankA.ap());
		assertEquals(List.of(new RewardItem(188054115, 1)), rankA.items());
	}

	@Test
	void buildsLunaRewardsFromRetailTimeAttackRows() {
		assertEquals(InstanceSettlementService.timeAttackPlan(301630000, 1),
				InstanceSettlementService.lunaPlan(301630000, 1));
		assertEquals(InstanceSettlementService.timeAttackPlan(301640000, 1),
				InstanceSettlementService.lunaPlan(301640000, 1));
		assertThrows(IllegalStateException.class, () -> InstanceSettlementService.lunaPlan(301510000, 1));
	}

	@Test
	void calculatesTimeAttackRankFromRetailScoreAndTime() {
		assertEquals(1, InstanceSettlementService.timeAttackRank(301510000, 16_000, 750));
		assertEquals(2, InstanceSettlementService.timeAttackRank(301510000, 16_000, 780));
		assertEquals(3, InstanceSettlementService.timeAttackRank(301510000, 11_500, 840));
		assertEquals(6, InstanceSettlementService.timeAttackRank(301510000, 8_099, 1));
	}

	@Test
	void loadsTimeAttackDurationsFromRetailTable() {
		assertEquals(60, InstanceSettlementService.timeAttackWaitSeconds(301510000));
		assertEquals(900, InstanceSettlementService.timeAttackLimitSeconds(301510000));
		assertEquals(100, InstanceSettlementService.timeAttackWaitSeconds(302000000));
		assertEquals(480, InstanceSettlementService.timeAttackLimitSeconds(302000000));
	}

	@Test
	void buildsInfinityRewardsFromRetailTable() {
		assertEquals(2_000_000, InstanceSettlementService.infinityPlan(2).kinah());
		RewardPlan floorFour = InstanceSettlementService.infinityPlan(4);
		assertEquals(20, floorFour.gp());
		assertEquals(List.of(new RewardItem(188057798, 1)), floorFour.items());
	}

	@Test
	void buildsTournamentRewardsFromRetailRounds() {
		RewardPlan first = InstanceSettlementService.tournamentPlan(DataManager.RETAIL_INSTANCE_DATA.tournament(1), 1);
		assertEquals(1_000, first.exp());
		assertEquals(30, first.gp());
		assertEquals(List.of(new RewardItem(166000025, 1), new RewardItem(166000027, 3)), first.items());

		RewardPlan champion = InstanceSettlementService.tournamentPlan(DataManager.RETAIL_INSTANCE_DATA.tournament(2), 6);
		assertEquals(50_000_000, champion.exp());
		assertEquals(5_000_000, champion.kinah());
		assertEquals(38_616, champion.ap());
		assertEquals(600, champion.gp());
		assertEquals(List.of(new RewardItem(186000454, 2)), champion.items());
		assertThrows(IllegalArgumentException.class,
				() -> InstanceSettlementService.tournamentPlan(DataManager.RETAIL_INSTANCE_DATA.tournament(2), 7));
	}

	@Test
	void buildsBattlegroundRewardsFromRetailResultAndContribution() {
		RewardPlan winner = InstanceSettlementService.battlegroundPlan(301120000, 0, BattleResult.WIN,
				1, 20_000, 0, 0);
		assertEquals(25_000, winner.ap());
		assertEquals(150, winner.gp());
		assertEquals(List.of(new RewardItem(186000236, 1), new RewardItem(188055442, 1),
				new RewardItem(188100391, 750)), winner.items());

		RewardPlan loser = InstanceSettlementService.battlegroundPlan(301120000, 0, BattleResult.LOSE,
				0.25, 0, 0, 0);
		assertEquals(5_812, loser.ap());
		assertEquals(List.of(new RewardItem(186000236, 1)), loser.items());

		assertThrows(IllegalStateException.class, () -> InstanceSettlementService.battlegroundPlan(302350000, 0,
				BattleResult.WIN, 1, 0, 0, 0));
	}

	@Test
	void appliesRetailBattlegroundResultTargetAndPopulationRules() {
		assertEquals(BattleResult.WIN, InstanceSettlementService.battlegroundResult(2, 1));
		assertEquals(BattleResult.DRAW, InstanceSettlementService.battlegroundResult(1, 1));
		assertEquals(0.375, InstanceSettlementService.battlegroundBonusRate(0.75, 1, 1));

		RewardPlan idgelWinner = InstanceSettlementService.battlegroundPlan(301310000, 0, BattleResult.WIN,
				0, 0, 1, 0);
		assertEquals(1, idgelWinner.itemCount(188053032));
		RewardPlan idgelLoser = InstanceSettlementService.battlegroundPlan(301310000, 0, BattleResult.LOSE,
				0, 0, 1, 0);
		assertEquals(0, idgelLoser.itemCount(188053032));

		RewardPlan evergale = InstanceSettlementService.battlegroundPlan(302350000, 1, BattleResult.WIN,
				0, 0, 0, 75);
		assertEquals(30, evergale.itemCount(186000472));
	}

	@Test
	void buildsTreasureIslandRewardsFromRetailTable() {
		RewardPlan winnerBase = InstanceSettlementService.battlegroundPlan(301700000, 0, BattleResult.WIN,
				0, 0, 0, 3);
		assertEquals(1_600, winnerBase.ap());
		assertEquals(50, winnerBase.gp());
		assertEquals(6, winnerBase.itemCount(185000320));

		RewardPlan winnerFull = InstanceSettlementService.battlegroundPlan(301700000, 0, BattleResult.WIN,
				1, 0, 0, 3);
		assertEquals(8_000, winnerFull.ap());
		assertEquals(6, winnerFull.itemCount(185000320));

		RewardPlan draw = InstanceSettlementService.battlegroundPlan(301700000, 0, BattleResult.DRAW,
				0, 0, 0, 3);
		assertEquals(320, draw.ap());
		assertEquals(10, draw.gp());
		assertEquals(4, draw.itemCount(185000320));
	}

	@Test
	void loadsAllRetailArenaRowsAndSelectsSpawnPageStrictly() {
		List<Row> rows = DataManager.RETAIL_INSTANCE_DATA.rewards("instant_dungeon_idarenapvp");
		assertEquals(40, rows.size());
		for (int worldId : List.of(300350000, 300360000, 300420000, 300430000,
				300450000, 300550000, 300570000, 301100000)) {
			assertEquals(5, rows.stream().filter(row -> row.requiredInt("world_id") == worldId).count());
		}
		assertEquals(300401, InstanceSettlementService.arenaRow(300450000, 31).requiredInt("id"));
		assertThrows(IllegalStateException.class, () -> InstanceSettlementService.arenaRow(300450000, 0));
	}

	@Test
	void calculatesRetailArenaRewardsForDisciplineChaosGloryAndHarmony() {
		ArenaReward discipline = InstanceSettlementService.arenaReward(
				InstanceSettlementService.arenaRow(300360000, 31), 0, 2, 15_000, 25_000, 1);
		assertEquals(1_892, discipline.plan().ap());
		assertEquals(1_117, discipline.plan().itemCount(186000130));
		assertEquals(82, discipline.plan().itemCount(186000137));

		ArenaReward chaos = InstanceSettlementService.arenaReward(
				InstanceSettlementService.arenaRow(300350000, 31), 0, 4, 40_000, 100_000, 1);
		assertEquals(2_056, chaos.plan().ap());
		assertEquals(1, chaos.plan().itemCount(186000185));

		ArenaReward glory = InstanceSettlementService.arenaReward(
				InstanceSettlementService.arenaRow(300550000, 31), 0, 4, 40_000, 100_000, 1);
		assertEquals(40_400, glory.plan().ap());
		assertEquals(630, glory.plan().itemCount(186000469));
		assertEquals(1, glory.plan().itemCount(182213259));

		ArenaReward harmony = InstanceSettlementService.arenaReward(
				InstanceSettlementService.arenaRow(300450000, 31), 0, 2, 15_000, 25_000, 1);
		assertEquals(4_250, harmony.plan().ap());
		assertEquals(54, harmony.plan().itemCount(186000137));
		assertEquals(1, harmony.plan().itemCount(188052605));
		assertEquals(1, harmony.plan().itemCount(188052482));
	}

	@Test
	void appliesArenaPopulationThresholdsAndTrainingGroundZeroRewards() {
		Row chaos = InstanceSettlementService.arenaRow(300350000, 31);
		assertEquals(0, InstanceSettlementService.arenaReward(chaos, 0, 1, 10_000, 10_000, 1)
				.plan().itemCount(186000185));
		assertEquals(1, InstanceSettlementService.arenaReward(chaos, 0, 2, 10_000, 20_000, 1)
				.plan().itemCount(186000185));

		ArenaReward harmonyTraining = InstanceSettlementService.arenaReward(
				InstanceSettlementService.arenaRow(300570000, 31), 0, 2, 13_000, 26_000, 1);
		assertEquals(0, harmonyTraining.plan().ap());
		assertEquals(0, harmonyTraining.plan().gp());
		assertTrue(harmonyTraining.plan().items().isEmpty());

		ArenaReward unityTraining = InstanceSettlementService.arenaReward(
				InstanceSettlementService.arenaRow(301100000, 31), 0, 2, 13_000, 26_000, 1);
		assertTrue(unityTraining.plan().items().isEmpty());
		assertThrows(IllegalArgumentException.class,
				() -> InstanceSettlementService.arenaReward(chaos, 2, 2, 10_000, 20_000, 1));
	}

	@Test
	void appliesRetailArenaTopScoreAndGapEndConditions() {
		Row chaos = InstanceSettlementService.arenaRow(300350000, 31);
		assertFalse(InstanceSettlementService.arenaScoreLimitReached(chaos, 49_000, 7_000));
		assertTrue(InstanceSettlementService.arenaScoreLimitReached(chaos, 50_000, 7_000));

		Row discipline = InstanceSettlementService.arenaRow(300360000, 31);
		assertTrue(InstanceSettlementService.arenaScoreLimitReached(discipline, 12_000, 10_500));
	}

	@Test
	void payloadIsCanonicalAndRoundTrips() {
		RewardPlan plan = new RewardPlan(List.of(new RewardItem(20, 2), new RewardItem(10, 1),
				new RewardItem(20, 3)), 4, 5, 6, 7);
		assertEquals(List.of(new RewardItem(10, 1), new RewardItem(20, 5)), plan.items());
		assertEquals(plan, RewardPlan.decode(plan.encode()));
		assertEquals(64, InstanceSettlementService.hash(plan.encode()).length());
	}

	@Test
	void migratedHandlersCannotRestoreHardcodedFinalRewardsOrRankThresholds() throws IOException {
		Path handlers = Path.of("src/main/java/com/aionemu/gameserver/instance/handlers/scripts");
		for (String relative : TIME_ATTACK_HANDLERS) {
			String source = Files.readString(handlers.resolve(relative));
			assertTrue(source.contains("InstanceSettlementService.settleTimeAttack("), relative);
			Matcher rank = CHECK_RANK.matcher(source);
			assertTrue(rank.find(), relative);
			assertTrue(rank.group(1).contains("InstanceSettlementService.timeAttackRank("), relative);
			assertFalse(rank.group(1).contains(">="), relative);
		}
		String shugoShared = Files.readString(handlers.resolve("ShugoVaultTimeAttackInstance.java"));
		assertTrue(shugoShared.contains("InstanceSettlementService.settleTimeAttack("));
		assertTrue(shugoShared.contains("InstanceSettlementService.timeAttackRank("));
		for (String relative : List.of("TheShugoEmperorVaultInstance.java", "EmperorTrillirunerkSafeInstance.java")) {
			String source = Files.readString(handlers.resolve(relative));
			assertTrue(source.contains("extends ShugoVaultTimeAttackInstance"), relative);
			assertFalse(source.contains("Future<?>"), relative);
			assertFalse(source.contains("checkRank("), relative);
		}
		for (String relative : List.of("luna/ContaminatedUnderpathInstance.java",
				"luna/SecretMunitionsFactoryInstance.java")) {
			String source = Files.readString(handlers.resolve(relative));
			assertTrue(source.contains("InstanceSettlementService.settleLuna("), relative);
			assertTrue(source.contains("InstanceSettlementService.lunaPlan("), relative);
			assertFalse(source.contains("InstanceSettlementService.settleTimeAttack("), relative);
		}

		String infinity = Files.readString(handlers.resolve("crucible/CrucibleSpireInstance.java"));
		assertTrue(infinity.contains("InstanceSettlementService.settleInfinity("));
		assertFalse(infinity.contains("TOWER_REWARD_DATA"));
		assertFalse(infinity.contains("TowerStageRewardTemplate"));
	}
}
