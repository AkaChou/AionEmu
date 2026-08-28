package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1354（上级飞行术考试）的 120 秒飞行倒计时、飞行环推进与奖励归属合同。
 * Locks quest 1354 (Practical Aerobatics) 120-second flight countdown, ring progression, and reward contracts.
 */
class Quest1354ClientDialogAlignmentTest {
	private static final Path QUEST_PATH = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/1354.xml");
	private static final int NPC_ID = 203983;

	@Test
	void locksFlightTimerAndRingProgressionContract() throws Exception {
		QuestDefinition definition = load();

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "t", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "r2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "r3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "r4", QuestStatus.START, Map.of("var0", 4));
		assertNode(definition, "r5", QuestStatus.START, Map.of("var0", 5));
		assertNode(definition, "r6", QuestStatus.START, Map.of("var0", 6));
		assertNode(definition, "r7", QuestStatus.START, Map.of("var0", 7));
		assertNode(definition, "r8", QuestStatus.START, Map.of("var0", 8));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 8));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 8));

		// 确认开始考试对话 (SETPRO1) 必须启动 120 秒 (2分钟) 倒计时，并关闭对话。
		// Confirming flight test start dialog (SETPRO1) must start 120-second (2 minutes) countdown and close dialog.
		QuestTransition startFlight = singleTalkRoute(definition, "started", NPC_ID, QuestDialogAction.SETPRO1);
		assertEquals("t", startFlight.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.StartQuestTimer(120,
				QuestTimerPolicy.session("countdown", QuestTimerPolicy.OverwritePolicy.REPLACE)),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), startFlight.afterCommit());

		// 验证飞行环顺序与到达终点取消计时器。
		// Verify flying ring order and timer cancellation at final ring.
		assertFlyingRing(definition, "t", "r2", "ERACUS_TEMPLE_AIR_BOOSTER_1", false);
		assertFlyingRing(definition, "r2", "r3", "ERACUS_TEMPLE_AIR_BOOSTER_4", false);
		assertFlyingRing(definition, "r3", "r4", "ERACUS_TEMPLE_AIR_BOOSTER_3", false);
		assertFlyingRing(definition, "r4", "r5", "ERACUS_TEMPLE_AIR_BOOSTER_6", false);
		assertFlyingRing(definition, "r5", "r6", "ERACUS_TEMPLE_AIR_BOOSTER_5", false);
		assertFlyingRing(definition, "r6", "r7", "ERACUS_TEMPLE_AIR_BOOSTER_2", false);
		assertFlyingRing(definition, "r7", "r8", "ERACUS_TEMPLE_AIR_BOOSTER_7", true);

		// 超时重置回 started。
		// Timeout resets back to started.
		for (String stepNode : List.of("t", "r2", "r3", "r4", "r5", "r6", "r7")) {
			QuestTransition timeout = transition(definition, stepNode, "started", new QuestEvent.QuestTimerEnd());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				timeout.afterCommit());
		}
	}

	private static void assertFlyingRing(QuestDefinition definition, String source, String target,
			String ring, boolean cancelsTimer) {
		QuestTransition transition = transition(definition, source, target,
			new QuestEvent.PassFlyingRing(ring));
		if (cancelsTimer) {
			assertEquals(List.of(
				new AfterCommitAction.CancelQuestTimer(
					new QuestTimerPolicy.Identity("countdown", QuestTimerPolicy.Scope.PLAYER_QUEST)),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				transition.afterCommit());
		} else {
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				transition.afterCommit());
		}
	}

	private static QuestTransition singleTalkRoute(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& talk.dialogId() == action.id())
			.toList();
		assertEquals(1, routes.size(), "quest 1354 " + source + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), "quest 1354 " + source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_PATH)) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
