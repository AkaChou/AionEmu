package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ShugoImperialTombMigrationTest {
	@Test
	void retailConditionsOwnWavesAndBosses() throws Exception {
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ShugoImperialTombInstance.java"));
		assertFalse(handler.contains("Condition_S2"));
		assertFalse(handler.contains("RetailConditionSpawnEngine"));
		for (String removed : new String[] {
			"scheduleRaid", "spawnRaidNpc", "RAID_WAVE_OFFSETS", "tomb.kills.", "spawn(219", "Future<?>",
			"GameThreadPoolServices", "onDropRegistered", "regDropItem", ".started_at", "startConditionStage"
		}) {
			assertFalse(handler.contains(removed), removed);
		}

		String ai = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/shugoImperialTomb/ShugoImperialTombStageStarterAI2.java"));
		assertTrue(ai.contains("case 831110 -> new Stage(\"Condition_S2\", 1401582)"));
		assertTrue(ai.contains("case 831111 -> new Stage(\"Condition_S3\", 1401583)"));
		assertTrue(ai.contains("case 831112 -> new Stage(\"Condition_S4\", 1401584)"));
		assertTrue(ai.contains("setVariable(getPosition().getWorldMapInstance(), stage.variable(), 1, 0)"));
		for (String removed : new String[] { "Crown_Prince_AdmirerAI2.java", "Empress_AdmirerAI2.java", "Emperor_AdmirerAI2.java" }) {
			assertFalse(Files.exists(Path.of("src/main/java/com/aionemu/gameserver/ai/instance/shugoImperialTomb", removed)));
		}

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		Matcher world = Pattern.compile("<world id=\"300560000\".*?</world>", Pattern.DOTALL).matcher(conditions);
		assertTrue(world.find());
		String block = world.group();
		assertTrue(block.contains("<variable name=\"condition_s2\"/>"));
		assertTrue(block.contains("<variable name=\"condition_s3\"/>"));
		assertTrue(block.contains("<variable name=\"condition_s4\"/>"));
		assertEquals(453, Pattern.compile("<condition id=").matcher(block).results().count());

		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_test_worldevent.xml"));
		String mappings = Files.readString(Path.of("src/main/resources/aion/definitions/compact/ai/npc-ai.xml"));
		for (String[] expected : new String[][] {
			{ "219508", "IDDF2Flying_event01_A_Fungy_55_Ae", "Condition_S2", "1" },
			{ "219516", "IDDF2Flying_event01_C_TowerKiller", "Condition_S3", "1" },
			{ "219524", "IDDF2Flying_event01_D_Towermonster01", "Condition_S4", "1" },
			{ "831130", "IDDF2Flying_event01_B_WavePortal1_55_Ae", "Condition_S2", "299" },
			{ "831304", "IDDF2Flying_event01_C_DefenseTower", "Condition_S3", "250" },
			{ "831305", "IDDF2Flying_event01_B_WavePortal2_55_Ae", "Condition_S4", "350" }
		}) {
			assertTrue(block.contains("<npc id=\"" + expected[0] + "\""), expected[0]);
			assertTrue(Pattern.compile("<npc id=\"" + expected[0] + "\"[^>]+ai=\"" + expected[1] + "\"")
				.matcher(mappings).find(), expected[0]);
			Matcher retailPattern = Pattern.compile("<npc_ai_pattern><name>" + expected[1] + "</name>.*?</npc_ai_pattern>")
				.matcher(patterns);
			assertTrue(retailPattern.find(), expected[1]);
			assertTrue(retailPattern.group().contains("<string>" + expected[2]
				+ "</string><set>0</set><modify>" + expected[3] + "</modify>"), expected[1]);
		}

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300560000_Shugo_Imperial_Tomb.xml"));
		for (int npcId : new int[] { 831110, 831111, 831112 }) {
			assertTrue(staticSpawns.contains("npc_id=\"" + npcId + "\""));
		}
		String templates = Files.readString(Path.of("src/main/resources/aion/data/static_data/npcs/npc_template.xml"));
		for (int npcId : new int[] { 831110, 831111, 831112 }) {
			assertTrue(Pattern.compile("npc_id=\"" + npcId + "\"[^>]+ai=\"shugo_imperial_tomb_stage_starter\"")
				.matcher(templates).find());
		}
	}
}
