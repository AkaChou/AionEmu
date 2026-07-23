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
		assertMigrated("PadmarashkaCaveInstance", "scheduleDeadline(\"expire\"", "padma.protectors");
		assertMigrated("CradleOfEternityInstance", "scheduleDeadline(\"start\"", "cradle.covetous_complete");
		assertSourceContains("CradleOfEternityInstance", "cradle.sun_revealed");
		assertSourceExcludes("CradleOfEternityInstance", "GameThreadPoolServices");
		assertMigrated("TransidiumAnnexInstance", "scheduleDeadline(\"start\"", "transidium.hangar_barricade");
		assertSourceContains("TransidiumAnnexInstance", "scheduleDeadline(\"return\"");
		assertSourceContains("TransidiumAnnexInstance", "transidium.return_deadline");
		assertSourceContains("TransidiumAnnexInstance", "transidium.return_complete");
		assertSourceExcludes("TransidiumAnnexInstance", "GameThreadPoolServices");
		assertMigrated("TheobomosLabInstance", "scheduleDeadline(\"stone\"", "theobomos.ifrit_deadline");
		assertMigrated("DraupnirCaveInstance", "scheduleDeadline(\"gate_raid_2\"", "draupnir.adjutants");
		assertSourceExcludes("DraupnirCaveInstance", "GameThreadPoolServices");
		assertSourceExcludes("RentusBaseInstance", "GameThreadPoolServices");
		assertSourceExcludes("RentusBaseInstance", "case 217292");
		assertSourceExcludes("RentusBaseInstance", "case 217299");
		assertSourceExcludes("OccupiedRentusBaseInstance", "GameThreadPoolServices");
		assertSourceExcludes("OccupiedRentusBaseInstance", "case 236267");
		assertMigrated("MirashSanctuaryInstance", "scheduleDeadline(\"wave\"", "mirash.wave_spawned");
		assertSourceExcludes("MirashSanctuaryInstance", "GameThreadPoolServices");
		assertSourceContains("crucible/CrucibleChallengeInstance", "scheduleDeadline(\"revive.\"");
		assertSourceExcludes("crucible/CrucibleChallengeInstance", "GameThreadPoolServices");
		assertSourceExcludes("crucible/CrucibleChallengeInstance", "onDropRegistered");
		assertSourceExcludes("crucible/EmpyreanCrucibleInstance", "GameThreadPoolServices");
		assertSourceExcludes("crucible/EmpyreanCrucibleInstance", "onDropRegistered");
		assertMigrated("LinkgateFoundryInstance", "scheduleDeadline(\"expire\"",
				"linkgate.expire_deadline");
		assertMigrated("RightWingChamberInstance", "scheduleDeadline(\"treasure\"",
				"rightwing.deadline");
		assertMigrated("LeftWingChamberInstance", "scheduleDeadline(\"treasure\"",
				"leftwing.deadline");
		assertMigrated("LowerUdasTempleInstance", "scheduleDeadline(\"chest\"",
				"lower_udas.next_deadline");
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
		assertMigrated("DarkPoetaInstance", "scheduleDeadline(\"expire\"", "dark.kill.");
		assertSourceContains("DarkPoetaInstance", "InstanceSettlementService.darkPoetaRank");
		assertSourceContains("DarkPoetaInstance", "DataManager.RETAIL_AI_DATA.getNpcScore");
		assertSourceExcludes("DarkPoetaInstance", "GameThreadPoolServices");
		assertNoFuture("DarkPoetaInstance");
	}

	@Test
	void tiamatStrongholdUsesRetailConditionsAndKeepsOnlyUnsupportedSwitch() throws Exception {
		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "300510000");
		assertEquals(18, count(world, "<variable "));
		assertEquals(159, count(world, "<condition "));
		for (String variable : new String[] { "arianasorus_spawn", "garnon_spawn", "idtiamat_enter_s1",
				"idtiamat_teleport_s1_1", "idtiamat_teleport_s1_2", "idtiamat_teleport_s1_3",
				"idtiamat_teleport_s3", "kahrun_spawn", "kahrun_talk", "murugan_spawn", "rewardl_spawn",
				"surama_spawn" }) {
			assertTrue(world.contains("<variable name=\"" + variable + "\"/>"), variable);
		}
		for (String expected : new String[] { "NAMED_KILL == 7", "TAHABATA_TREASUREBOX == 1",
				"IDTIAMAT_WAVE1 == 1", "IDTIAMAT_WAVE2 == 1", "IDTIAMAT_WAVE3 == 1", "npc id=\"701527\"",
				"npc id=\"701541\"", "npc id=\"800456\"" }) {
			assertTrue(world.contains(expected), expected);
		}

		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TiamatStrongholdInstance.java"));
		assertTrue(handler.contains("npc.getNpcId() == 701523"));
		assertTrue(handler.contains("setDoorState(22, true)"));
		for (String legacy : new String[] { "GameThreadPoolServices", "onDropRegistered", "onDie(", "spawn(",
				"onInstanceCreate", "Future<", "219363", "219364", "730694" }) {
			assertFalse(handler.contains(legacy), legacy);
		}

		String staticSpawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/300510000_Tiamat_Stronghold.xml"));
		for (String npcId : new String[] { "206265", "206266", "206267", "219392", "800369", "800373",
				"800374", "800375", "800376", "800377", "800378", "800379", "800380", "800423", "800424",
				"800435", "800436", "800438", "800460", "800463" }) {
			assertFalse(staticSpawns.contains("npc_id=\"" + npcId + "\""), npcId);
		}
	}

	@Test
	void rentusTransformationsUseRetailPatterns() throws Exception {
		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idyun_hue.xml"));
		assertTrue(patterns.contains("<name>IDYun_Temp_31</name>"));
		assertTrue(patterns.contains("<name>IDYun_Drakan_ND3</name>"));
		assertTrue(patterns.contains("<npc_nameid>IDYun_FallOff_Spawner_weak</npc_nameid>"));
		assertTrue(patterns.contains("<npc_nameid>IDYun_FallOff_Spawner</npc_nameid>"));
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
	void sanctuaryDungeonUsesRetailRaceConditionsWithoutLegacyHandler() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/SanctuaryDungeonInstance.java")));

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String sanctuary = worldBlock(conditions, "301580000");
		assertTrue(sanctuary.contains("<variable name=\"idf6_race_l\"/>"));
		assertTrue(sanctuary.contains("<variable name=\"idf6_race_d\"/>"));
		assertTrue(sanctuary.contains("IDF6_RACE_L ==1"));
		assertTrue(sanctuary.contains("IDF6_RACE_D ==1"));
		for (String npcId : new String[] { "806076", "806080", "806189", "806190" }) {
			assertTrue(sanctuary.contains("npc id=\"" + npcId + "\""));
		}
		assertEquals(4, count(sanctuary, "<condition "));

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301580000_Sanctuary_Dungeon.xml"));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"703092\" respawn_time=\"1\">"));
		assertTrue(staticSpawns.contains("x=\"432.973297\" y=\"490.474091\" z=\"102.525612\""));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"806118\" respawn_time=\"1\">"));

		String npcAi = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-ai.xml"));
		assertTrue(npcAi.contains("id=\"703092\" name=\"LDF6_OP_race_check_NPC\""
			+ " ai=\"LF6_F2_Din_04_Enter_Attack_67\""));

		String pattern = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idf6_ydy.xml"));
		assertTrue(pattern.contains("<name>LF6_F2_Din_04_Enter_Attack_67</name>"));
		assertTrue(pattern.contains("<string>IDF6_RACE_L</string>"));
		assertTrue(pattern.contains("<string>IDF6_RACE_D</string>"));
	}

	@Test
	void azoturanFortressUsesRetailMatchmakingWithoutPrivateQuestBuff() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AzoturanFortressInstance.java")));

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		assertTrue(coverage.contains("behavior=\"MATCHMAKER\""
			+ " behavior_source=\"matchmaker.xml:334,matchmaker.xml:405\""
			+ " classification=\"standard\" cooltime_id=\"25\""
			+ " creation_ids=\"31,221,234,257\" id=\"310100000\""));
	}

	@Test
	void karamatisUsesRetailAscensionBlessingWithoutLegacyZoneHandler() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/KaramatisInstance.java")));

		String quest = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/quest/handlers/ascension/_1006Ascension.java"));
		assertTrue(quest.contains("if (qs.getQuestVars().getQuestVars() == 99) {\n"
			+ "\t\t\t\t\t\t\t\tGameEngineServices.skillEngine().applyEffectDirectly(281, player, player, 0);"));
	}

	@Test
	void ataxiarUsesRetailAscensionShieldWithoutLegacyZoneHandler() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AtaxiarInstance.java")));

		String quest = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/quest/handlers/ascension/_2008Ascension.java"));
		assertTrue(quest.contains("if (var == 99) {\n"
			+ "\t\t\t\t\t\t\tGameEngineServices.skillEngine().applyEffectDirectly(257, player, player, 0);"));
	}

	@Test
	void kromedesTrialKeepsRageOnRetailCorpseInteraction() throws Exception {
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/KromedesTrialInstance.java"));
		assertFalse(handler.contains("rageOfKromede"));
		assertFalse(handler.contains("19288"));
		assertFalse(handler.contains("onDropRegistered"));
		for (String privateReward : new String[] { "188052826", "188053787", "190080005", "190080006",
			"190080007", "190080008", "190200000" }) {
			assertFalse(handler.contains(privateReward));
		}
		assertFalse(handler.contains("sendMovie(player, 454);"));
		assertFalse(handler.contains("KALIGA_DUNGEONS_300230000"));

		for (String file : new String[] { "_18602Nightmare_In_Shining_Armor.java", "_28602Into_The_Unknown.java" }) {
			String quest = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/quest/handlers/kromedes_trial", file));
			assertTrue(quest.contains("getSkill(player, 19288, 1, player).useNoAnimationSkill()"));
			assertFalse(quest.contains("1111307"));
		}

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_006.xml"));
		assertTrue(npcDropBlock(drops, "216999")
			.contains("item_id=\"185000101\" chance=\"100.00\""));
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
		String level52Skinmenders = spawnBlock(staticSpawns, "215857");
		for (String misplacedPosition : new String[] { "x=\"527.282\" y=\"503.964\"",
				"x=\"595.821\" y=\"640.241\"", "x=\"441.13\" y=\"435.65\"",
				"x=\"524.769\" y=\"387.584\"", "x=\"652.313\" y=\"525.245\"",
				"x=\"767.793\" y=\"304.979\"", "x=\"492.231\" y=\"386.141\"" }) {
			assertFalse(level52Skinmenders.contains(misplacedPosition));
		}
		String level53Skinmenders = spawnBlock(staticSpawns, "215808");
		assertEquals(7, count(level53Skinmenders, "<spot "));
		assertFalse(level53Skinmenders.contains("respawn_time="));
		for (String npcId : new String[] { "215782", "215783", "215793", "730217", "700706", "730272" }) {
			assertFalse(staticSpawns.contains("npc_id=\"" + npcId + "\""));
		}
	}

	@Test
	void lowerUdasUsesCompactDropsWithoutPrivateInjection() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/LowerUdasTempleInstance.java"));
		assertFalse(handler.contains("onDropRegistered"));
		assertFalse(handler.contains("GameWorldServices"));
		for (String privateDrop : new String[] { "188053579", "188053580", "188052306", "188053788" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}

		String bosses = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_005.xml"));
		assertTrue(npcDropBlock(bosses, "215786").contains("item_id=\"185000086\" chance=\"100.00\""));
		assertTrue(npcDropBlock(bosses, "215796").contains("item_id=\"185000087\" chance=\"10.00\""));
		assertTrue(npcDropBlock(bosses, "215797").contains("item_id=\"188052306\" chance=\"20.00\""));

		String chests = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_006.xml"));
		assertTrue(npcDropBlock(chests, "216149").contains("IDTEMPLE_BOX_ACCESSORY_HEAD_A_N_U1_52A"));
		assertTrue(npcDropBlock(chests, "216150").contains("IDTEMPLE_BOX_ACCESSORY_HEAD_A_N_U1_52A"));

		String eventChests = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(eventChests, "702658").contains("item_id=\"188053579\" chance=\"100.00\""));
		assertTrue(npcDropBlock(eventChests, "702659").contains("item_id=\"188053580\" chance=\"100.00\""));
		assertFalse(bosses.contains("188053788"));
		assertFalse(chests.contains("188053788"));
		assertFalse(eventChests.contains("188053788"));
	}

	@Test
	void sealedArgentManorUsesCompactDropsWithoutPrivateBossRewards() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/SealedArgentManorInstance.java"));
		assertFalse(handler.contains("onDropRegistered"));
		assertFalse(handler.contains("GameWorldServices"));
		for (String privateDrop : new String[] { "190080005", "190080006", "190080007", "190080008",
				"190200000" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}

		String keys = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_013.xml"));
		assertTrue(npcDropBlock(keys, "237190").contains("item_id=\"185000242\" chance=\"100.00\""));
		assertFalse(keys.contains("npc_id=\"237193\""));
		assertFalse(keys.contains("npc_id=\"237194\""));

		String chests = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		String chest = npcDropBlock(chests, "702816");
		assertTrue(chest.contains("item_id=\"188054117\" chance=\"100.00\""));
		assertTrue(chest.contains("item_id=\"188054118\" chance=\"45.00\""));
		assertTrue(chest.contains("IDELEMENTAL_GOODS_BOX_65A"));
		assertTrue(chest.contains("IDELEMENTAL_SUBMATTER_BOX_65A"));
	}

	@Test
	void mirashSanctuaryUsesCompactDropsWithoutPrivateBossRewards() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/MirashSanctuaryInstance.java"));
		assertTrue(handler.contains("case 835784"));
		for (String privateDrop : new String[] { "188058115", "188058116", "188058117", "188058118",
				"188058130", "188058131", "188058132", "190080005", "190080006", "190080007",
				"190080008", "190200000" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}

		String bosses = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_017.xml"));
		String boss = npcDropBlock(bosses, "248013");
		assertTrue(boss.contains("item_id=\"188058117\" chance=\"100.00\""));
		assertTrue(boss.contains("item_id=\"188058118\" chance=\"100.00\""));
		assertTrue(boss.contains("item_id=\"190200000\" chance=\"100.00\""));
		assertTrue(boss.contains("ABYSS_70_ALL_IDABRE_CORE_03"));
		assertTrue(npcDropBlock(bosses, "248533").contains("item_id=\"185000317\" chance=\"100.00\""));

		String chests = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(chests, "835730").contains("item_id=\"188058116\" chance=\"100.00\""));
		assertTrue(npcDropBlock(chests, "835732").contains("item_id=\"182006892\" chance=\"0.10\""));
		assertTrue(npcDropBlock(chests, "835733").contains("item_id=\"182006892\" chance=\"0.10\""));
	}

	@Test
	void cradleOfEternityUsesCompactKeyDropsWithoutRemovingUnverifiedBossRewards() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/CradleOfEternityInstance.java"));
		assertTrue(handler.contains("case 220526"));
		assertFalse(handler.contains("regDropItem(1, 0, npcId, 185000266, 1)"));
		assertFalse(handler.contains("regDropItem(1, 0, npcId, 185000267, 1)"));

		String guardians = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_009.xml"));
		for (String npcId : new String[] { "220470", "220471", "220472", "220594" }) {
			assertTrue(npcDropBlock(guardians, npcId).contains("item_id=\"185000266\" chance=\"100.00\""), npcId);
		}

		String chest = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(chest, "834091").contains("item_id=\"185000267\" chance=\"100.00\""));
	}

	@Test
	void linkgateFoundryUsesCompactDropsWithoutPrivateHandlerFlow() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/LinkgateFoundryInstance.java"));
		assertFalse(handler.contains("onDropRegistered"));
		for (String privateDrop : new String[] { "188053789", "188053238", "188053239", "190080005",
				"190080006", "190080007", "190080008", "190200000" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}

		String baseDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_011.xml"));
		String volatileBelsagos = npcDropBlock(baseDrops, "233898");
		assertTrue(volatileBelsagos.contains("item_id=\"186000236\" chance=\"100.00\" min_amount=\"12\""));
		assertTrue(volatileBelsagos.contains("item_id=\"188053295\" chance=\"1.20\""));
		assertTrue(npcDropBlock(baseDrops, "234194").contains("item_id=\"188052973\" chance=\"100.00\""));
		assertTrue(npcDropBlock(baseDrops, "234195").contains("item_id=\"188052974\" chance=\"100.00\""));

		String bosses = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_012.xml"));
		assertTrue(npcDropBlock(bosses, "234990").contains("item_id=\"188053331\" chance=\"100.00\""));
		String furiousBelsagos = npcDropBlock(bosses, "234991");
		assertTrue(furiousBelsagos.contains("item_id=\"186000236\" chance=\"100.00\" min_amount=\"12\""));
		assertTrue(furiousBelsagos.contains("item_id=\"188053295\" chance=\"1.20\""));
	}

	@Test
	void archivesOfEternityUsesCompactDropsWithoutPrivateHandlerFlow() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ArchivesOfEternityInstance.java"));
		assertFalse(handler.contains("onDropRegistered"));
		assertTrue(handler.contains("spawnHistoriesOfAtreia"));
		for (String privateDrop : new String[] { "188058413", "166040001", "190080005", "190080006",
				"190080007", "190080008", "190200000" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}

		String bosses = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_026.xml"));
		for (String npcId : new String[] { "857452", "857456", "857459" }) {
			assertTrue(npcDropBlock(bosses, npcId).contains("item_id=\"188057928\" chance=\"100.00\""), npcId);
		}
		for (String npcId : new String[] { "857460", "857462", "857464" }) {
			assertTrue(npcDropBlock(bosses, npcId).contains("item_id=\"188057929\" chance=\"100.00\""), npcId);
		}
		assertTrue(npcDropBlock(bosses, "857460").contains("item_id=\"182215992\" chance=\"100.00\""));

		String objects = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(objects, "806139").contains("IDETERNITY01_ARMOR_LOOK_E_70A"));
		for (String npcId : new String[] { "703131", "703132", "703133", "703149", "703150", "703151", "703134" }) {
			assertTrue(npcDropBlock(objects, npcId).contains("IDETERNITY01_"), npcId);
		}

		String groups = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/common_drop_groups.xml"));
		assertTrue(groups.contains("IDETERNITY01_QUEST_BOOK_L_01A"));
		assertTrue(groups.contains("item_id=\"188100300\" chance=\"20.00\""));
		assertTrue(groups.contains("IDETERNITY01_QUEST_BOOK_D_03A"));
		assertTrue(groups.contains("item_id=\"188100329\" chance=\"20.00\""));
		assertTrue(groups.contains("IDETERNITY01_KEY_BOOK_N_75A"));
	}

	@Test
	void esoterraceUsesCompactDropsWithoutPrivateHandlerFlow() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/EsoterraceInstance.java"));
		assertFalse(handler.contains("onDropRegistered"));
		assertTrue(handler.contains("case 217206"));
		assertTrue(handler.contains("spawn(701025"));
		for (String privateDrop : new String[] { "188053789", "190020089", "190020148", "190020204",
				"190070004", "190070012" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}

		String bossDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_007.xml"));
		String surama = npcDropBlock(bossDrops, "217206");
		assertTrue(surama.contains("IDLDF4_REWARD_WEAPON_A_N_U1_55A"));
		assertFalse(surama.contains("188053789"));

		String boxDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		String sundries = npcDropBlock(boxDrops, "701025");
		assertTrue(sundries.contains("item_id=\"190000050\" chance=\"73.55\""));
		assertTrue(sundries.contains("item_id=\"188058280\" chance=\"70.00\""));
	}

	@Test
	void theobomosLabUsesCompactDropsWhileKeepingUnmodeledQuestFlows() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TheobomosLabInstance.java"));
		assertTrue(handler.contains("case 700422"));
		assertTrue(handler.contains("182208053"));
		assertTrue(handler.contains("case 237247"));
		for (String privateDrop : new String[] { "185000016", "185000025", "185000023", "185000022",
				"185000021", "188053788", "188053083", "188054176", "188054180", "185000015",
				"166050023", "166050024", "166050025", "166050026", "166050027", "166050028",
				"166050029", "166050030", "166050031", "166050032", "166050033", "166050034",
				"166050035", "166050036", "166050037", "166050038" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}

		String drops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_013.xml"));
		for (String[] expected : new String[][] { { "237108", "185000016" }, { "237110", "185000025" },
				{ "237112", "185000023" }, { "237113", "185000022" }, { "237114", "185000021" } }) {
			assertTrue(npcDropBlock(drops, expected[0]).contains("item_id=\"" + expected[1] + "\" chance=\"100.00\""),
					expected[0]);
		}
		assertTrue(npcDropBlock(drops, "237111").contains("item_id=\"185000015\" chance=\"100.00\""));
		String ifrit = npcDropBlock(drops, "237251");
		assertTrue(ifrit.contains("item_id=\"188054176\" chance=\"40.00\""));
		assertTrue(ifrit.contains("item_id=\"188054180\" chance=\"60.00\""));

		String eventChests = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(eventChests, "702658").contains("item_id=\"188053579\" chance=\"100.00\""));
		assertTrue(npcDropBlock(eventChests, "702659").contains("item_id=\"188053580\" chance=\"100.00\""));
	}

	@Test
	void occupiedRentusBaseUsesCompactDropsWithoutPrivateHandlerFlow() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/OccupiedRentusBaseInstance.java"));
		assertFalse(handler.contains("onDropRegistered"));
		assertTrue(handler.contains("spawnOccupiedDirectFiringGunIDYun"));
		assertTrue(handler.contains("handleUseItemFinish"));
		for (String privateDrop : new String[] { "188053789", "170170033", "170030052", "188053083",
				"188053703", "188053704", "188053705", "185000229" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}

		String bossDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_012.xml"));
		String vasharti = npcDropBlock(bossDrops, "236300");
		assertTrue(vasharti.contains("item_id=\"188053702\" chance=\"100.00\""));
		assertTrue(vasharti.contains("item_id=\"188053706\" chance=\"100.00\""));
		assertTrue(vasharti.contains("IDYUN_HARD_HEAD_E_65A"));

		String jewelryDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_008.xml"));
		String jewelry = npcDropBlock(jewelryDrops, "218572");
		assertTrue(jewelry.contains("item_id=\"170195109\" chance=\"10.00\""));
		assertTrue(jewelry.contains("IDYUN_HEAD_N_U1_60A"));

		String objectDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(objectDrops, "833048").contains("item_id=\"188053706\" chance=\"33.30\""));
		assertTrue(npcDropBlock(objectDrops, "702658").contains("item_id=\"188053579\" chance=\"100.00\""));
		assertTrue(npcDropBlock(objectDrops, "702659").contains("item_id=\"188053580\" chance=\"100.00\""));
	}

	@Test
	void rentusBaseUsesCompactDropsWithoutPrivateHandlerFlow() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/RentusBaseInstance.java"));
		assertFalse(handler.contains("onDropRegistered"));
		assertTrue(handler.contains("spawnDirectFiringGunIDYun"));
		assertTrue(handler.contains("handleUseItemFinish"));
		for (String privateDrop : new String[] { "185000228", "188053789", "170170033", "188053083" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}

		String bossDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_007.xml"));
		String vasharti = npcDropBlock(bossDrops, "217313");
		assertTrue(vasharti.contains("item_id=\"170030052\" chance=\"10.00\""));
		assertTrue(vasharti.contains("IDYUN_Nmd_HEAD_N_E1_60A"));

		String jewelryDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_008.xml"));
		String jewelry = npcDropBlock(jewelryDrops, "218572");
		assertTrue(jewelry.contains("item_id=\"170195109\" chance=\"10.00\""));
		assertTrue(jewelry.contains("IDYUN_HEAD_N_U1_60A"));

		String objectDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(objectDrops, "833047").contains("item_id=\"170030052\" chance=\"3.33\""));
		assertTrue(npcDropBlock(objectDrops, "702658").contains("item_id=\"188053579\" chance=\"100.00\""));
		assertTrue(npcDropBlock(objectDrops, "702659").contains("item_id=\"188053580\" chance=\"100.00\""));
	}

	@Test
	void sauroSupplyBaseUsesCompactDropsWhileKeepingOpportunityBundleAndKeyPrompt() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/SauroSupplyBaseInstance.java"));
		assertTrue(handler.contains("npcId == 230847"));
		assertTrue(handler.contains("sendMsg(1401946"));
		assertTrue(handler.contains("npcId != 802181"));
		for (String privateDrop : new String[] { "188053219", "188052578", "188052582", "188053789",
				"188053083", "188053211", "188053579", "188053580" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}
		for (String privateRegistration : new String[] { "regDropItem(1, 0, npcId, 185000176, 1)",
				"regDropItem(1, 0, npcId, 185000177, 1)", "regDropItem(1, 0, npcId, 185000178, 1)",
				"regDropItem(index++, player.getObjectId(), npcId, 185000179, 1)" }) {
			assertFalse(handler.contains(privateRegistration), privateRegistration);
		}
		for (String retainedDrop : new String[] { "186000051, 30", "186000052, 30", "186000236, 50",
				"186000237, 50" }) {
			assertTrue(handler.contains(retainedDrop), retainedDrop);
		}

		String baseDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_010.xml"));
		for (String[] expected : new String[][] { { "230846", "IDVRITRABASE_SHUGOTHIEF" },
				{ "230849", "IDVRITRABASE_WEAPON_U1_65A" }, { "230850", "IDVRITRABASE_ACCESSORY_U1_65A" },
				{ "230851", "IDVRITRABASE_WPAR_U1_65A" }, { "230852", "IDVRITRABASE_ARAC_U1_65A" },
				{ "230853", "IDVRITRABASE_ARMOR_U1_65A" }, { "230857", "IDVRITRABASE_WPAR_E1_65A" },
				{ "230858", "IDVRITRABASE_WPAR_M1_65A" } }) {
			assertTrue(npcDropBlock(baseDrops, expected[0]).contains(expected[1]), expected[0]);
		}
		assertTrue(npcDropBlock(baseDrops, "230847").contains("item_id=\"185000179\" chance=\"100.00\""));

		String commanderDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_011.xml"));
		assertTrue(npcDropBlock(commanderDrops, "233258").contains("IDVRITRABASE_WPAR_U1_65A"));

		String chestDrops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(chestDrops, "702658").contains("item_id=\"188053579\" chance=\"100.00\""));
		assertTrue(npcDropBlock(chestDrops, "702659").contains("item_id=\"188053580\" chance=\"100.00\""));
	}

	@Test
	void danuarSanctuariesUseCompactDropsWhileKeepingCannonballsAndKeyPrompt() throws Exception {
		for (String[] expected : new String[][] { { "DanuarSanctuaryInstance", "235600" },
				{ "SeizedDanuarSanctuaryInstance", "235574" } }) {
			String handler = Files.readString(Path.of(
					"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/" + expected[0] + ".java"));
			assertTrue(handler.contains("npcId == 233391"), expected[0]);
			assertTrue(handler.contains("sendMsg(1401946"), expected[0]);
			assertTrue(handler.contains("npcId != " + expected[1]), expected[0]);
			assertTrue(handler.contains("regDropItem(1, 0, npcId, 186000254, 1)"), expected[0]);
			for (String privateDrop : new String[] { "188053789", "188053579", "188053580", "169405254",
					"152012580" }) {
				assertFalse(handler.contains(privateDrop), expected[0] + ':' + privateDrop);
			}
		}

		String bosses = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_012.xml"));
		for (String npcId : new String[] { "235624", "235625", "235626" }) {
			assertTrue(npcDropBlock(bosses, npcId).contains("item_id=\"188052613\" chance=\"100.00\""), npcId);
		}
		for (String npcId : new String[] { "235619", "235620", "235621" }) {
			assertTrue(npcDropBlock(bosses, npcId).contains("item_id=\"188053710\" chance=\"100.00\""), npcId);
		}
		for (String npcId : new String[] { "235658", "235655" }) {
			assertTrue(npcDropBlock(bosses, npcId).contains("item_id=\"185000174\" chance=\"100.00\""), npcId);
		}

		String objects = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_011.xml"));
		assertTrue(npcDropBlock(objects, "233391").contains("item_id=\"188052656\" chance=\"100.00\""));
		assertTrue(npcDropBlock(objects, "233185").contains("item_id=\"188052581\" chance=\"25.00\""));
		for (String npcId : new String[] { "233190", "233191", "233192" }) {
			assertTrue(npcDropBlock(objects, npcId).contains("IDUNDER02H_TREASURE"), npcId);
		}

		String eventChests = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(eventChests, "702658").contains("item_id=\"188053579\" chance=\"100.00\""));
		assertTrue(npcDropBlock(eventChests, "702659").contains("item_id=\"188053580\" chance=\"100.00\""));

		String disassembly = Files.readString(Path.of("src/main/resources/aion/data/static_data/items/disassembly_items.xml"));
		assertTrue(disassembly.contains("disassemblyItem_Id=\"188052656\""));
		for (String key : new String[] { "185000181", "185000182", "185000183" }) {
			assertTrue(disassembly.contains("itemId=\"" + key + "\""), key);
		}
	}

	@Test
	void ophidanBridgesUseCompactDropsWhileKeepingOpportunityBundle() throws Exception {
		for (String className : new String[] { "OphidanBridgeInstance", "Lucky_OphidanBridgeInstance" }) {
			String handler = Files.readString(Path.of(
					"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ophidanBridge/" + className + ".java"));
			assertTrue(handler.contains("npcId != 802180"), className);
			for (String retainedDrop : new String[] { "186000051, 30", "186000052, 30", "186000236, 50",
					"186000237, 50" }) {
				assertTrue(handler.contains(retainedDrop), className + ':' + retainedDrop);
			}
			for (String privateDrop : new String[] { "182215759", "182215760", "188053708", "188053709",
					"188053710", "188053789", "188052612", "188053579", "188053580" }) {
				assertFalse(handler.contains(privateDrop), className + ':' + privateDrop);
			}
		}

		String drops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_012.xml"));
		for (String npcId : new String[] { "235759", "235763", "235767" }) {
			String leader = npcDropBlock(drops, npcId);
			assertTrue(leader.contains("item_id=\"182215759\" chance=\"80.00\""), npcId);
			assertTrue(leader.contains("item_id=\"182215760\" chance=\"80.00\""), npcId);
		}
		for (String npcId : new String[] { "235768", "235769", "235770", "235771" }) {
			assertTrue(npcDropBlock(drops, npcId).contains("item_id=\"188052612\" chance=\"100.00\""), npcId);
		}

		String chests = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		assertTrue(npcDropBlock(chests, "702658").contains("item_id=\"188053579\" chance=\"100.00\""));
		assertTrue(npcDropBlock(chests, "702659").contains("item_id=\"188053580\" chance=\"100.00\""));
	}

	@Test
	void padmarashkaCaveUsesCompactDropsWithoutPrivateBossRewards() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/PadmarashkaCaveInstance.java"));
		assertFalse(handler.contains("onDropRegistered"));
		for (String privateDrop : new String[] { "188053789", "188057935", "100001640", "100101258",
				"102001175", "115001680", "115001794" }) {
			assertFalse(handler.contains(privateDrop), privateDrop);
		}

		String drops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_008.xml"));
		String padmarashka = npcDropBlock(drops, "218756");
		assertTrue(padmarashka.contains("IDDRAMATA_EQUIP_N_E1_55A"));
		assertTrue(padmarashka.contains("IDDRAMATA_ARMOR_N_U2_55A"));
	}

	@Test
	void hexwayUsesRetailStaticChestsWithoutPrivateHandlerFlow() throws Exception {
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TheHexwayInstance.java"));
		for (String legacy : new String[] { "onDropRegistered", "scheduleDeadline", "CHEST_", "701664", "185000129",
				"GameWorldServices", "onInstanceCreate", "onEnterInstance", "219609" }) {
			assertFalse(handler.contains(legacy), legacy);
		}
		assertTrue(handler.contains("npc.getNpcId() == 219617"));

		String drops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_008.xml"));
		assertFalse(npcDropBlock(drops, "219609").contains("18500013"));
		assertTrue(npcDropBlock(drops, "219610").contains("item_id=\"185000135\" chance=\"100.00\""));

		String spawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/300700000_The_Hexway.xml"));
		String soloA = spawnBlock(spawns, "701662");
		assertFalse(soloA.contains("respawn_time="));
		assertEquals(6, count(soloA, "<spot "));
		assertEquals(6, count(soloA, "z=\"366.120148\""));

		String soloB = spawnBlock(spawns, "701663");
		assertTrue(soloB.startsWith("<spawn npc_id=\"701663\" pool=\"1\">"));
		assertFalse(soloB.contains("respawn_time="));
		assertEquals(5, count(soloB, "<spot "));
		for (String position : new String[] { "x=\"227.478607\" y=\"423.795471\" z=\"366.320160\" h=\"8\"",
				"x=\"205.742401\" y=\"486.374512\" z=\"366.320160\" h=\"5\"",
				"x=\"193.947632\" y=\"552.009888\" z=\"366.320160\" h=\"1\"",
				"x=\"193.586792\" y=\"619.047424\" z=\"366.320160\" h=\"118\"",
				"x=\"205.529861\" y=\"684.402527\" z=\"366.320160\" h=\"115\"" }) {
			assertTrue(soloB.contains(position), position);
		}

		String party = spawnBlock(spawns, "701664");
		assertFalse(party.contains("respawn_time="));
		assertEquals(1, count(party, "<spot "));
		assertTrue(party.contains("x=\"230.309158\" y=\"746.760254\" z=\"366.120148\" h=\"111\""));
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
		assertFalse(handler.contains("spawn("));
		assertTrue(handler.contains("npc.getNpcId() == 700437"));
		assertTrue(handler.contains("getSkill(npc, 276, 16, player)"));

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300030000_Nochsana_Training_Camp.xml"));
		assertEquals(124, count(staticSpawns, "<spot "));
		assertTrue(spawnBlock(staticSpawns, "256686").contains(
			"x=\"328.757874\" y=\"285.468597\" z=\"386.559998\" h=\"26\" random_walk=\"2\""));
		assertTrue(spawnBlock(staticSpawns, "256688").contains(
			"x=\"338.743591\" y=\"284.947327\" z=\"386.559998\" h=\"25\" random_walk=\"2\""));
		assertTrue(staticSpawns.contains("npc_id=\"700437\""));
		assertTrue(staticSpawns.contains("npc_id=\"700438\""));

		String npcAi = Files.readString(Path.of("src/main/resources/aion/definitions/compact/ai/npc-ai.xml"));
		for (String binding : new String[] { "id=\"256686\" name=\"Mini_Castle_LizardmanAs_26_Ae\" ai=\"DrGuard_AeB\"",
				"id=\"256688\" name=\"Mini_Castle_LizardmanPr_26_Ae\" ai=\"DrGuard_PeB\"",
				"id=\"256693\" name=\"Mini_Castle_DrakanFi_27_Ah\" ai=\"MiBGuard_ChiefC_ver40\"",
				"id=\"256694\" name=\"Mini_Castle_Door_Dr\" ai=\"MiDoor\"" }) {
			assertTrue(npcAi.contains(binding), binding);
		}
		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns.xml"));
		for (String pattern : new String[] { "DrGuard_AeB", "DrGuard_PeB", "MiBGuard_ChiefC", "MiDoor" }) {
			assertTrue(patterns.contains("<name>" + pattern + "</name>"), pattern);
		}
		String npcSkills = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/skills/npc-skills.xml"));
		for (String assignment : new String[] { "npc_ids=\"256686 290154\"", "npc_ids=\"256688 290156\"",
				"npc_ids=\"256693 290161\"", "npc_ids=\"700437\"" }) {
			assertTrue(npcSkills.contains(assignment), assignment);
		}
		assertTrue(npcSkills.contains("name=\"NPC_ShieldofCompassion\" id=\"276\" level=\"16\""));

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_019.xml"));
		for (String npcId : new String[] { "256686", "256688", "256693" }) {
			assertTrue(npcDropBlock(drops, npcId).contains("<drop "), npcId);
		}
		String portals = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/portals/portal_template2.xml"));
		assertTrue(portals.contains("<portal_use npc_id=\"700438\">"));
		assertTrue(portals.contains("<portal_path loc_id=\"2100202\" race=\"ELYOS\"/>"));
		assertTrue(portals.contains("<portal_path loc_id=\"2200205\" race=\"ASMODIANS\"/>"));
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		assertFalse(conditions.contains("<world id=\"300030000\""));
		for (String quest : new String[] { "eltnen/_3732General_Mania.java", "morheim/_4732General_Malevolence.java" }) {
			String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/quest/handlers/" + quest));
			assertTrue(source.contains("registerQuestNpc(256694).addOnKillEvent"), quest);
			assertTrue(source.contains("registerQuestNpc(256693).addOnKillEvent"), quest);
			assertTrue(source.contains("defaultOnKillEvent(env, 256694, 0, 1)"), quest);
			assertTrue(source.contains("defaultOnKillEvent(env, 256693, 1, true)"), quest);
		}

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		assertTrue(coverage.contains("behavior_source=\"retail world/static spawns/AI/drops/portal/quests; handler artifact skill bridge\""));
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

	private static String spawnBlock(String source, String npcId) {
		var matcher = Pattern.compile("<spawn npc_id=\\\"" + npcId + "\\\".*?</spawn>", Pattern.DOTALL)
				.matcher(source);
		assertTrue(matcher.find());
		return matcher.group();
	}

	private static int count(String source, String needle) {
		return (int) ((source.length() - source.replace(needle, "").length()) / needle.length());
	}
}
