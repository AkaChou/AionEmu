package com.aionemu.gameserver.dataholders.loadingutils;

import com.aionemu.gameserver.ai.RetailPatternAI2;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.WalkerData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetailAiDefinitionLoaderTest {

	@TempDir
	Path tempDir;

	void rejectsExactMemberAssignedToMultiplePartyTokens() throws Exception {
		Path parties = tempDir.resolve("npc-parties.xml");
		Files.writeString(parties, """
			<npc_parties version="1"><world id="123" name="world">
			<party token="party-1"><npc id="1" x="10" y="20" z="30"/></party>
			<party token="party-2"><npc id="1" x="10" y="20" z="30"/></party>
			</world></npc_parties>
			""");

		IllegalStateException error = assertThrows(IllegalStateException.class,
			() -> RetailAiDefinitionLoader.loadNpcParties(parties.toFile()));

		assertTrue(error.getCause().getMessage().contains("belongs to multiple parties"));
	}

	@Test
	void loadsNpcMappingAndCompilesPhaseRulesByPriority() throws Exception {
		Path patternsDirectory = Files.createDirectory(tempDir.resolve("ai"));
		Path patterns = patternsDirectory.resolve("patterns.xml");
		Files.writeString(patterns, """
			<static_bundle><static_document><content><npc_ai_patterns><npc_ai_pattern>
			<name>IDTiamat_Tahabata</name><event_handlers><on_battle_timer>
			<pattern><priority>2</priority><action_category>PLANNED</action_category><conditions>
			<is_hp_lower_than><who>OBJI_SELF</who><percent>60</percent></is_hp_lower_than>
			</conditions><actions><use_skill><target>OBJI_SELF</target><skill>SKILLI_INDEX_4</skill><skill_level>0</skill_level></use_skill></actions></pattern>
			<pattern><priority>7</priority><action_category>PLANNED</action_category><actions><add_battle_timer><btimer_indicator>BTIMERI_INDEX_1</btimer_indicator><delay>3000</delay></add_battle_timer></actions></pattern>
			</on_battle_timer></event_handlers></npc_ai_pattern></npc_ai_patterns></content></static_document></static_bundle>
			""");
		Path mappings = tempDir.resolve("npc-ai.xml");
		Files.writeString(mappings, """
			<npc_ai_mappings>
			<npc id="219358" name="IDTiamat_Tahabata_Named_60_Ah" ai="IDTiamat_Tahabata" sensory_range="20"
				sensory_range_short="6" sensory_angle="300" talk_delay="10" max_chase_time="SP"
				react_to_pathfind_fail="pull_target" move_type_return="run" move_speed_return="200"
				decrease_sensory_range_return="40"/>
			<npc id="200001" name="DefaultReturnNpc" ai="DefaultReturn" sensory_range="8"
				sensory_range_short="3" sensory_angle="240" talk_delay="0"/>
			</npc_ai_mappings>
			""");
		Path strings = tempDir.resolve("ai-strings.xml");
		Files.writeString(strings, "<ai_strings/>");
		Path areas = tempDir.resolve("ai-areas.xml");
		Files.writeString(areas, "<ai_areas/>");

		RetailAiData data = RetailAiDefinitionLoader.load(patternsDirectory.toFile(), mappings.toFile(), strings.toFile(),
			areas.toFile());

		RetailAiData.Pattern pattern = data.getPattern(219358);
		assertNotNull(pattern);
		assertEquals("IDTiamat_Tahabata", pattern.name());
		assertEquals(7, pattern.event("on_battle_timer").get(0).priority());
		assertEquals("60", pattern.event("on_battle_timer").get(1).conditions().get(0).value("percent"));
		assertEquals("SKILLI_INDEX_4", pattern.event("on_battle_timer").get(1).actions().get(0).value("skill"));
		var retailNpc = data.getNpc(219358);
		assertEquals(10, retailNpc.talkDelay());
		assertEquals("SP", retailNpc.maxChaseTime());
		assertEquals(RetailAiData.PathfindFailReaction.PULL_TARGET, retailNpc.pathfindFailReaction());
		assertEquals("run", retailNpc.returnMoveType());
		assertEquals(200, retailNpc.returnSpeedPercent());
		assertEquals(40, retailNpc.returnSensoryPercent());
		var defaultNpc = data.getNpc(200001);
		assertNull(defaultNpc.maxChaseTime());
		assertEquals(RetailAiData.PathfindFailReaction.RETURN_TO_SP, defaultNpc.pathfindFailReaction());
		assertEquals("walk", defaultNpc.returnMoveType());
		assertEquals(150, defaultNpc.returnSpeedPercent());
		assertEquals(50, defaultNpc.returnSensoryPercent());
	}

	@Test
	void loadsCompleteRetailDefinitions() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		RetailAiData data;
		try {
			data = new XmlDataLoader().loadRetailAiData();
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}

		assertEquals(12797, data.patternCount());
		assertEquals(87721, data.npcCount());
		assertEquals(3491, data.stringCount());
		assertEquals(134, data.areaCount());
		assertEquals(18, data.resurrectAreaCount());
		assertEquals(231, data.questAreaCount());
		assertEquals(1, data.limitAreaCount());
		assertEquals(112, data.groupControlAreaCount());
		assertEquals(56, data.groupControllerCount());
		assertEquals(276, data.skillAreaCount());
		assertEquals(5017, data.conditionSpawnCount());
		assertEquals(40, data.sensoryAreaCount());
		var sensoryArea = data.findSensoryArea(301550000, 220582, 980.914185f, 774.380676f, 1046.33447f);
		assertNotNull(sensoryArea);
		assertTrue(sensoryArea.isInside3D(980, 774, 1046));
		assertNull(data.findSensoryArea(301550000, 220582, 0, 0, 0));
		assertEquals(14457, data.skillCategoryCount());
		assertEquals(2829, data.npcScoreCount());
		assertEquals(356, data.locationAliasCount());
		assertEquals(52, data.getGroupControllers(210100000).stream()
			.filter(controller -> controller.exitWorldId() > 0).count()
			+ data.getGroupControllers(220110000).stream().filter(controller -> controller.exitWorldId() > 0).count()
			+ data.getGroupControllers(210060000).stream().filter(controller -> controller.exitWorldId() > 0).count()
			+ data.getGroupControllers(220050000).stream().filter(controller -> controller.exitWorldId() > 0).count()
			+ data.getGroupControllers(210040000).stream().filter(controller -> controller.exitWorldId() > 0).count()
			+ data.getGroupControllers(220040000).stream().filter(controller -> controller.exitWorldId() > 0).count());
		for (int worldId : new int[] { 210040000, 210060000, 210100000, 220040000, 220050000, 220110000 }) {
			data.getGroupControllers(worldId).stream().filter(controller -> controller.exitWorldId() > 0).forEach(controller ->
				assertNotNull(data.findLocationAlias(controller.exitWorldId(), controller.exitAlias()), controller.name()));
		}
		assertEquals(30, data.directPortalCount());
		assertEquals(92, data.npcPartyCount());
		assertEquals(278, data.npcPartyMemberCount());
		assertEquals(91, data.getNpcParties(300540000).size());
		assertEquals(1, data.getNpcParties(301390000).size());
		var portal = data.getDirectPortal(73);
		assertNotNull(portal);
		assertEquals(220080000, portal.start().worldId());
		assertEquals(3, portal.destination().groups().size());
		var userPortal = data.getDirectPortal(82);
		assertNotNull(userPortal);
		assertEquals("key_f5_legion_potal_d01", userPortal.needItem());
		assertEquals(11, userPortal.groupId());
		assertEquals(5, userPortal.invadeType());
		assertEquals(288, data.dynamicAreaCount());
		assertEquals(12654, java.util.stream.StreamSupport.stream(data.patterns().spliterator(), false)
			.filter(RetailPatternAI2::supports).count());
		for (int npcId : new int[] { 251812, 251813, 251814, 257300, 257305, 257310, 855729 }) {
			assertTrue(RetailPatternAI2.supports(data.getPattern(npcId)), data.getPattern(npcId).name());
		}
		var unsupportedGauge = java.util.stream.StreamSupport.stream(data.patterns().spliterator(), false)
			.filter(pattern -> pattern.events().keySet().stream().anyMatch(event -> event.startsWith("on_gauge_")))
			.filter(pattern -> !RetailPatternAI2.supports(pattern)).map(RetailAiData.Pattern::name).toList();
		assertTrue(unsupportedGauge.isEmpty(), unsupportedGauge.toString());
		var bossLocation = data.findLocationAlias(302400000, "Location_Boss");
		assertNotNull(bossLocation);
		assertEquals(1, bossLocation.size());
		assertEquals(206.430801f, bossLocation.get(0).x());
		assertEquals(249.652161f, bossLocation.get(0).y());
		assertEquals(976.699585f, bossLocation.get(0).z());
		assertEquals(0, bossLocation.get(0).direction());
		assertNull(data.findLocationAlias(302400000, "Missing_Alias"));
		var lightHeroesEnd = data.findLocationAlias(310160000, "IDAb1_Heroes_L_Airport_End");
		assertNotNull(lightHeroesEnd);
		assertEquals(81.177345f, lightHeroesEnd.get(0).x());
		var darkHeroesEnd = data.findLocationAlias(320160000, "IDAb1_Heroes_L_Airport_End");
		assertNotNull(darkHeroesEnd);
		assertEquals(94.626068f, darkHeroesEnd.get(0).x());
		assertTrue(RetailPatternAI2.supports(data.getPattern(806731)));
		assertTrue(RetailPatternAI2.supports(data.getPattern(806732)));
		for (int worldId : new int[] { 310160000, 320160000 }) {
			for (String variable : new String[] { "1st_door", "2nd_door", "3rd_door", "4th_door", "boss_die" }) {
				assertTrue(data.supportsConditionVariable(worldId, variable), worldId + ":" + variable);
			}
			assertEquals(9, data.getConditionSpawns(worldId).stream()
				.filter(spawn -> spawn.expression().contains("door") || spawn.expression().contains("boss_die")).count());
		}
		assertEquals(20, data.getConditionSpawns(310160000).stream()
			.filter(spawn -> spawn.expression().equals("boss_die == 1"))
			.flatMap(spawn -> spawn.groups().stream()).flatMap(group -> group.slots().stream())
			.flatMap(java.util.List::stream).flatMap(choice -> choice.members().stream())
			.filter(npc -> npc.id() == 806787).findFirst().orElseThrow().life());
		assertEquals(1, data.getConditionSpawns(310160000).stream()
			.filter(spawn -> spawn.expression().equals("boss_die == 1"))
			.flatMap(spawn -> spawn.groups().stream()).flatMap(group -> group.slots().stream())
			.flatMap(java.util.List::stream).flatMap(choice -> choice.members().stream())
			.filter(npc -> npc.id() == 806731).findFirst().orElseThrow().respawnTime());
		for (int worldId : new int[] { 300220000, 300600000 }) {
			for (String variable : new String[] { "lightdark_spawn", "nmdd_spawn", "nmdd_spawn_hard", "nmdde_boxspawn", "nmddh_boxspawn" }) {
				assertTrue(data.supportsConditionVariable(worldId, variable), worldId + ":" + variable);
			}
			assertEquals(13, data.getConditionSpawns(worldId).stream()
				.filter(spawn -> spawn.expression().contains("NmdD") || spawn.expression().contains("LightDark")).count());
		}
		assertEquals("SKILLCTG_HEAL", data.getSkillCategory(245));
		assertEquals(1250, data.getNpcScore(232855).value());
		assertTrue(data.supportsConditionVariable(300320000, "Condition_S4B"));
		assertEquals(220140000, data.findConditionWorldId("df4_m"));
		assertEquals(210040000, data.findConditionWorldId("LF3"));
		assertEquals(9, data.getConditionSpawns(300320000).stream()
			.filter(spawn -> spawn.expression().contains("Condition_S4B")).count());
		assertEquals(1501652, data.findStringId("STR_CHAT_IDTransform_Boss_GOSSIP_02"));
		assertEquals(349942, data.findStringId("STR_Chat_Raksha_Solo_boss_Skill_01"));
		assertEquals(1500178, data.findStringId("STR_CHAT_NPC_Robstin_Patterns_01"));
		assertEquals(1403373, data.findStringId("STR_MSG_F6_Event_G1_Po_Time_Start_01"));
		assertEquals("LC1_LF4_Teleport", data.getNpc(730218).aiName());
		assertEquals(15, data.getNpc(219358).sensoryRange());
		assertEquals(0, data.getNpc(219358).sensoryRangeShort());
		assertEquals(99999, data.getNpc(248031).talkDelay());
		assertEquals(10, data.getNpc(297591).talkDelay());
		assertEquals(0, data.getNpc(702010).talkDelay());
		assertEquals(50, data.getNpc(702857).talkDelay());
		assertEquals(1, data.getNpc(835026).talkDelay());
		assertNotNull(data.getPattern(219358));
		assertEquals("MiBGuard_ChiefC", data.getPattern(256693).name());
		assertEquals("95", data.getPattern(219358).event("on_battle_timer").get(0).conditions().get(0).value("percent"));
		assertTrue(data.findArea(310110000, "IDLF2a_Lab_SZ_SkillAlarm").isInside3D(500, 500, 100));
		assertTrue(data.findSkillAreas(301550000, 103).stream()
			.anyMatch(area -> area.isInside3D(640, 1278, 823)));
		assertTrue(data.hasResurrectArea(301700000, "AttributeShapeResurrect_Area1"));
		assertFalse(data.hasResurrectArea(301700000, "Missing_Area"));
		assertTrue(data.hasSkillArea(1745));
		assertFalse(data.hasSkillArea(999999));
		assertFalse(data.hasArea("ab1_ship_msg"));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(207504)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(218354)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(217893)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(214659)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(214664)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(237107)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(216526)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(230996)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(236727)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(219998)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(857460)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(857462)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(857464)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(231501)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(212648)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(212283)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(212874)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(216264)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(230820)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(282420)));
		assertEquals(true, RetailPatternAI2.supports(data.getPattern(219358)));
	}

	@Test
	void loadsOnlyCompleteWorldScopedRetailWaypoints() {
		WalkerData data = XmlDataLoader.loadRetailAiWaypointData(
			Path.of("src/main/resources/aion/definitions/compact/ai/ai-waypoints.xml").toFile(),
			Path.of("src/main/resources/aion/definitions/schemas/ai-waypoints.xsd").toFile());

		assertEquals(3042, data.size());
		var rudra = data.getWalkerTemplate("retail:300170000:npcpathpath_rudrawindc1");
		assertNotNull(rudra);
		assertEquals(557.167297f, rudra.getRouteSteps().get(0).getX());
		assertEquals(1358.827271f, rudra.getRouteSteps().get(0).getY());
		assertNotNull(data.getWalkerTemplate("retail:220020000:df2_e4_lycanwizardmboss_pet_7"));
		assertNull(data.getWalkerTemplate("retail:210050000:e3_cheru3_1"));
	}

	@Test
	void keepsStructurallySupportedRetailCoverageForLegacyGenericBossAi() throws Exception {
		Path aiDirectory = Path.of("src/main/resources/aion/definitions/compact/ai");
		RetailAiData data = RetailAiDefinitionLoader.load(
			aiDirectory.toFile(), aiDirectory.resolve("npc-ai.xml").toFile(),
			aiDirectory.resolve("ai-strings.xml").toFile(), aiDirectory.resolve("ai-areas.xml").toFile());
		Matcher matcher = java.util.regex.Pattern.compile(
			"<npc_template\\b[^>]*\\bnpc_id=\"(\\d+)\"[^>]*\\bai=\"(?:bomb|summoner)\"")
			.matcher(Files.readString(Path.of("src/main/resources/aion/data/static_data/npcs/npc_template.xml")));
		int supported = 0;
		while (matcher.find()) {
			if (RetailPatternAI2.supports(data.getPattern(Integer.parseInt(matcher.group(1))))) {
				supported++;
			}
		}

		assertEquals(132, supported);
	}
}
