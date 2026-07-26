package com.aionemu.gameserver.instance.handlers.scripts.pvparenas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.Test;

class PvPArenaMigrationTest {
	private static final Path HANDLERS = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/pvparenas");
	private static final Path CONDITION_SPAWNS = Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
	private static final Path INSTANCE_SPAWNS = Path.of("src/main/resources/aion/data/static_data/spawns/Instances");
	private static final Path GATHER_SPAWNS = Path.of("src/main/resources/aion/data/static_data/spawns/Gather");

	@Test
	void arenaChildrenCannotRestoreHardcodedSettlement() throws IOException {
		for (String name : List.of("ArenaOfChaosInstance.java", "ChaosTrainingGroundsInstance.java",
				"ArenaOfDisciplineInstance.java", "DisciplineTrainingGroundsInstance.java",
				"ArenaOfGloryInstance.java", "ArenaOfHarmonyInstance.java",
				"HarmonyTrainingGroundInstance.java", "UnityTrainingGroundInstance.java")) {
			String source = Files.readString(HANDLERS.resolve(name));
			assertFalse(source.contains("protected void reward()"), name);
			assertFalse(source.contains("AbyssPointsService"), name);
			assertFalse(source.contains("getGloryRewardRate()"), name);
		}
	}

	@Test
	void harmonyUsesRetailTimingLedgerAndTeamSettlement() throws IOException {
		String source = Files.readString(HANDLERS.resolve("HarmonyArenaInstance.java"));
		assertTrue(source.contains("getWaitTimeSeconds()"));
		assertTrue(source.contains("getStageTimeSeconds()"));
		assertTrue(source.contains("getKillScore()"));
		assertTrue(source.contains("getDeathScore(rank)"));
		assertTrue(source.contains("getStageEndBuffId("));
		assertTrue(source.contains("getStageEndBuffTargetRank("));
		assertTrue(source.contains("getHarmonyRewardRate()"));
		assertTrue(source.contains("InstanceSettlementService.queue("));
		assertTrue(source.contains("InstanceSettlementService.settle("));
		assertFalse(source.contains("instanceReward.removePlayerReward("));
		assertFalse(source.contains("AbyssPointsService"));
		assertFalse(source.contains("ItemService"));
		assertFalse(source.contains("120000"));
		assertFalse(source.contains("}, 10000);"));
	}

	@Test
	void arenaRewardPacketKeepsRetailFixedBlockSize() throws IOException {
		String packet = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_INSTANCE_SCORE.java"));
		int helperStart = packet.indexOf("private void writeArenaReward(");
		int helperEnd = packet.indexOf("private void fillTableWithGroup(", helperStart);
		String helper = packet.substring(helperStart, helperEnd);
		assertEquals(19, helper.split("writeD\\(", -1).length - 1);
		assertTrue(packet.contains("new byte[92 * (12 - playerCount)]"));
		assertTrue(packet.contains("new byte[76]"));
		assertTrue(packet.contains("writeArenaReward(harmonyPlayerReward)"));
	}

	@Test
	void arenaHandlersUseRecoverableDeadlinesAndRetailScores() throws IOException {
		for (String name : List.of("PvPArenaInstance.java", "HarmonyArenaInstance.java")) {
			String source = Files.readString(HANDLERS.resolve(name));
			assertTrue(source.contains("runtimeState().put(STATE + \"phase\""), name);
			assertTrue(source.contains("scheduleDeadline(\"prepare\""), name);
			assertTrue(source.contains("scheduleDeadline(\"round\""), name);
			assertTrue(source.contains("scheduleDeadline(\"exit\""), name);
			assertTrue(source.contains("DataManager.RETAIL_AI_DATA.getNpcScore"), name);
			assertFalse(source.contains("GameThreadPoolServices"), name);
			assertFalse(source.contains("private int getNpcBonus(int npcId) {\n\t\tswitch"), name);
		}
		String solo = Files.readString(HANDLERS.resolve("PvPArenaInstance.java"));
		assertFalse(solo.contains("ItemService.addItem"));
		assertFalse(solo.contains("186000454"));
	}

	@Test
	void harmonyConditionsOwnRetailActorsAndStaticSpawnsAreEmpty() throws IOException {
		String conditions = Files.readString(CONDITION_SPAWNS);
		int[] worlds = { 300450000, 300570000, 301100000 };
		int[] slots = { 693, 691, 691 };
		int[] sensoryAreas = { 41, 40, 40 };
		for (int i = 0; i < worlds.length; i++) {
			String world = world(conditions, worlds[i]);
			assertEquals(134, count(world, "<condition "));
			assertEquals(slots[i], count(world, "<slot>"));
			assertEquals(36, count(world, "id=\"207101\""));
			assertEquals(sensoryAreas[i], count(world, "<sensory_area "));
			assertEquals(7, count(world, "id=\"207102\""));
			assertEquals(4, count(world, "id=\"219272\""));
			assertEquals(1, count(world, "id=\"207106\""));
			assertEquals(2, count(world, "id=\"207107\""));
			assertEquals(1, count(world, "id=\"207117\""));
			assertTrue(world.contains("s1_lever == 1"));
			assertTrue(world.contains("s2_track"));
		}
		for (String name : List.of("300450000_Arena_Of_Harmony.xml", "300570000_Harmony_Training_Grounds.xml",
				"301100000_Unity_Training_Grounds.xml")) {
			String source = Files.readString(Path.of("src/main/resources/aion/data/static_data/spawns/Instances", name));
			assertFalse(source.contains("<spawn "), name);
		}
		for (String name : List.of("HarmonyTrainingGroundInstance.java", "UnityTrainingGroundInstance.java")) {
			assertFalse(Files.readString(HANDLERS.resolve(name)).contains("FlyRing"), name);
		}
	}

	@Test
	void harmonyRetailScoresUseUniquePersistentTeamPath() throws IOException {
		String source = Files.readString(HANDLERS.resolve("HarmonyArenaInstance.java"));
		for (String required : List.of(
			"case 207099, 207101, 207102, 207116, 207117", "219277, 219278, 219279",
			"219328, 219481, 219485, 219486", "243678, 243679, 243680")) {
			assertTrue(source.contains(required), required);
		}
		assertFalse(source.contains("consumedScoreNpcs"));
		int start = source.indexOf("public synchronized boolean onRetailNpcScore(");
		String score = source.substring(start, source.indexOf("private int getTime()", start));
		assertTrue(score.contains("npc.getSpawn().getStableKey()"));
		assertTrue(score.contains("runtimeState().put(eventKey, true)"));
		assertTrue(score.contains("restoreGroup(group)"));
		assertTrue(score.contains("group.addPoints(points)"));
		assertTrue(score.contains("persistGroup(group)"));
		assertTrue(score.contains("sendSystemMsg(player, npc, points)"));
		assertTrue(score.contains("instanceReward.sendPacket(10, player.getObjectId())"));
		assertTrue(score.contains("finishBattle(true)"));

		String first = HarmonyArenaInstance.scoreEventKey("condition:1:generation.1", 100);
		assertEquals(first, HarmonyArenaInstance.scoreEventKey("condition:1:generation.1", 101));
		assertFalse(first.equals(HarmonyArenaInstance.scoreEventKey("condition:1:generation.2", 100)));
		assertEquals("arena.score.event.object.100", HarmonyArenaInstance.scoreEventKey(null, 100));

		String death = source.substring(source.indexOf("public void onDie(Npc npc)"),
			source.indexOf("protected void sendSystemMsg", source.indexOf("public void onDie(Npc npc)")));
		assertTrue(death.contains("supportsRetailNpcScore(npc.getNpcId(), 0)"));
		String interaction = source.substring(source.indexOf("public void handleUseItemFinish(Player player, Npc npc)"),
			source.indexOf("private void scheduleRound()", source.indexOf("public void handleUseItemFinish(Player player, Npc npc)")));
		assertTrue(interaction.contains("supportsRetailNpcScore(npc.getNpcId(), 0)"));
	}

	@Test
	void soloArenaRetailSpawnsMatchMapFamilyOwnership() throws Exception {
		String conditions = Files.readString(CONDITION_SPAWNS);
		for (int worldId : new int[] { 300350000, 300420000 }) {
			String world = world(conditions, worldId);
			assertEquals(5, count(world, "<variable "));
			assertEquals(10, count(world, "<condition "));
			assertEquals(10, count(world, "<slot>"));
		}
		assertFalse(conditions.contains("<world id=\"300360000\""));
		assertFalse(conditions.contains("<world id=\"300430000\""));
			String glory = world(conditions, 300550000);
			assertEquals(2, count(glory, "<variable "));
			assertEquals(21, count(glory, "<condition "));
			assertEquals(21, count(glory, "<slot>"));
			assertEquals(17, count(glory, "source=\"idarena_glory/world_N.xml#unconditional-random-"));
			assertEquals(2, count(glory, "<party probability=\"5000\""));
			assertEquals(7, count(glory, "id=\"207102\""));
			for (int npcId : new int[] { 219502, 219503, 219504, 219540, 219541, 219542, 219653, 219654, 243675, 243676 }) {
				assertEquals(8, count(glory, "id=\"" + npcId + "\""), Integer.toString(npcId));
			}
			for (int npcId : new int[] { 701216, 701221, 701226 }) {
				assertEquals(4, count(glory, "id=\"" + npcId + "\""), Integer.toString(npcId));
			}
			assertEquals(8, count(glory, "id=\"701852\""));
			for (int page : new int[] { 1, 11, 21, 31, 41 }) {
				assertEquals(3, count(glory, "page_start=\"" + page + "\" page_end=\"" + page + "\""));
			}
			String gloryPools = glory.substring(glory.indexOf("<condition id=\"300550001\""));
			assertEquals("b107c1bfc7a14c69541516e7eb8bc5288d71161bea36a90d74d7b3c2989d1ddd",
				HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(gloryPools.getBytes(StandardCharsets.UTF_8))));

		for (String name : List.of("300350000_Arena_Of_Chaos.xml", "300420000_Chaos_Training_Grounds.xml")) {
			String source = Files.readString(INSTANCE_SPAWNS.resolve(name));
			assertFalse(source.contains("npc_id=\"207102\""), name);
			for (int npcId : new int[] { 701169, 701170, 701171, 701172, 701212 }) {
				assertTrue(source.contains("npc_id=\"" + npcId + "\""), name + ":" + npcId);
			}
			for (String spawn : List.of(
					"<spawn npc_id=\"701181\" respawn_time=\"45\" spawn_page=\"1\" initial_delay=\"1\">",
					"<spawn npc_id=\"701195\" respawn_time=\"45\" spawn_page=\"11\" initial_delay=\"1\">",
					"<spawn npc_id=\"701209\" respawn_time=\"45\" spawn_page=\"21\" initial_delay=\"1\">",
					"<spawn npc_id=\"701317\" respawn_time=\"30\" spawn_page=\"1\" initial_delay=\"1\">",
					"<spawn npc_id=\"701318\" respawn_time=\"30\" spawn_page=\"11\" initial_delay=\"1\">",
					"<spawn npc_id=\"701319\" respawn_time=\"30\" spawn_page=\"21\" initial_delay=\"1\">",
					"<spawn npc_id=\"701842\" respawn_time=\"45\" spawn_page=\"31\" initial_delay=\"1\">",
					"<spawn npc_id=\"701848\" respawn_time=\"30\" spawn_page=\"31\" initial_delay=\"1\">")) {
				assertTrue(source.contains(spawn), name + ":" + spawn);
			}
			String gather = Files.readString(GATHER_SPAWNS.resolve(name));
			assertTrue(gather.contains("npc_id=\"405000\""), name);
			assertTrue(gather.contains("npc_id=\"405001\""), name);
		}
		for (String name : List.of("300360000_Arena_Of_Discipline.xml", "300430000_Discipline_Training_Grounds.xml")) {
			String source = Files.readString(INSTANCE_SPAWNS.resolve(name));
			for (int npcId : new int[] { 207102, 243675, 243676 }) {
				assertTrue(source.contains("npc_id=\"" + npcId + "\""), name + ":" + npcId);
			}
			assertFalse(Files.exists(GATHER_SPAWNS.resolve(name)), name);
		}
			String glorySpawns = Files.readString(INSTANCE_SPAWNS.resolve("300550000_Arena_Of_Glory.xml"));
			for (int npcId : new int[] { 207047, 218710, 218789, 218793 }) {
				assertTrue(glorySpawns.contains("npc_id=\"" + npcId + "\""), "300550000:" + npcId);
			}
			for (int npcId : new int[] { 218757, 243675, 243676 }) {
				assertFalse(glorySpawns.contains("npc_id=\"" + npcId + "\""), "300550000:" + npcId);
			}
			assertFalse(Files.exists(GATHER_SPAWNS.resolve("300550000_Arena_Of_Glory.xml")));
	}

	@Test
	void soloArenaScoresUseUniquePersistentOwnerPath() throws IOException {
		String source = Files.readString(HANDLERS.resolve("PvPArenaInstance.java"));
		int supportStart = source.indexOf("public boolean supportsRetailNpcScore(");
		String support = source.substring(supportStart, source.indexOf("public synchronized boolean onRetailNpcScore(", supportStart));
		for (int npcId : new int[] { 207102, 219502, 219503, 219504, 219540, 219541, 219542, 219653, 219654,
				243675, 243676, 701173, 701174, 701181, 701187, 701188, 701195, 701209, 701216, 701221, 701226,
				701317, 701318, 701319, 701842, 701848, 701852 }) {
			assertTrue(support.contains(Integer.toString(npcId)), Integer.toString(npcId));
		}
		assertFalse(support.contains("701169"));
		assertFalse(support.contains("701212"));

		int scoreStart = source.indexOf("public synchronized boolean onRetailNpcScore(");
		String score = source.substring(scoreStart, source.indexOf("public InstanceReward", scoreStart));
		assertTrue(score.contains("npc.getSpawn().getStableKey()"));
		assertTrue(score.contains("runtimeState().put(eventKey, true)"));
		assertTrue(score.contains("reward.addPoints(points)"));
		assertTrue(score.contains("persistPlayer(reward)"));
		assertTrue(score.contains("spawnBlessedRelics(30000)"));
		assertTrue(score.contains("spawnCursedRelics(30000)"));
		assertTrue(score.contains("instanceReward.hasCapPoints()"));

		String first = PvPArenaInstance.scoreEventKey("spawn:1:generation.1", 100);
		assertEquals(first, PvPArenaInstance.scoreEventKey("spawn:1:generation.1", 101));
		assertFalse(first.equals(PvPArenaInstance.scoreEventKey("spawn:1:generation.2", 100)));
		assertEquals("arena.score.event.object.100", PvPArenaInstance.scoreEventKey(null, 100));

		String death = source.substring(source.indexOf("public void onDie(Npc npc)"),
			source.indexOf("public void onEnterInstance", source.indexOf("public void onDie(Npc npc)")));
		assertTrue(death.contains("npc.getAi2() instanceof RetailPatternAI2"));
		assertTrue(death.contains("supportsRetailNpcScore(npc.getNpcId(), 0)"));
		String interaction = source.substring(source.indexOf("public void handleUseItemFinish(Player player, Npc npc)"),
			source.indexOf("private void scheduleRound()", source.indexOf("public void handleUseItemFinish(Player player, Npc npc)")));
		assertTrue(interaction.contains("npc.getAi2() instanceof RetailPatternAI2"));
		assertTrue(interaction.contains("supportsRetailNpcScore(npc.getNpcId(), 0)"));

		for (String name : List.of("ArenaOfChaosInstance.java", "ChaosTrainingGroundsInstance.java")) {
			assertTrue(Files.readString(HANDLERS.resolve(name)).contains("persistPlayer(reward)"), name);
		}
		String training = Files.readString(HANDLERS.resolve("ChaosTrainingGroundsInstance.java"));
		String rings = training.substring(training.indexOf("public boolean onPassFlyingRing("));
		assertEquals(3, count(rings, "playerReward.addPoints(250)"));
		assertEquals(3, count(rings, "persistPlayer(playerReward)"));
		for (String name : List.of("ArenaOfDisciplineInstance.java", "DisciplineTrainingGroundsInstance.java")) {
			assertFalse(Files.readString(HANDLERS.resolve(name)).contains("onGather("), name);
		}
	}

	private static String world(String source, int id) {
		int start = source.indexOf("<world id=\"" + id + "\"");
		assertTrue(start >= 0, Integer.toString(id));
		return source.substring(start, source.indexOf("</world>", start));
	}

	private static int count(String source, String token) {
		int count = 0;
		for (int at = 0; (at = source.indexOf(token, at)) >= 0; at += token.length()) {
			count++;
		}
		return count;
	}
}
