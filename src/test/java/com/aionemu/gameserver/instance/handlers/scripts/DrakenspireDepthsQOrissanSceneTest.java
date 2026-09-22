package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 龙脊深渊任务副本奥里萨死亡场景闸门。
 * Gate for the Drakenspire Depths quest-instance Orissan death scene.
 * <p>背景：237231（虚脱的奥里萨）死亡场景原先引用从未在任务副本生成的 209712/209777（非任务副本的
 * Scene 14 任务 NPC），排定任务必然 NPE，爆破手（209711/209776）喊话也不会播。场景里的台词由
 * {@code STR_CHAT_IDSeal_Bomber_Gossip_*} 归属爆破手，因此必须落在实际生成的那个 NPC 上。</p>
 */
class DrakenspireDepthsQOrissanSceneTest {

	private static final Path SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenspireDepthsQInstance.java");
	private static final String FALLBACK = "ensureExhaustedOrissanSpawned(npc)";

	@Test
	void immortalDeathBacksUpTheQuestKillTarget() throws IOException {
		// 237230 的死亡事件必须留一条幂等兜底：真端 pattern（on_die / 异常状态分支）或模板 AI 未生成
		// 237231 时，任务 15300/25300 会永久停在「消灭盘龙巢穴的奥里萨(0/1)」。
		// The death of 237230 must leave an idempotent backup: when neither the retail pattern (on_die / abnormal
		// state) nor the template AI spawns 237231, quests 15300/25300 stall at "Slay the Exhausted Orissan (0/1)".
		String onDie = onDie();
		int immortalCase = onDie.indexOf("case IMMORTAL_ORISSAN_NPC_ID:");
		assertTrue(immortalCase >= 0, "the immortal Orissan case must exist");
		assertTrue(onDie.substring(immortalCase).contains(FALLBACK),
			"the immortal Orissan case must call the fallback");

		String source = Files.readString(SOURCE);
		assertTrue(source.contains("IMMORTAL_ORISSAN_NPC_ID = 237230;"));
		assertTrue(source.contains("EXHAUSTED_ORISSAN_NPC_ID = 237231;"),
			"the fallback must spawn the quest kill target, not the immortal form");

		String fallback = methodBody(source, "private void ensureExhaustedOrissanSpawned(Npc immortal)");
		assertTrue(fallback.contains("Npc questTarget = getNpc(EXHAUSTED_ORISSAN_NPC_ID)")
				&& fallback.contains("!questTarget.getLifeStats().isAlreadyDead()"),
			"a working retail/AI spawn must not be duplicated");
		assertTrue(fallback.contains("spawn(EXHAUSTED_ORISSAN_NPC_ID,"));
		assertTrue(fallback.contains("}, 1500);"),
			"the check waits for the death chain (instance onDie runs before the AI death event)");
	}

	@Test
	void delayedSceneHelpersTolerateSuppressedSpawns() throws IOException {
		String source = Files.readString(SOURCE);
		assertTrue(methodBody(source, "private void raidSeal(final Npc npc)").contains("if (npc == null) {"));
		assertTrue(methodBody(source, "private void moveToSealForward(final Npc npc,").contains("if (npc == null) {"));
	}

	@Test
	void orissanSceneOnlyAddressesNpcsItSpawns() throws IOException {
		String scene = orissanScene();

		assertFalse(scene.contains("209712"), "the Q instance never spawns 209712 (non-quest scene npc)");
		assertFalse(scene.contains("209777"), "the Q instance never spawns 209777 (non-quest scene npc)");
		assertTrue(scene.contains("spawn(209711,") && scene.contains("getNpc(209711)"),
			"ELYOS must address the demolisher it spawns");
		assertTrue(scene.contains("spawn(209776,") && scene.contains("getNpc(209776)"),
			"ASMODIAN must address the demolisher it spawns");
	}

	@Test
	void demolisherGossipRunsInsideTheTaskThatSpawnsIt() throws IOException {
		String scene = orissanScene();

		assertSameTask(scene, "spawn(209711,", "1501314");
		assertSameTask(scene, "spawn(209776,", "1501314");
	}

	@Test
	void closingGossipWaitsForTheDemolisherInBothRaces() throws IOException {
		String scene = orissanScene();

		assertEquals(2, occurrences(scene, "}, 10000);"),
			"both races must close the scene after the demolisher exists");
		int elyosDoor = scene.indexOf("killNpc(getNpcs(700546));");
		int asmodianDoor = scene.indexOf("killNpc(getNpcs(700546));", elyosDoor + 1);
		assertTrue(scene.substring(elyosDoor, scene.indexOf("1501311", elyosDoor)).contains("getNpc(209711)"),
			"ELYOS closing gossip must resolve the spawned demolisher");
		assertTrue(scene.substring(asmodianDoor, scene.indexOf("1501311", asmodianDoor)).contains("getNpc(209776)"),
			"ASMODIAN closing gossip must resolve the spawned demolisher");
	}

	private static void assertSameTask(String scene, String spawnAnchor, String gossipAnchor) {
		int spawn = scene.indexOf(spawnAnchor);
		assertTrue(spawn >= 0, spawnAnchor + " must exist");
		int gossip = scene.indexOf(gossipAnchor, spawn);
		assertTrue(gossip > spawn, gossipAnchor + " must follow " + spawnAnchor);
		assertFalse(scene.substring(spawn, gossip).contains("}, 0);"),
			gossipAnchor + " must be sent from the task that spawns " + spawnAnchor);
	}

	private static int occurrences(String source, String text) {
		int count = 0;
		int index = 0;
		while ((index = source.indexOf(text, index)) >= 0) {
			count++;
			index += text.length();
		}
		return count;
	}

	private static String orissanScene() throws IOException {
		String source = Files.readString(SOURCE);
		int start = source.indexOf("case 237231: //Exhausted Orissan.");
		int end = source.indexOf("case 237216: //Grave Cavity Rendclaw.", start);
		assertTrue(start >= 0, "the exhausted Orissan case must exist");
		assertTrue(end > start, "the case after the exhausted Orissan scene must exist");
		return source.substring(start, end);
	}

	private static String onDie() throws IOException {
		return methodBody(Files.readString(SOURCE), "public void onDie(Npc npc)");
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
