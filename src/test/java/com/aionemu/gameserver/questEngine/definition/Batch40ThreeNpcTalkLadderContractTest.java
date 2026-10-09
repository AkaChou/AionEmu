package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 40：三行「接取 -&gt; 和行 0 NPC 对话 -&gt; 和行 1 NPC 对话 -&gt; 向行 2 NPC 报告领奖」族
 * （11072/21081/24150）。
 * <p>
 * 三家客户端任务书都是三行、槽位 %0/%3/%6，且三家现在都由原版表供货（RETAIL_TABLE，含 21081）。
 * 页链按客户端契约：每行一个**阶段首屏**（行 0 = select2 页 1352、行 1 = select3 页 1693、行 2 = 领奖窗
 * 页 5），行内翻页（select2_1/select3_1）是客户端本地行为——服务端不得有导航路由；阶段推进由客户端
 * 结果页按钮承担，`SETPRO{K}` 与 `SET_SUCCEED` 是两条同义边（同一 var0 推进、同一动作集）。
 * 行 2 走规范交付（带门 `QUEST_SELECT` 直翻领奖态 + 档位奖励窗），select5 报告页与 39 检查中转已退场。
 * </p>
 * <p>
 * Locks batch 40: one talking owner per journal row. Accept stays on the accept NPC, rows 0/1 own the
 * derived stage head page (select2/select3) and advance through the synonymous SETPRO{K}/SET_SUCCEED
 * buttons, and row 2 owns the canonical gated delivery into the tiered reward window. In-stage page
 * flips stay client-local: the server registers no navigation route for them.
 * </p>
 */
class Batch40ThreeNpcTalkLadderContractTest {

	private record TalkQuest(int questId, int acceptNpc, int row0Npc, int row1Npc, int row2Npc,
		int workItemId, boolean workItemGrantedAtAccept) {
	}

	private static final List<TalkQuest> FAMILY = List.of(
		new TalkQuest(11072, 798937, 798907, 798960, 798937, 182206860, false),
		new TalkQuest(21081, 799225, 799332, 799217, 799202, 182214017, true),
		new TalkQuest(24150, 204702, 204733, 204734, 204702, 182215460, false)
	);

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		for (TalkQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertNode(definition, contract.questId(), "started", QuestStatus.START, 0);
			assertNode(definition, contract.questId(), "step1", QuestStatus.START, 1);
			assertNode(definition, contract.questId(), "step2", QuestStatus.START, 2);
			assertNode(definition, contract.questId(), "reward", QuestStatus.REWARD, 2);

			Set<Integer> rows = new LinkedHashSet<>();
			for (QuestNode candidate : definition.nodes()) {
				Integer row = candidate.projection().variables().get("var0");
				QuestStatus status = candidate.projection().status();
				if (row == null || row > 2 || (status != QuestStatus.START && status != QuestStatus.REWARD)) {
					continue;
				}
				rows.add(row);
			}
			assertEquals(Set.of(0, 1, 2), rows,
				() -> "quest " + contract.questId() + " owns a state per journal row");
		}
	}

	@Test
	void acceptChainStaysOnTheAcceptNpcOnly() throws Exception {
		for (TalkQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> starts = definition.transitions().stream()
				.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_1.id())
				.toList();
			assertEquals(1, starts.size(),
				() -> "quest " + contract.questId() + " must declare exactly one accept route");
			QuestEvent.TalkToNpc accept = (QuestEvent.TalkToNpc) starts.getFirst().event();
			assertEquals(contract.acceptNpc(), accept.npcId(),
				() -> "quest " + contract.questId() + " accept must stay on the accept NPC");
			assertEquals("started", starts.getFirst().targetNode(),
				() -> "quest " + contract.questId() + " accept must enter the first row state");
		}
	}

	@Test
	void rowOwnersDriveTheClientPageChain() throws Exception {
		for (TalkQuest contract : FAMILY) {
			int questId = contract.questId();
			QuestDefinition definition = definition(questId).definition();

			// 行 0：阶段首屏 = 客户端声明的 select2 页；行内翻页无服务端路由；推进 = SETPRO1 ≡ SET_SUCCEED。
			// Row 0: the derived select2 head page, no in-stage navigation route, SETPRO1 == SET_SUCCEED.
			assertStageHead(definition, "started", contract.row0Npc(), QuestDialogPage.SELECT2, questId);
			assertNoRoute(definition, "started", contract.row0Npc(), QuestDialogAction.SELECT2_1, questId);
			QuestTransition row0Advance = assertSynonymousAdvances(definition, "started",
				contract.row0Npc(), QuestDialogAction.SETPRO1, "step1", questId);
			List<QuestAction> row0Actions = new ArrayList<>();
			row0Actions.add(new QuestAction.SetVariable("var0", 1));
			if (!contract.workItemGrantedAtAccept()) {
				row0Actions.add(new QuestAction.GiveItem(contract.workItemId(), 1));
			}
			assertEquals(row0Actions, row0Advance.actions(),
				() -> "quest " + questId + " row 0 advance must move to row 1"
					+ (contract.workItemGrantedAtAccept() ? "" : " and hand out the work item"));

			// 行 1：阶段首屏 = select3 页；推进 = SETPRO2 ≡ SET_SUCCEED，收回行 0 发下的工作物品。
			// Row 1: the select3 head page; SETPRO2 == SET_SUCCEED and the work item is collected back.
			assertStageHead(definition, "step1", contract.row1Npc(), QuestDialogPage.SELECT3, questId);
			assertNoRoute(definition, "step1", contract.row1Npc(), QuestDialogAction.SELECT3_1, questId);
			QuestTransition row1Advance = assertSynonymousAdvances(definition, "step1",
				contract.row1Npc(), QuestDialogAction.SETPRO2, "step2", questId);
			List<QuestAction> row1Actions = new ArrayList<>();
			row1Actions.add(new QuestAction.SetVariable("var0", 2));
			if (!contract.workItemGrantedAtAccept()) {
				row1Actions.add(new QuestAction.RemoveItem(contract.workItemId(), 1));
			}
			// 接取就发工作物品的行（21081）在交付边收物品；行 0 发物品的行在行 1 收回。
			// Rows that hand the work item out at accept collect it on the delivery edge instead.
			assertEquals(row1Actions, row1Advance.actions(),
				() -> "quest " + questId + " row 1 advance must move to row 2");

			// 行 2：规范交付 = 带门 QUEST_SELECT 直翻领奖态 + 档位奖励窗；39 检查中转与 select5 报告页退场。
			// Row 2: the canonical gated delivery opens the tiered reward window; the 39 relay and the
			// select5 report page are retired (no route at all).
			List<QuestTransition> delivery = dialogRoutes(definition, "step2", contract.row2Npc(),
				QuestDialogAction.QUEST_SELECT).stream()
				.filter(route -> "reward".equals(route.targetNode()))
				.toList();
			assertEquals(1, delivery.size(),
				() -> "quest " + questId + " delivery route must be unique");
			List<QuestCondition> expectedConditions = contract.workItemGrantedAtAccept()
				? List.of(new QuestCondition.HasItem(contract.workItemId(), 1)) : List.of();
			List<QuestAction> expectedDeliveryActions = contract.workItemGrantedAtAccept()
				? List.of(new QuestAction.RemoveItem(contract.workItemId(), 1)) : List.of();
			if (contract.workItemGrantedAtAccept()) {
				assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), row1Advance.actions(),
					() -> "quest " + questId + " keeps the work item until the delivery edge");
			}
			assertEquals(expectedConditions, delivery.getFirst().conditions(),
				() -> "quest " + questId + " delivery gate");
			assertEquals(expectedDeliveryActions, delivery.getFirst().actions(),
				() -> "quest " + questId + " delivery item contract");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.ShowQuestDialog(
				deliveryWindowPage(definition.metadata()))), delivery.getFirst().afterCommit(),
				() -> "quest " + questId + " row 2 must open the tiered reward window");
			assertNoRoute(definition, "step2", contract.row2Npc(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, questId);
		}
	}

	@Test
	void rewardOwnerHoldsTheCompletionAndOtherNpcsDoNot() throws Exception {
		for (TalkQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> completions = definition.transitions().stream()
				.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
				.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
				.toList();
			assertFalse(completions.isEmpty(),
				() -> "quest " + contract.questId() + " must keep a reward-side completion route");
			assertTrue(completions.stream().allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == contract.row2Npc()),
				() -> "quest " + contract.questId() + " completion must stay on the row-2 NPC");
			assertTrue(completions.stream().flatMap(route -> route.actions().stream())
					.anyMatch(new QuestAction.CompleteQuest(0)::equals),
				() -> "quest " + contract.questId() + " must complete at reward index 0");
			// 无 NPC 键的自动领奖入口（QuestDialog(SELECTED_QUEST_AUTO_REWARD*)）必须落本任务且带同一档奖励：
			// 单档形是独占的 108，多档 / 职业可选形是 110.. 系列（21081 = 110..122，24150 = 110/111）。
			// Keyless auto-reward entries must stay on this quest with the same reward: the single-tier form is
			// the exclusive 108, the multi-tier / class-selectable form is the 110.. series.
			List<QuestTransition> autoReward = definition.transitions().stream()
				.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
				.filter(route -> route.event() instanceof QuestEvent.QuestDialog dialog
					&& (dialog.dialogId() == QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id()
						|| dialog.dialogId() >= QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id()))
				.toList();
			assertFalse(autoReward.isEmpty(),
				() -> "quest " + contract.questId() + " must keep a keyless auto-reward entry");
			assertTrue(autoReward.stream().flatMap(route -> route.actions().stream())
					.anyMatch(new QuestAction.CompleteQuest(0)::equals),
				() -> "quest " + contract.questId() + " auto-reward must grant the reward");
			List<Integer> autoRewardIds = autoReward.stream()
				.map(route -> ((QuestEvent.QuestDialog) route.event()).dialogId())
				.distinct().sorted().toList();
			assertTrue(autoRewardIds.stream().allMatch(id -> id == QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id()
					|| id >= QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id()),
				() -> "quest " + contract.questId() + " keyless entries must use the auto-reward ids"
					+ " but were " + autoRewardIds);
			if (autoRewardIds.contains(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id())) {
				assertEquals(List.of(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id()), autoRewardIds,
					() -> "quest " + contract.questId() + " must not mix the 108 form with the 110.. series");
			}
			Set<Integer> nonOwnerNpcs = Set.of(contract.row0Npc(), contract.row1Npc());
			boolean strayReward = definition.transitions().stream()
				.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())
				.anyMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& nonOwnerNpcs.contains(talk.npcId()));
			assertFalse(strayReward,
				() -> "quest " + contract.questId() + " must not keep row-0/row-1 reward windows");
		}
	}

	@Test
	void staleRewardSaveHealsToRowTwo() throws Exception {
		for (TalkQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> stale = definition.transitions().stream()
				.filter(route -> route.sourceNode() == null)
				.filter(route -> "reward".equals(route.targetNode()))
				.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", 0))))
				.toList();
			assertEquals(1, stale.size(),
				() -> "quest " + contract.questId() + " must heal the pre-migration reward save");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), stale.getFirst().actions(),
				() -> "quest " + contract.questId() + " heals to the report row");
		}
	}

	private static void assertNode(QuestDefinition definition, int questId, String label,
		QuestStatus status, int row) {
		QuestNode node = definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
		assertEquals(status, node.projection().status(),
			() -> "quest " + questId + " node " + label + " status");
		assertEquals(row, node.projection().variables().get("var0"),
			() -> "quest " + questId + " node " + label + " row");
	}

	/** 阶段首屏：该行 NPC 对 QUEST_SELECT 只下发客户端声明的页。 / Derived stage head page. */
	private static void assertStageHead(QuestDefinition definition, String source, int npcId,
		QuestDialogPage page, int questId) {
		List<QuestTransition> routes = dialogRoutes(definition, source, npcId, QuestDialogAction.QUEST_SELECT);
		assertEquals(1, routes.size(),
			() -> "quest " + questId + " stage head " + source + " + npc " + npcId + " must be unique");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), routes.getFirst().afterCommit(),
			() -> "quest " + questId + " stage head " + source + " + npc " + npcId + " page");
	}

	/** 行内翻页是客户端本地行为：服务端不得有该动作的路由。 / In-stage flip stays client-local. */
	private static void assertNoRoute(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, int questId) {
		assertTrue(dialogRoutes(definition, source, npcId, action).isEmpty(),
			() -> "quest " + questId + " must not route the client-local action " + action
				+ " on " + source + " + npc " + npcId);
	}

	/**
	 * 阶段推进两条同义边：客户端结果页按钮 `SETPRO{K}` 与服务端 `SET_SUCCEED` 必须落到同一目标、
	 * 同一动作集、同一 after-commit。
	 * The two synonymous advance buttons must agree on target, actions and after-commit.
	 */
	private static QuestTransition assertSynonymousAdvances(QuestDefinition definition, String source,
		int npcId, QuestDialogAction progress, String target, int questId) {
		QuestTransition viaProgress = singleRoute(definition, source, npcId, progress, questId);
		QuestTransition viaSucceed = singleRoute(definition, source, npcId,
			QuestDialogAction.SET_SUCCEED, questId);
		assertEquals(target, viaProgress.targetNode(),
			() -> "quest " + questId + " " + progress + " target");
		assertEquals(target, viaSucceed.targetNode(),
			() -> "quest " + questId + " SET_SUCCEED target");
		assertEquals(viaProgress.actions(), viaSucceed.actions(),
			() -> "quest " + questId + " " + progress + " and SET_SUCCEED must share the actions");
		assertEquals(viaProgress.afterCommit(), viaSucceed.afterCommit(),
			() -> "quest " + questId + " " + progress + " and SET_SUCCEED must share the after-commit");
		assertTrue(viaProgress.afterCommit().contains(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			() -> "quest " + questId + " advance refreshes visibility");
		return viaProgress;
	}

	private static QuestTransition singleRoute(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, int questId) {
		List<QuestTransition> routes = dialogRoutes(definition, source, npcId, action);
		assertEquals(1, routes.size(),
			() -> "quest " + questId + " route " + source + " + npc " + npcId + " + " + action
				+ " must be unique");
		return routes.getFirst();
	}

	private static List<QuestTransition> dialogRoutes(QuestDefinition definition, String source,
		int npcId, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == action.id())
			.toList();
	}

	/** 交付窗页（与 RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage 同口径：档位查表，零奖励组回落窗 1）。 */
	private static int deliveryWindowPage(QuestMetadata metadata) {
		return metadata.rewardGroups().isEmpty()
			? QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()
			: QuestDialogPage.rewardWindowForTier(metadata.rewardGroups().size() - 1).orElseThrow().id();
	}

	private static CompiledQuestDefinition definition(int questId) {
		return ProductionQuestDefinitions.definitionInOverlay(questId);
	}
}
