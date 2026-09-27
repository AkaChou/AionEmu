package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 1553 将起始接取、中间说话镜子注入魔力、佩兰托军团长询问与皮埃拉完成交付限定在各自正规 NPC。
 * Verifies quest 1553 confines its start, talking mirror magic infusion, Perento inquiry, and Piera reward completion to their retail NPC owners.
 */
class Quest1553ClientDialogAlignmentTest {
	private static final int START_NPC = 203786;
	private static final int TALKING_MIRROR_NPC = 730051;
	private static final int PERENTO_NPC = 204500;
	private static final int PIERA_NPC = 204584;

	private static final int INITIAL_MIRROR_ITEM = 182201794;
	private static final int INFUSED_MIRROR_ITEM = 182201795;

	@Test
	void verifiesQuest1553DefinitionContractAndOwnerIsolation() {
		QuestDefinition definition = load().definition();

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		// QE-051：客户端 QUEST_Q1553.html 共 3 行，末行为领奖行，reward 投影 = 2（批次 1-7 已收口）。
		// QE-051: QUEST_Q1553.html has 3 journal rows and the last one is the reward row, so the REWARD
		// projection is 2 (closed by batches 1-7).
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 2));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		// 1. 迪亚娜 (203786) 为唯一的接任务 NPC。S2：接取窗由 QUEST_SELECT 直发（页 4），
		// select1 页梯与 1007 中转随规范接取段退场。
		// 1. Diana (203786) is the only accept NPC. S2 canonical accept: QUEST_SELECT opens page 4 directly.
		QuestTransition startDialog = route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT);
		assertContract(startDialog, "unaccepted", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())));

		QuestTransition accept = route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(new QuestAction.GiveItem(INITIAL_MIRROR_ITEM, 1)), accept.actions());

		QuestTransition inProgressDiana = route(definition, "started", START_NPC, QuestDialogAction.QUEST_SELECT);
		assertContract(inProgressDiana, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())));
		assertTrue(routes(definition, "reward", START_NPC).isEmpty());

		// 2. 会说话的镜子 (730051) 仅作为第 1 步 (started) 交互对象，严禁作为 Start / Complete NPC
		assertTrue(routes(definition, "unaccepted", TALKING_MIRROR_NPC).isEmpty());
		assertTrue(routes(definition, "s1", TALKING_MIRROR_NPC).isEmpty());
		assertTrue(routes(definition, "reward", TALKING_MIRROR_NPC).isEmpty());

		QuestTransition mirrorSelect2 = route(definition, "started", TALKING_MIRROR_NPC, QuestDialogAction.QUEST_SELECT);
		assertContract(mirrorSelect2, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())));

		QuestTransition mirrorSelect21 = route(definition, "started", TALKING_MIRROR_NPC, QuestDialogAction.SELECT2_1);
		assertContract(mirrorSelect21, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())));

		QuestTransition mirrorSetpro1 = route(definition, "started", TALKING_MIRROR_NPC, QuestDialogAction.SETPRO1);
		assertEquals("s1", mirrorSetpro1.targetNode());
		assertEquals(List.of(
			new QuestAction.GiveItem(INFUSED_MIRROR_ITEM, 1),
			new QuestAction.RemoveItem(INITIAL_MIRROR_ITEM, 1)
		), mirrorSetpro1.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()
		), mirrorSetpro1.afterCommit());

		// 3. 佩兰托 (204500) 仅作为第 2 步 (stage1) 交互对象，严禁作为 Start / Complete NPC
		assertTrue(routes(definition, "unaccepted", PERENTO_NPC).isEmpty());
		assertTrue(routes(definition, "started", PERENTO_NPC).isEmpty());
		assertTrue(routes(definition, "reward", PERENTO_NPC).isEmpty());

		QuestTransition perentoSelect3 = route(definition, "s1", PERENTO_NPC, QuestDialogAction.QUEST_SELECT);
		assertContract(perentoSelect3, "s1", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())));

		QuestTransition perentoSelect31 = route(definition, "s1", PERENTO_NPC, QuestDialogAction.SELECT3_1);
		assertContract(perentoSelect31, "s1", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_1.id())));

		QuestTransition perentoSetpro2 = route(definition, "s1", PERENTO_NPC, QuestDialogAction.SETPRO2);
		assertEquals("s2", perentoSetpro2.targetNode());
		assertEquals(List.of(), perentoSetpro2.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()
		), perentoSetpro2.afterCommit());

		// 4. 皮埃拉 (204584) 仅作为第 3 步奖励交付 NPC，严禁作为 Start NPC
		assertTrue(routes(definition, "unaccepted", PIERA_NPC).isEmpty());
		assertTrue(routes(definition, "started", PIERA_NPC).isEmpty());
		assertTrue(routes(definition, "s1", PIERA_NPC).isEmpty());
		// S2：交付 = QUEST_SELECT(s2→reward) 空门直翻领奖态并下发奖励窗；SELECT5 报告页与 1009 检查中转
		// 随规范交付段退场（未集齐零路由，关窗兜底交 DialogService）。
		// S2 canonical delivery: QUEST_SELECT(s2→reward) flips REWARD with the reward window; the report
		// page and the 1009 check relay retire with the canonical segment.
		QuestTransition pieraReport = route(definition, "s2", PIERA_NPC, QuestDialogAction.QUEST_SELECT);
		assertContract(pieraReport, "reward", List.of(), List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(deliveryWindowPage(definition.metadata()))));

		assertTrue(routes(definition, "reward", PIERA_NPC).stream().noneMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(QuestDialogAction.QUEST_SELECT.id()).equals(talk.dialogId())),
			"quest 1553 reward 态不再保留 SELECT5 报告页路由");

		// 完成流的领奖态预览出口（1009）保留：下发本档奖励窗（preview 形，cri=0 ⇒ 第 1 档）。
		// The completion flow keeps its REWARD-state preview exit (1009), showing this tier's reward window.
		QuestTransition pieraPreview = route(definition, "reward", PIERA_NPC, QuestDialogAction.SELECT_QUEST_REWARD);
		assertContract(pieraPreview, "reward", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));

		List<QuestTransition> completionRoutes = routes(definition, "reward", PIERA_NPC).stream()
			.filter(transition -> {
				Integer dialogId = ((QuestEvent.TalkToNpc) transition.event()).dialogId();
				return dialogId != null && dialogId >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
					&& dialogId <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
			})
			.toList();
		assertEquals(16, completionRoutes.size());
		for (QuestTransition completion : completionRoutes) {
			assertContract(completion, "complete", List.of(
				new QuestAction.GrantReward("EXP", 0, 2954681, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("AP", 0, 200, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.CompleteQuest(0)
			), List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())
			));
		}
	}

	private static void assertContract(QuestTransition transition, String target,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		assertEquals(target, transition.targetNode());
		assertEquals(List.of(), transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
		assertNull(transition.priority());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> matches = routes(definition, source, npcId).stream()
			.filter(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(npcId, action.id())))
			.toList();
		assertEquals(1, matches.size(), "quest 1553 " + source + " " + npcId + " " + action);
		return matches.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	/** 交付窗页（与 RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage 同口径：档位查表，零奖励组回落窗 1）。 */
	private static int deliveryWindowPage(QuestMetadata metadata) {
		return metadata.rewardGroups().isEmpty()
			? QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()
			: QuestDialogPage.rewardWindowForTier(metadata.rewardGroups().size() - 1).orElseThrow().id();
	}

	private static CompiledQuestDefinition load() {
		return ProductionQuestDefinitions.definitionInOverlay(1553);
	}
}
