package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Full vertical proof for the production owners 1115 / 1127. */
class XmlQuestFamilyDefinitionTest {
	@Test
	void packagedDirectoryAndRetailOverlayOwnTheirRespectiveQuests() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		assertTrue(catalog.find(1115).isEmpty());
		assertTrue(catalog.find(1127).isPresent());
		assertTrue(ProductionQuestDefinitions.catalog().find(1115).isPresent());
	}

	@Test
	void elimMessageAdvancesThroughTheBearerThenReportsToTheEndNpc() throws Exception {
		CompiledQuestDefinition compiled = definition("1115.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();

		// 佩拉(203072)推进 var0 到 1，进入中间节点 v1；终端 NPC(203058) 再报告进入 reward。
		// Pela (203072) advances var0 to 1 into the v1 node; the end NPC (203058) then reports into reward.
		QuestTransition progress = talk(transitions, "started", 203072, 10000, "v1");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), progress.afterCommit());
		// 终端 NPC(203058) 报告：先展示 SELECT5 页，再选奖励推进到 reward。
		// End NPC (203058) report: shows the SELECT5 page, then the reward selection advances to reward.
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)),
			talk(transitions, "v1", 203058, 31, "v1").afterCommit());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(5)),
			talk(transitions, "v1", 203058, 1009, "reward").afterCommit());
		assertNotNull(talk(transitions, "unaccepted", 203072, 31, "unaccepted"));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1352)),
			talk(transitions, "started", 203072, 31, "started").afterCommit());

		assertEquals(1, varsOf(compiled, "v1").get("var0"));
		assertEquals(1, varsOf(compiled, "reward").get("var0"));
		List<QuestAction> rewardActions = completions(transitions, "reward");
		assertTrue(rewardActions.contains(new QuestAction.GrantReward("GOLD", 0, 680, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(rewardActions.contains(new QuestAction.GrantReward("EXP", 0, 2673, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(rewardActions.contains(new QuestAction.CompleteQuest(0)));
	}

	/**
	 * 同构两步报告链：中间 NPC 的 SETPRO1 推进 var0 到 1（k1 节点），终端 NPC 再报告进入 reward。
	 * 客户端 select2→SETPRO1 与交付段分属两个 NPC，中间必须有 var 节点区分阶段；双块规范段行（S2）
	 * 的交付段已换成带门 QUEST_SELECT 直翻领奖态（select5 报告页与 1009 检查中转退场）。
	 * Isomorphic two-step report chain: the mid-NPC's SETPRO1 advances var0 to 1 (k1 node),
	 * the end NPC then reports into reward. The client's select2→SETPRO1 and the hand-in belong to two
	 * distinct NPCs, so an intermediate var node must separate the phases; dual-block rows (S2) hand in
	 * through the gated QUEST_SELECT instead of the select5 page plus the 1009 relay.
	 */
	@Test
	void twoStepReportChainsKeepAnIntermediateVarNodeBetweenSetproAndReport() throws Exception {
		for (TwoStepReport quest : List.of(
			new TwoStepReport(1115, "v1", 203072, 203058, List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestSelectionDialog(10)), true),
			new TwoStepReport(3201, "k1", 804601, 204534, List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestSelectionDialog(10)), false),
			new TwoStepReport(4201, "k1", 205233, 204791, List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestSelectionDialog(10)), false),
			new TwoStepReport(39000, "k1", 800501, 800500, List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestSelectionDialog(10)), true),
			new TwoStepReport(49000, "k1", 800503, 800502, List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestSelectionDialog(10)), true),
			// 11109 的 SETPRO1 后关闭对话（零售设计），玩家再去找终端 NPC 报告。
			// 11109's SETPRO1 closes the dialog (retail design); the player then seeks the end NPC.
			new TwoStepReport(11109, "k1", 798979, 799075, List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), true),
			// 1158 的物件 700003 SETPRO1 后关闭对话（捡起印章），玩家再找 203128 交还印章领奖。
			// 1158's object 700003 SETPRO1 closes the dialog (pick up seal); player then returns it to 203128.
			new TwoStepReport(1158, "k1", 700003, 203128, List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), true))) {
			CompiledQuestDefinition compiled = definition(quest.id() + ".xml");
			List<QuestTransition> transitions = compiled.definition().transitions();

			// SETPRO1 推进 var0 到 1，进入中间节点，而非直达 reward。
			QuestTransition progress = talk(transitions, "started", quest.midNpcId(), 10000, quest.midNode());
			assertEquals(quest.setproAfterCommit(), progress.afterCommit(),
				"quest " + quest.id() + " SETPRO1 protocol");

			// 终端 NPC 报告：双块规范段行（S2）交付 = 空门 QUEST_SELECT(midNode→reward) 直翻领奖态并下发
			// 奖励窗，SELECT5 报告页与 1009 检查中转随规范交付段退场；其余行保持 SELECT5 页 + 1009 中转。
			// The end NPC reports: canonical (dual-block, S2) rows deliver through the empty-gate
			// QUEST_SELECT(midNode→reward) into the reward window; the other rows keep page 2375 + 1009.
			if (quest.canonicalDelivery()) {
				QuestTransition delivery = talk(transitions, quest.midNode(), quest.endNpcId(), 31, "reward");
				assertNotNull(delivery, "quest " + quest.id() + " canonical delivery edge");
				assertEquals(List.of(), delivery.conditions());
				assertEquals(List.of(), delivery.actions());
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.ShowQuestDialog(
					deliveryWindowPage(compiled.definition().metadata()))), delivery.afterCommit(),
					"quest " + quest.id() + " canonical delivery response");
				assertNull(talk(transitions, quest.midNode(), quest.endNpcId(), 1009, "reward"),
					"quest " + quest.id() + " 的 1009 检查中转必须随规范交付段退场");
			} else {
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)),
					talk(transitions, quest.midNode(), quest.endNpcId(), 31, quest.midNode()).afterCommit(),
					"quest " + quest.id() + " report page");
				assertEquals(List.of(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(5)),
					talk(transitions, quest.midNode(), quest.endNpcId(), 1009, "reward").afterCommit(),
					"quest " + quest.id() + " reward transition");
			}

			assertEquals(1, varsOf(compiled, quest.midNode()).get("var0"),
				"quest " + quest.id() + " mid node var0");
			assertEquals(1, varsOf(compiled, "reward").get("var0"),
				"quest " + quest.id() + " reward var0");
		}
	}

	private record TwoStepReport(int id, String midNode, int midNpcId, int endNpcId,
			List<AfterCommitAction> setproAfterCommit, boolean canonicalDelivery) {
	}

	/** 交付窗页（与 RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage 同口径：档位查表，零奖励组回落窗 1）。 */
	private static int deliveryWindowPage(QuestMetadata metadata) {
		return metadata.rewardGroups().isEmpty()
			? QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()
			: QuestDialogPage.rewardWindowForTier(metadata.rewardGroups().size() - 1).orElseThrow().id();
	}

	@Test
	void ancientCubeGivesTheWorkItemThenRewardsOnlyWithTheCollectedCube() throws Exception {
		CompiledQuestDefinition compiled = definition("1127.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();

		// quest_use_item must pass ACTION_ITEM_USE eligibility before the use-object event can fire.
		assertTrue(transitions.stream().anyMatch(t -> t.sourceNode().equals("started")
			&& t.targetNode().equals("started")
			&& t.event() instanceof QuestEvent.CanAct(int templateId, String actionType)
			&& templateId == 700001
			&& "ACTION_ITEM_USE".equals(actionType)));

		// Use-object on 700001 grants the cube and advances to v1.
		QuestTransition give = talk(transitions, "started", 700001, -1, "v1");
		assertTrue(give.actions().contains(new QuestAction.GiveItem(182200215, 1)));

		// The collect-check splits on whether the cube is held.
		QuestTransition enough = talk(transitions, "v1", 798008, 39, "reward");
		assertTrue(enough.conditions().contains(new QuestCondition.HasItem(182200215, 1)));
		assertTrue(enough.actions().contains(new QuestAction.RemoveItem(182200215, 1)));
		QuestTransition missing = talk(transitions, "v1", 798008, 39, "v1");
		assertTrue(missing.conditions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2716)), missing.afterCommit());

		// The reward path carries the full typed reward set.
		List<QuestAction> complete = completions(transitions, "reward");
		assertTrue(complete.contains(new QuestAction.GrantReward("GOLD", 0, 2400, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(complete.contains(new QuestAction.GrantReward("EXP", 0, 4015, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(complete.contains(new QuestAction.CompleteQuest(0)));
	}

	@Test
	void productionCatalogDoesNotRetainTheLegacyXmlQuestOwners() throws Exception {
		assertFalse(legacyScriptDataExists(), "quest_script_data directory must be fully removed");
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source, int npcId,
			Integer dialogId, String target) {
		return transitions.stream().filter(t -> java.util.Objects.equals(t.sourceNode(), source)
				&& t.targetNode().equals(target))
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
				&& java.util.Objects.equals(talk.dialogId(), dialogId))
			.findFirst().orElse(null);
	}

	/** Flattened action set shared by every reward->complete route. */
	private static List<QuestAction> completions(List<QuestTransition> transitions, String source) {
		return transitions.stream().filter(t -> t.sourceNode().equals(source) && t.targetNode().equals("complete"))
			.flatMap(t -> t.actions().stream()).toList();
	}

	private static java.util.Map<String, Integer> varsOf(CompiledQuestDefinition compiled, String label) {
		return compiled.definition().nodes().stream().filter(n -> n.label().equals(label))
			.findFirst().orElseThrow().projection().variables();
	}

	private CompiledQuestDefinition definition(String file) throws Exception {
		return ProductionQuestDefinitions.definitionInOverlay(Integer.parseInt(file.replace(".xml", "")));
	}
	private static boolean legacyScriptDataExists() {
		return java.nio.file.Files.exists(
			java.nio.file.Path.of("src/main/resources/aion/data/static_data/quest_script_data"));
	}

}
