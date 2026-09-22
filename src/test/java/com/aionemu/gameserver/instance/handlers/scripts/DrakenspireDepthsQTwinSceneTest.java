package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 龙脊深渊任务副本死亡事件处理器闸门。
 * Gate for the Drakenspire Depths quest-instance death-event handler.
 * <p>背景：{@code AggroList#getMostPlayerDamage()} 可能返回 null。若死亡事件中的阵营分支依赖击杀者，
 * 双子守护者死亡后的米西奥内/帕西娅等任务 NPC 会被静默跳过，任务 15300/25300 卡在对话步骤。
 * Background: {@code AggroList#getMostPlayerDamage()} may return null. If death-event race branches depend on
 * the killer, quest NPCs such as Masionel/Parsia are silently skipped after the twin protectors die.</p>
 */
class DrakenspireDepthsQTwinSceneTest {

	private static final Path SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenspireDepthsQInstance.java");
	private static final String INSTANCE_RACE = "sealSceneRaceQ != null ? sealSceneRaceQ : player.getRace()";

	@Test
	void postTwinSceneSpawnsQuestNpcWithoutKillerPlayer() throws IOException {
		String twinScene = twinScene(onDieMethod());

		assertTrue(twinScene.contains("if (sealSceneRaceQ != null || player != null) {"));
		assertTrue(twinScene.contains("switch (" + INSTANCE_RACE + ") {"));
		assertTrue(twinScene.contains("spawn(209863,"));
		assertTrue(twinScene.contains("spawn(209883,"));
		assertFalse(twinScene.contains("if (player != null) {"));
	}

	@Test
	void allOnDieRaceBranchesUseTheInstanceRace() throws IOException {
		String onDie = onDieMethod();

		assertFalse(onDie.contains("if (player != null) {"));
		assertFalse(onDie.contains("switch (player.getRace()) {"));
		assertEquals(12, occurrences(onDie));
	}

	private static String twinScene(String onDie) {
		int start = onDie.indexOf("case 237228:");
		int end = onDie.indexOf("case 237213:", start);
		assertTrue(start >= 0, "twin protector case must exist");
		assertTrue(end > start, "the case after the twin protector scene must exist");
		return onDie.substring(start, end);
	}

	private static int occurrences(String source) {
		int count = 0;
		int index = 0;
		String text = "switch (" + INSTANCE_RACE + ") {";
		while ((index = source.indexOf(text, index)) >= 0) {
			count++;
			index += text.length();
		}
		return count;
	}

	private static String onDieMethod() throws IOException {
		String source = Files.readString(SOURCE);
		String signature = "public void onDie(Npc npc)";
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
