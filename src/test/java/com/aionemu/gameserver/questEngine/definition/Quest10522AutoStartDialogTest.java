package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证任务 10522 的自动登记与 NPC 对话合同。 / Verifies quest 10522 automatic acquisition and NPC dialog contracts. */
class Quest10522AutoStartDialogTest {
	private static final int REWARD_NPC_ID = 806075;

	@Test
	void matchesLegacyAutoStartAndNpcDialogContract() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		// 领奖态投影必须等于引擎外写入方（CM_CREATIVITY_POINTS）留下的打包步数，否则该状态匹配不到领奖路由。
		// The REWARD projection must equal the packed step left by the engine-external writer
		// (CM_CREATIVITY_POINTS); otherwise the state matches no reward route.
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));
		assertEquals(List.of(List.of("finished:10521")), startConditionGroups(definition));

		assertAutoStart(definition, new QuestEvent.LevelUp());
		assertAutoStart(definition, new QuestEvent.ZoneMissionEnd());

		QuestTransition startDialog = transition(definition, "started",
			new QuestEvent.TalkToNpc(REWARD_NPC_ID, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals("started", startDialog.targetNode());
		assertEquals(List.of(), startDialog.conditions());
		assertEquals(List.of(), startDialog.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			startDialog.afterCommit());

		QuestTransition reward = transition(definition, "started",
			new QuestEvent.TalkToNpc(REWARD_NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals("reward", reward.targetNode());
		assertEquals(List.of(), reward.conditions());
		// 进入 REWARD 时由 reward 节点投影决定打包步数，事务动作不得再改写 var0。
		// The reward node projection owns the packed step on entry, so the transaction must not rewrite var0.
		assertEquals(List.of(), reward.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			reward.afterCommit());

		// 领奖态入口页：已在 REWARD 的玩家点任务行（客户端动作 31）必须拿回 select_success(10002)，
		// 该页唯一按钮 1009 再由 npc-complete 预览打开奖励窗口 5（旧 handler 的 31 -> 10002 / 1009 -> 5）。
		// Reward-state entry page: selecting the quest row (client action 31) while already at REWARD must
		// return select_success(10002); its only button 1009 then opens reward window 5 through the
		// npc-complete preview, matching the legacy handler contract.
		QuestTransition rewardEntry = transition(definition, "reward",
			new QuestEvent.TalkToNpc(REWARD_NPC_ID, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals("reward", rewardEntry.targetNode());
		assertEquals(List.of(), rewardEntry.conditions());
		assertEquals(List.of(), rewardEntry.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			rewardEntry.afterCommit());

		// 旧存档自愈：旧写入方只置 REWARD 而不写打包步数，定义又曾投影 var0=1，这类存档进入世界时归零。
		// Legacy save recovery: the old writer only set REWARD without writing the packed step while the
		// definition projected var0=1, so such saves are normalized on enter-world.
		QuestTransition recovery = unsourcedTransition(definition, new QuestEvent.EnterWorld(), "reward");
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.QuestVariableIs("var0", 1)), recovery.conditions());
		assertEquals(List.of(), recovery.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit());

		assertEquals(Set.of(REWARD_NPC_ID), dialogNpcIds(definition));
	}

	private static void assertAutoStart(QuestDefinition definition, QuestEvent event) {
		QuestTransition transition = transition(definition, "unaccepted", event);
		assertEquals("started", transition.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), transition.afterCommit());
	}

	private static List<List<String>> startConditionGroups(QuestDefinition definition) {
		return definition.metadata().startConditionGroups().stream()
			.map(group -> group.conditions().stream()
				.map(condition -> condition.type() + ":" + condition.questId()).toList())
			.toList();
	}

	private static Set<Integer> dialogNpcIds(QuestDefinition definition) {
		return definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::npcId)
			.collect(java.util.stream.Collectors.toSet());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source) && candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static QuestTransition unsourcedTransition(QuestDefinition definition, QuestEvent event,
			String targetNode) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> candidate.targetNode().equals(targetNode))
			.filter(candidate -> candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/10522.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 10522.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
