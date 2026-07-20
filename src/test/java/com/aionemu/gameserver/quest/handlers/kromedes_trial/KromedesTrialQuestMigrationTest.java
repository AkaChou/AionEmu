package com.aionemu.gameserver.quest.handlers.kromedes_trial;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class KromedesTrialQuestMigrationTest {

	private static final Path QUEST_18602 = Path.of(
			"src/main/java/com/aionemu/gameserver/quest/handlers/kromedes_trial/_18602Nightmare_In_Shining_Armor.java");
	private static final Path QUEST_28602 = Path.of(
			"src/main/java/com/aionemu/gameserver/quest/handlers/kromedes_trial/_28602Into_The_Unknown.java");
	private static final Path OLD_AI = Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/kromedesTrial/Maga_Potion_Temple_VaultAI2.java");
	private static final Path NPC_TEMPLATES = Path.of(
			"src/main/resources/aion/data/static_data/npcs/npc_template.xml");
	private static final Path SPAWNS = Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300230000_Kromede's_Trial.xml");

	@Test
	void relicKeyObjectRunsRetailMovie454Chain() throws IOException {
		for (String source : questSources()) {
			assertTrue(source.contains("730308, 700939"));
			assertTrue(source.contains("checkItemExistence(env, 185000109, 1, true)"));
			assertTrue(source.contains("changeQuestStep(env, 1, 2, false)"));
			assertTrue(source.contains("playQuestMovie(env, 454)"));
			assertTrue(source.contains("QuestService.addNewSpawn(300230000, player.getInstanceId(), 282089, 653f, 774f, 216f"));

			String movieEnd = methodBody(source, "public boolean onMovieEndEvent(QuestEnv env, int movieId)");
			assertTrue(movieEnd.contains("687.631104f, 675.972412f"));
			assertTrue(movieEnd.contains("201.040802f, (byte) 90, TeleportAnimation.NO_ANIMATION"));
			assertFalse(movieEnd.contains("changeQuestStep"));
		}
	}

	@Test
	void corpseAndFinalBossUseRetailQuestActions() throws IOException {
		for (String source : questSources()) {
			assertTrue(source.contains("qe.registerQuestNpc(217005).addOnKillEvent(questId)"));
			assertTrue(source.contains("targetId == 217005 && qs.getQuestVarById(0) == 3"));
			assertTrue(source.contains("playQuestMovie(env, 455)"));
			assertFalse(source.contains("217006"));
			assertTrue(source.contains("getSkill(player, 19288, 1, player).useNoAnimationSkill()"));
			assertTrue(source.contains("defaultCloseDialog(env, 2, 3)"));
			assertFalse(source.contains("1111307"));
		}
	}

	@Test
	void staticDataRoutesRelicObjectAndRestoresCorpse() throws IOException {
		assertFalse(Files.exists(OLD_AI));

		String templates = Files.readString(NPC_TEMPLATES);
		int start = templates.indexOf("<npc_template npc_id=\"730308\"");
		assertTrue(start >= 0);
		int end = templates.indexOf('>', start);
		assertTrue(templates.substring(start, end).contains("ai=\"quest_use_item\""));
		assertFalse(templates.contains("ai=\"maga_potion_1\""));

		String spawns = Files.readString(SPAWNS);
		assertTrue(spawns.contains("<spawn npc_id=\"700939\">"));
		assertTrue(spawns.contains("<spot x=\"656.92\" y=\"585.74\" z=\"199.04\"/>"));
	}

	private static String[] questSources() throws IOException {
		return new String[] { Files.readString(QUEST_18602), Files.readString(QUEST_28602) };
	}

	private static String methodBody(String source, String signature) {
		int signatureStart = source.indexOf(signature);
		assertTrue(signatureStart >= 0, signature + " must exist");
		int bodyStart = source.indexOf('{', signatureStart);
		int depth = 0;
		for (int i = bodyStart; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}' && --depth == 0) {
				return source.substring(bodyStart + 1, i);
			}
		}
		throw new AssertionError(signature + " method body was not closed");
	}
}
