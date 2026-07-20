package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class KromedesTrialInstanceTest {

	private static final Path SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/KromedesTrialInstance.java");
	private static final Path RETAIL_PATTERNS = Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_lf4_minho.xml");
	private static final Path AI_AREAS = Path.of(
			"src/main/resources/aion/definitions/compact/ai/ai-areas.xml");
	private static final Path AI_STRINGS = Path.of(
			"src/main/resources/aion/definitions/compact/ai/ai-strings.xml");
	private static final Path SPAWNS = Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300230000_Kromede's_Trial.xml");

	@Test
	void finalBossSelectionUsesRetailAngryJudgeMessageChain() throws IOException {
		String source = Files.readString(SOURCE);
		assertFalse(source.contains("onInstanceCreate(WorldMapInstance instance)"));
		assertFalse(source.contains("spawn(217005"));
		assertFalse(source.contains("spawn(217006"));
		assertFalse(source.contains("Rnd."));

		String spawns = Files.readString(SPAWNS);
		assertTrue(spawns.contains("<spawn npc_id=\"217006\">"));
		assertTrue(spawns.contains("x=\"668.567871\" y=\"774.373657\" z=\"216.88036\" h=\"60\""));

		String patterns = Files.readString(RETAIL_PATTERNS);
		int angryPattern = patterns.indexOf("<name>Cromede_Named_Angry</name>");
		int nextPattern = patterns.indexOf("<npc_ai_pattern>", angryPattern + 1);
		assertTrue(angryPattern >= 0);
		assertTrue(nextPattern > angryPattern);
		String body = patterns.substring(angryPattern, nextPattern);
		assertTrue(body.contains("<message_type>6404</message_type>"));
		assertTrue(body.contains("<npc_nameid>IDCromede_2up_Named_Judge_38_An</npc_nameid>"));
		assertTrue(body.contains("<despawn_self/>"));
	}

	@Test
	void onDieDoesNotSpawnPrivateClassTreasure() throws IOException {
		String source = Files.readString(SOURCE);
		String onDie = methodBody(source, "public void onDie(Npc npc)");

		assertFalse(source.contains("spawnClassTreasure"));
		for (String foreignChest : new String[] { "211861", "212333", "212335", "212338" }) {
			assertFalse(source.contains(foreignChest));
		}
	}

	@Test
	void woundedNpcSpawnsUseRetailPatterns() throws IOException {
		String source = Files.readString(SOURCE);
		for (String privateSpawn : new String[] { "spawn(217001", "spawn(217003", "spawn(217004" }) {
			assertFalse(source.contains(privateSpawn));
		}

		String patterns = Files.readString(RETAIL_PATTERNS);
		for (String retailSpawn : new String[] { "IDCromede_2up_Trigger_Wound_Wife_38_An",
			"IDCromede_2up_Trigger_Wound_AssiJudge_38_An", "IDCromede_2up_Trigger_Wound_Torture_38_An" }) {
			assertTrue(patterns.contains("<npc_nameid>" + retailSpawn + "</npc_nameid>"));
		}
	}

	@Test
	void kaligaTreasuryHintUsesRetailSensoryScript() throws IOException {
		String source = Files.readString(SOURCE);
		String onDie = methodBody(source, "public void onDie(Npc npc)");
		assertFalse(onDie.contains("case 216999"));
		assertFalse(source.contains("announceKaligaTreasury"));
		assertFalse(source.contains("removeSilverBladeRotan"));
		assertFalse(source.contains("1111370"));

		String patterns = Files.readString(RETAIL_PATTERNS);
		assertTrue(patterns.contains("<name>IDCromede_SensoryArea_BossDoor</name>"));
		assertTrue(patterns.contains("<on_user_enter_sensory_area>"));
		assertTrue(patterns.contains("<send_system_msg_by_user_indicator><user>USERI_EVENT_MAKER</user>"
			+ "<string_id>STR_QUEST_SAY_IDCromede_004</string_id></send_system_msg_by_user_indicator>"));

		String areas = Files.readString(AI_AREAS);
		assertTrue(areas.contains("<area world_id=\"300230000\" world_name=\"idcromede\" "
			+ "name=\"IDCromede_SensoryArea_BossDoor\" bottom=\"213.045425\" top=\"227.045425\">"));
		for (String point : new String[] { "561.241028\" y=\"756.814941", "561.440308\" y=\"782.309692",
			"588.809021\" y=\"782.268372", "589.051575\" y=\"756.332642" }) {
			assertTrue(areas.contains("x=\"" + point + "\""));
		}
		assertTrue(Files.readString(AI_STRINGS)
			.contains("<string name=\"STR_QUEST_SAY_IDCromede_004\" id=\"1111370\"/>"));
		assertTrue(Files.readString(SPAWNS)
			.contains("x=\"566.783936\" y=\"762.207581\" z=\"219.712234\""));
	}

	@Test
	void questOwnsRetailMoviesAndFinalBossCompletion() throws IOException {
		String source = Files.readString(SOURCE);
		String onDie = methodBody(source, "public void onDie(Npc npc)");
		String onEnterZone = methodBody(source, "public void onEnterZone(Player player, ZoneInstance zone)");

		assertFalse(onDie.contains("217005"));
		assertFalse(onDie.contains("217006"));
		assertFalse(source.contains("sendMovie(player, 455)"));
		assertFalse(onEnterZone.contains("KALIGA_DUNGEONS_300230000"));
		assertFalse(onEnterZone.contains("sendMovie(player, 454)"));
	}

	private static String methodBody(String source, String signature) {
		int signatureStart = source.indexOf(signature);
		assertTrue(signatureStart >= 0, signature + " must exist");
		int bodyStart = source.indexOf('{', signatureStart);
		assertTrue(bodyStart >= 0, signature + " must have a method body");

		int depth = 0;
		for (int i = bodyStart; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(bodyStart + 1, i);
				}
			}
		}
		throw new AssertionError(signature + " method body was not closed");
	}
}
