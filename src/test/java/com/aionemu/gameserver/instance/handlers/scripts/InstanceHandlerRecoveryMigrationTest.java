package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class InstanceHandlerRecoveryMigrationTest {

	@Test
	void migratedHandlersUsePersistentDeadlinesAndState() throws Exception {
		assertMigrated("AdmaStrongholdInstance", "scheduleDeadline(\"pot\"", "adma.complete");
		assertMigrated("PadmarashkaCaveInstance", "scheduleDeadline(\"expire\"", "padma.protectors");
		assertMigrated("CradleOfEternityInstance", "scheduleDeadline(\"start\"", "cradle.covetous_complete");
		assertMigrated("TransidiumAnnexInstance", "scheduleDeadline(\"start\"", "transidium.hangar_barricade");
		assertMigrated("TheobomosLabInstance", "scheduleDeadline(\"stone\"", "theobomos.ifrit_deadline");
		assertMigrated("DraupnirCaveInstance", "scheduleDeadline(\"gate_raid_2\"", "draupnir.adjutants");
		assertMigrated("crucible/CrucibleChallengeInstance", "scheduleDeadline(\"bonus_spawn\"",
				"crucible.bonus_spawning_done");
		assertMigrated("LinkgateFoundryInstance", "scheduleDeadline(\"expire\"",
				"linkgate.expire_deadline");
		assertMigrated("DrakenseerLairInstance", "scheduleDeadline(\"expire\"",
				"drakenseer.enhancers");
		assertMigrated("RightWingChamberInstance", "scheduleDeadline(\"chests\"",
				"rightwing.exit_deadline");
		assertMigrated("LeftWingChamberInstance", "scheduleDeadline(\"chest\"",
				"leftwing.next_deadline");
		assertMigrated("TheHexwayInstance", "scheduleDeadline(\"chest\"",
				"hexway.next_deadline");
		assertMigrated("LowerUdasTempleInstance", "scheduleDeadline(\"chest\"",
				"lower_udas.next_deadline");
		assertMigrated("AbyssStoreroomInstance", "scheduleDeadline(\"barrier_\"",
				"storeroom.next_deadline");
		assertMigrated("SealedArgentManorInstance", "scheduleDeadline(\"expire\"",
				"sealed.resistance_skill");
		assertSourceExcludes("SealedArgentManorInstance", "GameThreadPoolServices");
		assertMigrated("SmolderingFireTempleInstance", "scheduleDeadline(\"expire\"",
				"smolder.kill.");
		assertSourceExcludes("SmolderingFireTempleInstance", "GameThreadPoolServices");
		assertNoFuture("AbyssalSplinterInstance");
		assertNoFuture("UnstableAbyssalSplinterInstance");
		assertNoFuture("GraveOfSteelStoreroomInstance");
		assertNoFuture("IsleOfRootsStoreroomInstance");
		assertNoFuture("TwilightBattlefieldStoreroomInstance");
		assertNoFuture("TalocsHollowInstance");
		assertNoFuture("BeshmundirTempleInstance");
		assertNoFuture("KumukiCaveInstance");
		assertMigrated("ShugoVaultTimeAttackInstance", "scheduleDeadline(\"expire\"", "idsweep.");
		assertSourceContains("ShugoVaultTimeAttackInstance", "RetailConditionSpawnEngine.setVariable");
		assertSourceContains("ShugoVaultTimeAttackInstance", "S_REWARD_VARIABLE");
		assertSourceContains("ShugoVaultTimeAttackInstance", "consumeConditionSpawnDeath");
		assertSourceContains("ShugoVaultTimeAttackInstance", "getRespawnTime() > 0");
		assertSourceContains("ShugoVaultTimeAttackInstance", "Float.floatToIntBits");
		assertSourceExcludes("ShugoVaultTimeAttackInstance", "GameThreadPoolServices");
		assertNoFuture("TheShugoEmperorVaultInstance");
		assertNoFuture("EmperorTrillirunerkSafeInstance");
	}

	@Test
	void divineTowersUseRetailMatchAndAiWithoutEmptyHandlers() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DivineTowerInstanceL.java")));
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DivineTowerInstanceD.java")));

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		for (String worldId : new String[] { "310160000", "320160000" }) {
			assertTrue(Pattern.compile("<world\\b(?=[^>]*\\bid=\\\"" + worldId
				+ "\\\")(?=[^>]*\\bbehavior=\\\"MATCHMAKER\\\")[^>]*/>").matcher(coverage).find());
		}
	}

	@Test
	void idsweepUsesRetailConditionClosureAndRemovesSafeStaticCombatSpawns() throws Exception {
		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String vault = worldBlock(conditions, "301400000");
		String safe = worldBlock(conditions, "301590000");
		assertTrue(vault.contains("1STAGE_2START"));
		assertTrue(vault.contains("4STAGE_ELITE"));
		assertTrue(safe.contains("SpecialServer_Cond == 0"));
		assertTrue(safe.contains("SpecialServer_Cond == 1"));
		assertTrue(safe.contains("npc id=\"246773\""));
		assertTrue(safe.contains("npc id=\"244061\""));
		assertTrue(safe.contains("npc id=\"246773\""));
		assertTrue(vault.contains("npc id=\"235647\""));
		assertTrue(vault.contains("IDSweep_Reward"));
		assertTrue(vault.contains("npc id=\"832932\""));
		assertTrue(safe.contains("IDSweep_Reward_S"));
		assertTrue(safe.contains("npc id=\"832932\""));
		assertEquals(221, count(vault, "<condition "));
		assertEquals(410, count(safe, "<condition "));

		String staticSpawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/301590000_Emperor_Trillirunerk_Safe.xml"));
		assertFalse(staticSpawns.matches("(?s).*npc_id=\"235[0-9]+\".*"));
	}

	@Test
	void udasUsesRetailConditionClosureAndRemovesStaticConditionSpawns() throws Exception {
		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String udas = worldBlock(conditions, "300150000");
		assertTrue(udas.contains("<variable name=\"fanaticelnboss\"/>"));
		assertTrue(udas.contains("<variable name=\"teleporter_spawn\"/>"));
		assertTrue(udas.contains("SpecialServer_Cond == 0"));
		assertTrue(udas.contains("SpecialServer_Cond == 1"));
		assertTrue(udas.contains("FanaticElNBoss == 1"));
		assertTrue(udas.contains("Teleporter_Spawn == 1"));
		assertEquals(14, count(udas, "<condition "));

		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/UdasTempleInstance.java"));
		assertFalse(handler.contains("GameThreadPoolServices"));
		assertFalse(handler.contains("onDropRegistered"));
		assertFalse(handler.contains("onDie"));

		String staticSpawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/300150000_Udas_Temple.xml"));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"215787\" pool=\"1\">"));
		assertTrue(staticSpawns.contains("x=\"778.536682\" y=\"661.277710\""));
		assertTrue(staticSpawns.contains("x=\"689.528625\" y=\"669.004517\""));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"215788\">"));
		assertTrue(staticSpawns.contains("x=\"807.570984\" y=\"560.611877\""));
		for (String npcId : new String[] { "215782", "215783", "215793", "730217", "700706", "730272" }) {
			assertFalse(staticSpawns.contains("npc_id=\"" + npcId + "\""));
		}
	}

	@Test
	void admaFallUsesRetailConditionClosureAndRemovesLegacyHandlerSpawns() throws Exception {
		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String adma = worldBlock(conditions, "301600000");
		assertTrue(adma.contains("<variable name=\"sub_boss_die\"/>"));
		assertTrue(adma.contains("boss_summon == 1"));
		assertTrue(adma.contains("boss_summon_check == 4"));
		assertTrue(adma.contains("boss_summon == 2"));
		assertTrue(adma.contains("End_Boss_Die == 1"));
		assertEquals(6, count(adma, "<condition "));

		String staticSpawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/301600000_Adma's_Fall.xml"));
		assertFalse(staticSpawns.contains("npc_id=\"220427\""));
		assertTrue(staticSpawns.contains("npc_id=\"248974\""));
		assertFalse(Files.exists(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AdmaFallInstance.java")));
	}

	@Test
	void haramelUsesRetailAiAndDropsWithoutLegacyHandler() throws Exception {
		String staticSpawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/300200000_Haramel.xml"));
		assertTrue(staticSpawns.contains("npc_id=\"216922\""));
		assertFalse(staticSpawns.contains("npc_id=\"700829\""));
		assertFalse(staticSpawns.contains("npc_id=\"700852\""));
		assertFalse(Files.exists(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/HaramelInstance.java")));
		assertFalse(Files.exists(Path.of(
				"src/test/java/com/aionemu/gameserver/instance/handlers/scripts/HaramelInstanceTest.java")));

		String pattern = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns_ldf4_pjw.xml"));
		int hameroon = pattern.indexOf("<name>IDNovice_Hameroon</name>");
		assertTrue(hameroon >= 0);
		int nextPattern = pattern.indexOf("<npc_ai_pattern>", hameroon + 1);
		String hameroonPattern = pattern.substring(hameroon, nextPattern < 0 ? pattern.length() : nextPattern);
		assertTrue(hameroonPattern.contains("IDNovice_Chest_Fighter"));
		assertTrue(hameroonPattern.contains("IDNovice_Out"));
		assertTrue(hameroonPattern.contains("play_cutscene_by_user_indicator"));
	}

	@Test
	void nochsanaUsesRetailWorldFlowWithoutPrivateDrops() throws Exception {
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/NochsanaTrainingCampInstance.java"));
		assertFalse(handler.contains("onDropRegistered"));
		assertFalse(handler.contains("188053787"));
		assertFalse(handler.contains("188051138"));
		assertTrue(handler.contains("npc.getNpcId() == 700437"));
		assertTrue(handler.contains("getSkill(npc, 276, 10, player)"));

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300030000_Nochsana_Training_Camp.xml"));
		assertTrue(staticSpawns.contains("npc_id=\"700437\""));
		assertTrue(staticSpawns.contains("npc_id=\"700438\""));
		assertTrue(Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_019.xml"))
			.contains("npc_id=\"256693\""));
	}

	@Test
	void alquimiaUsesRetailDropsWithoutLegacyHandler() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AlquimiaResearchCenterInstance.java")));

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_004.xml"));
		assertTrue(npcDropBlock(drops, "214027").contains("item_id=\"185000006\" chance=\"100.00\""));
		assertTrue(npcDropBlock(drops, "214034").contains("item_id=\"185000007\" chance=\"100.00\""));
		assertFalse(npcDropBlock(drops, "214028").contains("item_id=\"188053787\""));

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		assertTrue(coverage.contains("behavior=\"MATCHMAKER\" behavior_source=\"matchmaker.xml:323,matchmaker.xml:407\""
			+ " classification=\"standard\" cooltime_id=\"5\" creation_ids=\"28,217,236\" id=\"320110000\""));
	}

	@Test
	void indratuFortressUsesRetailAiAndDropsWithoutLegacyHandler() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/IndratuFortressInstance.java")));
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/indratuFortress/Brigadier_IndratuAI2.java")));

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_004.xml"));
		assertFalse(npcDropBlock(drops, "214159").contains("item_id=\"188053787\""));

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/310090000_Indratu_Fortress.xml"));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"214159\">"));

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		assertTrue(coverage.contains("behavior=\"MATCHMAKER\" behavior_source=\"matchmaker.xml:315,matchmaker.xml:404\""
			+ " classification=\"standard\" cooltime_id=\"1\" creation_ids=\"32,215,233\" id=\"310090000\""));
	}

	@Test
	void shadowCourtUsesRetailQuestAndKeyDropsWithoutLegacyHandler() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ShadowCourtInstance.java")));

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_004.xml"));
		String[][] keys = {
			{ "214347", "185000014" }, { "214349", "185000011" }, { "214351", "185000012" },
			{ "214353", "185000013" }, { "214357", "185000009" }, { "214360", "185000010" },
			{ "214531", "185000008" }
		};
		for (String[] key : keys) {
			assertTrue(npcDropBlock(drops, key[0])
				.contains("item_id=\"" + key[1] + "\" chance=\"100.00\""));
		}

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/320120000_Shadow_Court_Dungeon.xml"));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"700369\">"));

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		assertTrue(Pattern.compile("<world\\b(?=[^>]*\\bid=\"320120000\")"
			+ "(?=[^>]*\\bbehavior=\"RETAIL_AI_QUEST\")(?=[^>]*_24046The_Shadow_Calls\\.java)[^>]*/>")
			.matcher(coverage).find());
	}

	@Test
	void aetherogeneticsLabUsesRetailKeyDropsAndKeepsOnlyKeyCleanup() throws Exception {
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AetherogeneticsLabInstance.java"));
		assertFalse(handler.contains("onDropRegistered"));
		assertFalse(handler.contains("188053787"));
		assertFalse(handler.contains("onDie"));
		assertFalse(handler.contains("GameWorldServices"));
		assertTrue(handler.contains("onPlayerLogOut"));
		assertTrue(handler.contains("onLeaveInstance"));
		for (int itemId = 185000001; itemId <= 185000005; itemId++) {
			assertTrue(handler.contains(Integer.toString(itemId)));
		}

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_002.xml"));
		String[][] keys = {
			{ "212341", "185000001" }, { "212175", "185000002" }, { "212196", "185000003" },
			{ "212193", "185000004" }, { "212342", "185000005" }
		};
		for (String[] key : keys) {
			assertTrue(npcDropBlock(drops, key[0])
				.contains("item_id=\"" + key[1] + "\" chance=\"100.00\""));
		}
		assertFalse(npcDropBlock(drops, "212202").contains("item_id=\"185000005\""));
		assertFalse(npcDropBlock(drops, "212211").contains("item_id=\"188053787\""));
	}

	@Test
	void steelRakeCabinUsesRetailPartyPatternsAndDropsWithoutLegacyPaths() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/steelRake/SteelRakeCabineInstance.java")));
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/steelRakeCabin/AnikikiAI2.java")));

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "300460000");
		assertTrue(world.contains("<variable name=\"lever_ver30\"/>"));
		assertEquals(1, count(world, "<condition "));
		assertEquals(2, count(world, "<party probability=\"4500\""));
		assertEquals(2, count(world, "<party probability=\"500\""));
		assertEquals(2, count(world, "npc id=\"219032\""));
		assertEquals(2, count(world, "npc id=\"219039\""));
		assertEquals(4, count(world, "npc id=\"219003\""));
		assertEquals(4, count(world, "x=\"463.124115\" y=\"512.749939\" z=\"953.665344\""));
		assertEquals(4, count(world, "x=\"502.858521\" y=\"548.550232\" z=\"953.665344\""));

		String walkers = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/npc_walker/custom_npc_walker.xml"));
		assertFalse(walkers.contains("route_id=\"3004600001\""));
		String retailWalkers = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/npc_walker/300100000_Steel Rake_Walkers.xml"));
		assertTrue(retailWalkers.contains("route_id=\"IDShip_FShulackWiBreeder_42_Ae_Path\""));

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300460000_Steel_Rake_Cabin.xml"));
		assertTrue(staticSpawns.contains("npc_id=\"219040\""));
		assertTrue(staticSpawns.contains("walker_id=\"IDShip_FShulackWiBreeder_42_Ae_Path\""));
		assertFalse(staticSpawns.contains("npc_id=\"219032\""));
		assertFalse(staticSpawns.contains("npc_id=\"219039\""));

		String npcAi = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-ai.xml"));
		for (String mapping : new String[] {
			"id=\"219033\" name=\"IDShip_ManduriGiantNmd_43_Ah_ver30\" ai=\"IDSShip_KK\"",
			"id=\"219040\" name=\"IDShip_FShulackWiBreeder_42_Ae_ver30\" ai=\"IDSlk_Extra1\"",
			"id=\"701386\" name=\"IDshulackship_Lever_A_ver30\" ai=\"IDSShip_LeverA\"",
			"id=\"701387\" name=\"IDshulackship_Lever_B_ver30\" ai=\"IDSShip_LeverB\""
		}) {
			assertTrue(npcAi.contains(mapping));
		}
	}

	@Test
	void aetherMineUsesRetailConditionFlowWithoutLegacySpawns() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AetherMineQInstance.java")));

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "301690000");
		assertTrue(world.contains("<variable name=\"f6_mission_start\"/>"));
		assertTrue(world.contains("<variable name=\"f6_mission_spawn\"/>"));
		assertEquals(42, count(world, "<condition "));

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301690000_Aether_Mine.xml"));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"244145\">"));
		assertTrue(staticSpawns.contains("x=\"320.514862\" y=\"263.688446\" z=\"261.491791\""));

		for (String quest : new String[] { "_10529Protection_Artifact_2", "_20529Building_A_Protection_Artifact_2" }) {
			String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/quest/handlers/archdaeva/" + quest + ".java"));
			assertFalse(source.contains("GameThreadPoolServices"));
			assertFalse(source.contains("QuestService.addNewSpawn"));
			assertTrue(source.contains("getNpc(244145)"));
			assertTrue(source.contains("controller.getObjectId()"));
		}

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		assertTrue(Pattern.compile("<world\\b(?=[^>]*\\bid=\"301690000\")(?=[^>]*\\bbehavior=\"RETAIL_AI_QUEST\")[^>]*/>")
			.matcher(coverage).find());
	}

	@Test
	void theobomosTestChamberUsesRetailBossFlowAndDrops() throws Exception {
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TheobomosTestChamberInstance.java"));
		assertTrue(handler.contains("npc.getNpcId() == 220426"));
		assertTrue(handler.contains("spawn(806221"));
		assertFalse(handler.contains("GameThreadPoolServices"));
		assertFalse(handler.contains("onDropRegistered"));
		assertFalse(handler.contains("StaticDoor"));
		assertFalse(handler.contains("spawn(806206"));
		for (String privateReward : new String[] { "188053789", "188058413", "188057620", "166040001",
			"188057618", "188057619", "188054908", "188054909" }) {
			assertFalse(handler.contains(privateReward));
		}
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/theobomosTestChamber/Desecrated_IfritAI2.java")));

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "301610000");
		assertEquals(6, count(world, "<condition "));
		assertTrue(world.contains("boss_summon == 2"));
		assertTrue(world.contains("boss_summon_check == 4"));
		assertTrue(world.contains("End_Boss_Die == 1"));
		assertTrue(world.contains("npc id=\"806206\""));

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301610000_Theobomos_Test_Chamber.xml"));
		assertFalse(staticSpawns.contains("npc_id=\"220426\""));
		assertTrue(staticSpawns.contains("npc_id=\"248975\""));

		String bossDrops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_009.xml"));
		assertTrue(npcDropBlock(bossDrops, "220425")
			.contains("item_id=\"185000264\" chance=\"40.00\""));
		String chestDrops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(chestDrops, "806221")
			.contains("common_drop_group name=\"IDF6_LAP_ARMOR_LOOK_R_69A\""));
	}

	@Test
	void infinityShardUsesRetailPatternAndConditionSpawnsWithoutLegacyPath() throws Exception {
		Path conditions = Path.of("src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
		String world = worldBlock(Files.readString(conditions), "300800000");
		assertTrue(world.contains("<variable name=\"csetcharge\"/>"));
		assertTrue(world.contains("<variable name=\"csetfastcharge\"/>"));
		assertTrue(world.contains("expression=\"cSetCharge==1\""));
		assertTrue(world.contains("expression=\"cSetIdPortal==1\""));
		assertTrue(world.contains("expression=\"cProtection01 &gt;= 3\""));
		assertTrue(world.contains("npc id=\"284765\""));
		assertTrue(world.contains("npc id=\"730842\""));

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300800000_Infinity_Shard.xml"));
		assertFalse(staticSpawns.contains("npc_id=\"284437\""));
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/InfinityShardInstance.java")));
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/infinityShard/HyperionAI2.java")));
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/infinityShard/IdeResonatorAI2.java")));

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		assertTrue(coverage.contains("id=\"300800000\""));
		assertTrue(coverage.contains("behavior=\"MATCHMAKER\" behavior_source=\"matchmaker.xml:324\""));
	}

	private static void assertMigrated(String className, String deadline, String state) throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/" + className + ".java"));
		assertTrue(source.contains(deadline));
		assertTrue(source.contains(state));
		assertFalse(source.contains("Future<?>"));
	}

	private static void assertNoFuture(String className) throws Exception {
		assertSourceExcludes(className, "Future<?>");
	}

	private static void assertSourceExcludes(String className, String forbidden) throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/" + className + ".java"));
		assertFalse(source.contains(forbidden));
	}

	private static void assertSourceContains(String className, String required) throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/" + className + ".java"));
		assertTrue(source.contains(required));
	}

	private static String worldBlock(String source, String worldId) {
		var matcher = Pattern.compile("<world id=\\\"" + worldId + "\\\".*?</world>", Pattern.DOTALL)
				.matcher(source);
		assertTrue(matcher.find());
		return matcher.group();
	}

	private static String npcDropBlock(String source, String npcId) {
		var matcher = Pattern.compile("<npc_drop npc_id=\\\"" + npcId + "\\\".*?</npc_drop>", Pattern.DOTALL)
			.matcher(source);
		assertTrue(matcher.find());
		return matcher.group();
	}

	private static int count(String source, String needle) {
		return (int) ((source.length() - source.replace(needle, "").length()) / needle.length());
	}
}
