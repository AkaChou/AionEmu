package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 1006 的职业选择、奖励预览入口、领奖完成合同与 Belpartan 飞行传送入口。
 * Verifies quest 1006 class selection, reward-preview ingress, completion contracts, and the
 * Belpartan flight-teleport ingress.
 */
class Quest1006ClientDialogAlignmentTest {
	private static final int PERNOS = 790001;
	private static final int BELPARTAN = 205000;

	@Test
	void returnsTheRewardWindowWhenTheClientUsesQuestSelectionIngress() throws Exception {
		QuestDefinition definition = load().definition();

		assertNode(definition, "s5", QuestStatus.START, Map.of("var0", 5));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 5));

		QuestTransition sorcererChoice = route(definition, "s5", "reward", PERNOS,
			QuestDialogAction.SETPRO9);
		assertEquals(List.of(new QuestCondition.PlayerClassIs(PlayerClass.MAGE)), sorcererChoice.conditions());
		assertEquals(List.of(), sorcererChoice.actions());
		assertEquals(List.of(
			new AfterCommitAction.SetPlayerClass(PlayerClass.SORCERER),
			new AfterCommitAction.TeleportPlayer(210010000, 245.14868f, 1639.1372f, 100.35713f, (byte) 60),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			sorcererChoice.afterCommit());

		assertRewardPreview(definition, QuestDialogAction.USE_OBJECT);
		assertRewardPreview(definition, QuestDialogAction.QUEST_SELECT);
		assertRewardPreview(definition, QuestDialogAction.SELECT_QUEST_REWARD);

		long completionRoutes = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward")
				&& transition.targetNode().equals("complete")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == PERNOS)
			.count();
		assertEquals(16, completionRoutes);
	}

	/**
	 * Belpartan 步必须由任务列表行选择（QUEST_SELECT/31）进入：客户端点任务行发 31，
	 * 退役 handler 的 START_DIALOG 同为 31；USE_OBJECT(-1) 是交互物动作，此处客户端不会发。
	 * The Belpartan step must be entered by the quest-list row selection (QUEST_SELECT/31): the
	 * client sends 31 for a row click and the retired handler fired on START_DIALOG (31) as well;
	 * USE_OBJECT(-1) is the quest-object action and is never sent here.
	 */
	@Test
	void routesTheBelpartanFlightTeleportFromTheQuestRowSelection() throws Exception {
		QuestDefinition definition = load().definition();

		QuestTransition belpartan = route(definition, "s99", "s50", BELPARTAN, QuestDialogAction.QUEST_SELECT);
		assertEquals(List.of(), belpartan.conditions());
		assertEquals(List.of(), belpartan.actions());
		assertEquals(List.of(
			new AfterCommitAction.FlightTeleport(1001),
			new AfterCommitAction.StartInvisibleTimer(43, new QuestTimerPolicy(
				new QuestTimerPolicy.Identity("1006-return", QuestTimerPolicy.Scope.PLAYER_QUEST),
				QuestTimerPolicy.Persistence.SESSION, QuestTimerPolicy.OverwritePolicy.REPLACE,
				QuestTimerPolicy.Delivery.AT_MOST_ONCE)),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			belpartan.afterCommit());

		// 本 NPC 只允许行选择动作：绑到 USE_OBJECT(-1) 的活步在本发版后永远不会被客户端触发。
		// Only the row selection may be bound here: a live step bound to USE_OBJECT(-1) is never
		// triggered by the client since this release.
		assertEquals(List.of(QuestDialogAction.QUEST_SELECT.id()), definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == BELPARTAN)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).dialogId())
			.distinct()
			.toList());

		QuestTransition wave = definition.transitions().stream()
			.filter(transition -> "s50".equals(transition.sourceNode())
				&& "s51".equals(transition.targetNode()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing quest 1006 wave route: s50 -> s51"));
		assertEquals(List.of(
			new AfterCommitAction.SpawnNpc("wave-1", 211042, new QuestSpawnLocation.Fixed(310010000,
				QuestInstanceTarget.currentOrDefault(), 224.073f, 239.1f, 206.7f, (byte) 0)),
			new AfterCommitAction.AddNpcAggro(211042, 1000),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			wave.afterCommit());
	}

	private static void assertRewardPreview(QuestDefinition definition, QuestDialogAction action) {
		QuestTransition preview = route(definition, "reward", "reward", PERNOS, action);
		assertEquals(List.of(), preview.conditions());
		assertEquals(List.of(), preview.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target, int npcId,
		QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode())
				&& target.equals(transition.targetNode())
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing quest 1006 route: "
				+ source + " -> " + target + " " + action));
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest1006ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/1006.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1006.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
