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
 * 验证任务 21320 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。
 * Verifies quest 21320 confines its simple acceptance, report, and reward routes to their retail NPC owners.
 */
class Quest21320ClientDialogAlignmentTest {
	private static final int START_NPC = 205847;
	private static final int REPORT_NPC = 804753;

	@Test
	void keepsTheRetailSimpleStartReportAndRewardOwnersExclusive() {
		QuestDefinition definition = load().definition();

		assertTrue(definition.metadata().prerequisites().isEmpty());
		assertTrue(definition.metadata().startConditions().isEmpty());
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		QuestTransition offer = route(definition, "unaccepted", START_NPC,
			QuestDialogAction.QUEST_SELECT);
		// 接取入口页 = 客户端任务页声明的页：真端接取窗 4 只在客户端声明 ask_quest_accept 时可用，
		// 否则必须下发该任务页声明的信页（select_none 4762 / select1 1011），不然客户端 load fail。
		// The accept entry page is the page the client task HTML declares: the native ask window 4 only
		// works when the client declares ask_quest_accept, otherwise the declared letter page
		// (select_none 4762 / select1 1011) must be emitted or the client reports load fail.
		assertContract(offer, "unaccepted", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(
				ClientAcceptEntryPageAssertions.expectedEntryPage(definition.id()))));

		QuestTransition accept = route(definition, "unaccepted", START_NPC,
			QuestDialogAction.QUEST_ACCEPT_SIMPLE);
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());
		assertNull(accept.priority());

		QuestTransition finish = route(definition, "started", START_NPC,
			QuestDialogAction.FINISH_DIALOG);
		assertContract(finish, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
		assertNoRoute(definition, "started", START_NPC, QuestDialogAction.QUEST_SELECT);
		assertNoRoute(definition, "started", START_NPC, QuestDialogAction.SELECT_QUEST_REWARD);
		assertTrue(routes(definition, "reward", START_NPC).isEmpty());

		assertTrue(routes(definition, "unaccepted", REPORT_NPC).isEmpty());
		// P0-2 DD 尾片：接取/交付切真端规范形（页 4 / 分档窗）。
		// P0-2 DD tail slice: accept/delivery switch to the retail canonical shape (page 4 / tiered window).
		QuestTransition reportPage = route(definition, "started", REPORT_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertContract(reportPage, "reward", List.of(), List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
		// 1009 领奖中转随页链删除（P0-2 DD 尾片）。
		// The 1009 reward hop goes with the page chain (P0-2 DD tail slice).
		assertNoRoute(definition, "started", REPORT_NPC, QuestDialogAction.SELECT_QUEST_REWARD);

		for (QuestDialogAction previewAction : List.of(
			QuestDialogAction.USE_OBJECT, QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = route(definition, "reward", REPORT_NPC, previewAction);
			assertContract(preview, "reward", List.of(), List.of(
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
		}

		List<QuestTransition> completionRoutes = routes(definition, "reward", REPORT_NPC).stream()
			.filter(transition -> {
				Integer dialogId = ((QuestEvent.TalkToNpc) transition.event()).dialogId();
				return dialogId != null && dialogId >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
					&& dialogId <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
			})
			.toList();
		assertEquals(16, completionRoutes.size());
		for (QuestTransition completion : completionRoutes) {
			assertContract(completion, "complete", List.of(
				new QuestAction.GrantReward("GOLD", 0, 46470, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 1005193, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.CompleteQuest(0)), List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
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

	private static void assertNoRoute(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		assertTrue(routes(definition, source, npcId).stream()
			.noneMatch(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(npcId, action.id()))));
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> matches = routes(definition, source, npcId).stream()
			.filter(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(npcId, action.id())))
			.toList();
		assertEquals(1, matches.size(), "quest 21320 " + source + " " + npcId + " " + action);
		return matches.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			// 生产定义可能带无 source 的自愈边，按 source 过滤时必须容忍 null。
			// Production definitions may carry source-less recovery edges, so null sources are skipped.
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

	/** 生产驱动定义（已退役的 XML 只在 git 历史）。 / The production-driver definition (the retired XML lives only in git history). */
	private static CompiledQuestDefinition load() {
		return ProductionQuestDefinitions.definition(21320);
	}
}
