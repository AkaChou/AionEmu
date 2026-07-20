package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DragonLordRefugeRetailMigrationTest {

	private static final Path HANDLERS = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts");
	private static final Path SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances");

	@Test
	void retailDataOwnsNormalAndAnguishedFlows() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String normal = worldBlock(conditions, "300520000");
		String anguished = worldBlock(conditions, "300630000");
		for (String world : new String[] { normal, anguished }) {
			assertEquals(18, count(world, "<variable "));
			assertEquals(51, count(world, "<condition "));
			for (String variable : new String[] { "god_spawn", "idtiamat_teleport_t2", "kahrun_spawn",
					"kalyndi_spawn", "relic_spawn", "surukanafalling", "tiamat_ground_spawn",
					"tiamat_spawn", "tiamat_treasurebox", "tiamatsheild_spawn" }) {
				assertTrue(world.contains("<variable name=\"" + variable + "\"/>"), variable);
			}
			for (String npcId : new String[] { "701502", "730625", "730694", "730699", "730700" }) {
				assertTrue(world.contains("npc id=\"" + npcId + "\""), npcId);
			}
		}
		for (String npcId : new String[] { "219359", "219360", "219361", "219362", "219488", "219489",
				"219491", "219492", "701542" }) {
			assertTrue(normal.contains("npc id=\"" + npcId + "\""), npcId);
		}
		for (String npcId : new String[] { "236274", "236275", "236276", "236277", "856020", "856021",
				"856023", "856024", "702729" }) {
			assertTrue(anguished.contains("npc id=\"" + npcId + "\""), npcId);
		}

		assertMinimalHandler("DragonLordRefugeInstance.java");
		assertMinimalHandler("AnguishedDragonLordRefugeInstance.java");
	}

	@Test
	void retailStaticSpawnsAndScriptAisOwnTheRemainingLifecycle() throws Exception {
		assertStaticSpawns("300520000_Dragon_Lord_Refuge.xml",
			new String[] { "219365", "219366", "219367", "219368" });
		assertStaticSpawns("300630000_Anguished_Dragon_Lord's_Refuge.xml",
			new String[] { "236278", "236279", "236280", "236281" });

		String kahrun = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/dragonLordRefuge/KahrunAI2.java"));
		String hardKahrun = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/anguishedDragonLordRefuge/IDTiamat2HardKahrunAI2.java"));
		for (String source : new String[] { kahrun, hardKahrun }) {
			assertTrue(source.contains("spawn(283154"));
			assertFalse(source.contains("730625"));
		}

		String timer = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/dragonLordRefuge/IDTiamatTiamatTimer01AI2.java"));
		for (String expected : new String[] { "InstanceDeadlineScheduler.schedule", "1_800_000", "type == 201",
				"onRetailMessage(10010" }) {
			assertTrue(timer.contains(expected), expected);
		}
		String templates = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/npcs/npc_template.xml"));
		assertTrue(templates.contains("npc_id=\"283246\" level=\"60\" name_id=\"350000\" name=\" \""
			+ " height=\"1\" npc_type=\"NON_ATTACKABLE\" rank=\"DISCIPLINED\" rating=\"NORMAL\""
			+ " sensory_range=\"25\" tribe=\"XDRAKAN\" type=\"MONSTER\" ai=\"IDTiamat_Tiamat_Timer_01\""));

		for (String patternFile : new String[] { "npcaipatterns_tiamat_hue.xml",
				"npcaipatterns_tiamathard_ssh.xml" }) {
			String patterns = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai", patternFile));
			assertTrue(patterns.contains("<message_type>10010</message_type>"), patternFile);
			assertTrue(patterns.contains("<string_id>IDTIAMAT_TIAMAT_COUNTDOWN_OVER</string_id>"), patternFile);
		}
	}

	private static void assertMinimalHandler(String file) throws Exception {
		String source = Files.readString(HANDLERS.resolve(file));
		for (String legacy : new String[] { "GameThreadPoolServices", "Future", "onDropRegistered", "onDie(",
				"spawn(", "onInstanceCreate" }) {
			assertFalse(source.contains(legacy), file + ":" + legacy);
		}
		assertTrue(source.contains("removeEffect(20932)"), file);
		assertTrue(source.contains("removeEffect(20936)"), file);
	}

	private static void assertStaticSpawns(String file, String[] bosses) throws Exception {
		String source = Files.readString(SPAWNS.resolve(file));
		for (String npcId : bosses) {
			assertTrue(source.contains("npc_id=\"" + npcId + "\""), npcId);
		}
		for (String npcId : new String[] { "283150", "283154", "730633", "730634", "730635", "730636" }) {
			assertTrue(source.contains("npc_id=\"" + npcId + "\""), npcId);
		}
		for (String npcId : new String[] { "701502", "730694", "730699", "831990" }) {
			assertFalse(source.contains("npc_id=\"" + npcId + "\""), npcId);
		}
	}

	private static String worldBlock(String xml, String worldId) {
		int start = xml.indexOf("<world id=\"" + worldId + "\"");
		int end = xml.indexOf("</world>", start);
		return xml.substring(start, end);
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
