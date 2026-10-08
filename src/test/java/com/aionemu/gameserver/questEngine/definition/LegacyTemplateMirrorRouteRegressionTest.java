package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for routes filled from the complete origin/history template index. */
class LegacyTemplateMirrorRouteRegressionTest {

	private record DialogRoute(int questId, String source, int npcId, int actionId, String targetNode,
			List<AfterCommitAction> afterCommit) {
	}

	@Test
	void itemCollectingMirrorsUseTheClientOwnedReportAndTurnInProtocol() throws Exception {
		// 2237 仍是 XML_RETENTION 成员（真端 SimpleCollectItem 行的交付 NPC 三方不一致：
		// retail-xml-retention.xml：SEMANTIC_GAP:REPORT_NPC_DIVERGENCE），不进真端编译集合
		// （RetailQuestDriver.java:413-440：只有 RETAIL_TABLE 行进 retailOwned*，XML_RETENTION 只记
		// reasons），因此定义由 XML 驱动、不在 S1 面内——报告页 SELECT5(2375) 与 20002
		// (CHECK_USER_HAS_QUEST_ITEM_SIMPLE) 双 prio 检查对逐字保留在交付 NPC 832822 上
		// （prio0 成功→reward + 窗 1，prio1 失败→CloseDialog）；物件 700145 已在物件 owner 收口
		// （2026-10-08）中收敛为纯采集掉落（can-act + loot），不带任何对话/发页路由。
		// 该行的真端校验动作是 20002（SIMPLE 变体）：真端 collect 行与 quest.xml 均无 39 检查轴。
		// 2237 stays XML-retained and outside the S1 face (only RETAIL_TABLE rows enter the retail
		// driver), so its SELECT5(2375) report page and the 20002 dual-priority check pair are preserved
		// verbatim on the turn-in NPC 832822; the object 700145 was trimmed to the pure collection drop
		// (can-act + loot, no dialog routes) by the 2026-10-08 object-owner sweep. The retail row
		// carries no 39 check axis.
		int legacyNpc = 832822;
		QuestDefinition legacy = compile(2237);
		assertTrue(talkRoutes(legacy, "started", 700145, QuestDialogAction.QUEST_SELECT.id()).isEmpty(),
			"quest 2237 object keeps no page routes");
		assertPage(legacy, "started", legacyNpc, 2375);

		List<QuestTransition> legacyChecks = talkRoutes(legacy, "started", legacyNpc, 20002);
		assertEquals(2, legacyChecks.size(), "quest 2237 item checks");
		QuestTransition legacySuccess = legacyChecks.stream()
			.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
			.findFirst().orElseThrow();
		QuestTransition legacyFailure = legacyChecks.stream()
			.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
			.findFirst().orElseThrow();
		List<QuestCondition> legacyConditions = legacy.metadata().itemRequirements().stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count(), true))
			.toList();
		List<QuestAction> legacyRemovals = legacy.metadata().itemRequirements().stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();

		assertEquals("reward", legacySuccess.targetNode(), "quest 2237 success target");
		assertEquals(legacyConditions, legacySuccess.conditions(), "quest 2237 conditions");
		assertEquals(legacyRemovals, legacySuccess.actions(), "quest 2237 removals");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(5)), legacySuccess.afterCommit(),
			"quest 2237 success response");
		assertEquals("started", legacyFailure.targetNode(), "quest 2237 failure target");
		assertTrue(legacyFailure.conditions().isEmpty(), "quest 2237 failure conditions");
		assertTrue(legacyFailure.actions().isEmpty(), "quest 2237 failure actions");
		assertEquals(List.of(new AfterCommitAction.CloseDialog()), legacyFailure.afterCommit(),
			"quest 2237 failure response");

		// P0-2 规范形（SimpleCollectItem 族）：2527/3096 是 RETAIL_TABLE 成员，随页链退役改走
		// QUEST_SELECT(31；QE-017 页 id 与按钮动作共号) 交付——客户端模板索引把交付挂在报告
		// NPC 204811 / 798225（Pyrrha，start/end 同体）上，采集对象 700328/700423..426 本就
		// 不在客户端契约内。整组 HasItem 门控直翻 REWARD，领奖窗按分档查表（单档=5）；
		// 39/20002 检查对与报告页 2375 一并删除。
		// P0-2 canonical (the SimpleCollectItem family): 2527/3096 are RETAIL_TABLE members;
		// with the page chain retired they deliver on QUEST_SELECT (31; QE-017 page/action shared
		// numbering) — the client template index owns the turn-in on the report NPCs 204811 /
		// 798225 (Pyrrha); the collect objects 700328/700423..426 were never in the client
		// contract. The whole HasItem hand-in set flips REWARD with the tiered reward window
		// (single tier = 5); the 39/20002 check pairs and report page 2375 are gone.
		// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）——11003/80356/80365 是真端单步
		// item_check 行（Quest_SimpleTalk.xml：item_check=1 且 quest.xml 已声明 collect_item），交付
		// 与采集族同构（canonicalDelivery 单一真源），随 S1 页链退役一并改锚。P0c-19 裁定（真端对、
		// XML 错）：客户端模板索引 start/end 列声明接取/交付分离——11003 接取 798933(Phailos)、交付
		// 798942(Strabon)；80356 接取 831815、交付 831819；80365 接取 831827、交付 831819（真端表
		// acquired/reward 同对）；遗留 XML 的对称双 NPC 全形状是手工漂移，已退役（git 历史可回溯）。
		// P0-3 S1: the SimpleTalk item_check rows 11003/80356/80365 deliver through the same
		// canonicalDelivery shape as the collect family. P0c-19 adjudication (retail-right,
		// XML-wrong): the client template index declares the asymmetric acquire/hand-in split and the
		// retail table agrees; the legacy symmetric dual-NPC shape was drift and is retired.
		for (int[] mirror : new int[][] {
			{2527, 204811}, {3096, 798225}, {11003, 798942}, {80356, 831819}, {80365, 831819}}) {
			QuestDefinition definition = compile(mirror[0]);
			List<QuestTransition> delivery = talkRoutes(definition, "started", mirror[1],
				QuestDialogAction.QUEST_SELECT.id());
			assertEquals(1, delivery.size(), "quest " + mirror[0] + " canonical delivery route count");
			QuestTransition deliver = delivery.getFirst();
			assertEquals("reward", deliver.targetNode(), "quest " + mirror[0] + " delivery target");
			assertEquals(definition.metadata().itemRequirements().stream()
				.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count(), true))
				.toList(), deliver.conditions(), "quest " + mirror[0] + " delivery conditions");
			assertEquals(definition.metadata().itemRequirements().stream()
				.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
				.toList(), deliver.actions(), "quest " + mirror[0] + " delivery removals");
			int rewardWindow = QuestDialogPage
				.rewardWindowForTier(definition.metadata().rewardGroups().size() - 1).orElseThrow().id();
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(rewardWindow)), deliver.afterCommit(),
				"quest " + mirror[0] + " delivery response");
			assertTrue(talkRoutes(definition, "started", mirror[1], 39).isEmpty(),
				"quest " + mirror[0] + " legacy item-check route removed");
			assertTrue(talkRoutes(definition, "started", mirror[1], 20002).isEmpty(),
				"quest " + mirror[0] + " legacy simple item-check route removed");
			assertTrue(talkRoutes(definition, "started", mirror[1], 2375).isEmpty(),
				"quest " + mirror[0] + " legacy report page route removed");
		}
	}

	@Test
	void eventShardStartsUseTheClientOwnedSelectNonePage() throws Exception {
		for (int questId : List.of(50031, 50038, 50040, 50041)) {
			QuestDefinition definition = compile(questId);
			QuestTransition start = definition.transitions().stream()
				.filter(transition -> transition.sourceNode().equals("unaccepted"))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& Integer.valueOf(31).equals(talk.dialogId()))
				.findFirst().orElseThrow();
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(4762)), start.afterCommit(),
				"quest " + questId + " start page");
		}
	}


	@Test
	void uniqueClientGraphCandidatesUseOwnedAcceptAndReportPages() throws Exception {
		for (int questId : List.of(50089, 50090)) {
			QuestDefinition definition = compile(questId);
			for (int npcId : List.of(835680, 835681)) {
				List<QuestTransition> accepts = talkRoutes(definition, "unaccepted", npcId, 1002);
				assertEquals(1, accepts.size(), "quest " + questId + " accept route count");
				QuestTransition accept = accepts.getFirst();
				assertEquals("started", accept.targetNode(), "quest " + questId + " accept target");
				assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions(),
					"quest " + questId + " accept conditions");
				assertTrue(accept.actions().isEmpty(), "quest " + questId + " accept actions");
				assertEquals(List.of(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(1011)), accept.afterCommit(),
					"quest " + questId + " accept response");
			}
		}

		QuestDefinition windstream = compile(21080);
		for (int npcId : List.of(799231, 799427)) {
			// 客户端 select4 页（拿出沃夫冈的信）必须可达：无信时对话显示 select4 而非重复报告页。
			// The client select4 page (produce Wolfgang's letter) must be reachable: without the
			// item the talk shows select4 instead of repeating the report page.
			List<QuestTransition> selects = talkRoutes(windstream, "started", npcId, 31);
			assertEquals(2, selects.size(), "quest 21080 select branches for " + npcId);
			QuestTransition report = selects.stream()
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.findFirst().orElseThrow();
			QuestTransition letter = selects.stream()
				.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
				.findFirst().orElseThrow();
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(10002)), report.afterCommit(),
				"quest 21080 report page for " + npcId);
			assertTrue(report.conditions().stream()
					.anyMatch(condition -> condition instanceof QuestCondition.HasItem),
				"quest 21080 report branch requires the letter for " + npcId);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2034)), letter.afterCommit(),
				"quest 21080 letter page for " + npcId);
			List<QuestTransition> reward = talkRoutes(windstream, "started", npcId, 1009);
			QuestTransition success = reward.stream()
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.findFirst().orElseThrow();
			QuestTransition failure = reward.stream()
				.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
				.findFirst().orElseThrow();
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(5)), success.afterCommit(),
				"quest 21080 success response for " + npcId);
			assertEquals(List.of(new AfterCommitAction.CloseDialog()), failure.afterCommit(),
				"quest 21080 failure response for " + npcId);
		}
	}

	@Test
	void quest2920RewardChoicesUseTheHandlerProvenRewardWindows() throws Exception {
		QuestDefinition definition = compile(2920);
		for (int[] choice : List.of(new int[] {10010, 5, 1}, new int[] {10011, 6, 2})) {
			List<QuestTransition> routes = talkRoutes(definition, "started", 204141, choice[0]);
			assertEquals(1, routes.size(), "quest 2920 choice " + choice[0]);
			QuestTransition route = routes.getFirst();
			assertEquals("reward" + choice[2], route.targetNode(), "quest 2920 choice target " + choice[0]);
			assertTrue(route.conditions().isEmpty(), "quest 2920 choice conditions " + choice[0]);
			assertTrue(route.actions().isEmpty(), "quest 2920 choice actions " + choice[0]);
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(choice[1])), route.afterCommit(),
				"quest 2920 choice response " + choice[0]);
		}
	}

	@Test
	void handlerProvenFollowUpDialogsUseTheirPageTargetState() throws Exception {
		for (DialogRoute expected : List.of(
			new DialogRoute(1345, "reward", 204006, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(1535, "started", 204580, 10000, "reward1",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(5))),
			new DialogRoute(1535, "started", 204580, 10001, "reward2",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(6))),
			new DialogRoute(1535, "started", 204580, 10002, "reward3",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(7))),
			new DialogRoute(1640, "unaccepted", 730033, 10000, "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog())),
			// 1722 的 SETPRO3 是把 var0=2 推进到 var0=3（legacy handler 的 var+1），目标为 s3。
			// 1722's SETPRO3 advances var0=2 to var0=3 (the legacy handler's var+1), so the target is s3.
			new DialogRoute(1722, "s2", 278544, 10002, "s3",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(2002, "s10", 790002, 10003, "s11",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(2230, "started", 203621, 10000, "started",
				List.of(new AfterCommitAction.StartQuestTimer(1800),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(1687, "started", 204601, 10009, "reward1",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(1687, "started", 204601, 10019, "reward2",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(1687, "started", 204601, 10029, "reward3",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(2004, "v2", 203539, 10001, "v2",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2284, "step1", 798040, 10001, "step1",
				List.of(new AfterCommitAction.DeleteInteractionNpc(true),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(2303, "started", 798082, 10009, "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.ShowQuestDialog(1012))),
			new DialogRoute(2303, "started", 798082, 10019, "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.ShowQuestDialog(1097))),
			new DialogRoute(2332, "started", 798084, 10000, "reward1",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(5))),
			new DialogRoute(2332, "started", 798084, 10001, "reward2",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(6))),
			new DialogRoute(2332, "started", 798084, 10002, "reward3",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(7))),
			new DialogRoute(2333, "reward", 798084, 10001, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(3205, "s16", 804601, 10001, "s16",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(14112, "reward", 203195, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(14153, "reward", 204505, 10001, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(15672, "unaccepted", 806114, 1009, "unaccepted",
				List.of(new AfterCommitAction.ShowQuestDialog(5))),
			new DialogRoute(20032, "started", 799239, 10001, "started",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(20032, "started", 799258, 10001, "started",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(20032, "started", 799503, 10001, "started",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(25672, "unaccepted", 806116, 1009, "unaccepted",
				List.of(new AfterCommitAction.ShowQuestDialog(5))),
			new DialogRoute(27510, "unaccepted", 806079, 10000, "unaccepted",
				List.of(new AfterCommitAction.CloseDialog())))) {
			QuestDefinition definition = compile(expected.questId());
			List<QuestTransition> routes = talkRoutes(definition, expected.source(), expected.npcId(),
				expected.actionId()).stream()
				.filter(route -> route.targetNode().equals(expected.targetNode()))
				.toList();
			// 94636797a 起，材料不足时的回显自环与领奖路由共用同一个客户端动作，
			// 因此按期望目标节点筛选后再要求唯一，而不是要求整组动作只有一条路由。
			// Since 94636797a a material-failure self-loop shares the same client action as the reward
			// route, so filter by the expected target node before requiring a unique route.
			assertEquals(1, routes.size(), "quest " + expected.questId() + " action " + expected.actionId());
			QuestTransition route = routes.getFirst();
			assertEquals(expected.afterCommit(), route.afterCommit(),
				"quest " + expected.questId() + " action " + expected.actionId() + " response");
		}

		// 1640 的 SETPRO2 是双分支：有信件走结算，无信件回 select2_1（客户端 select2_1 页必须可达）。
		// Quest 1640's SETPRO2 is a two-branch pair: with the letter it settles; without it the
		// client select2_1 page (required reachable) is shown again.
		QuestDefinition teleporter = compile(1640);
		List<QuestTransition> setpro2 = talkRoutes(teleporter, "started", 730033, 10001);
		assertEquals(2, setpro2.size(), "quest 1640 SETPRO2 branch count");
		QuestTransition settle = setpro2.stream()
			.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
			.findFirst().orElseThrow();
		QuestTransition retry = setpro2.stream()
			.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
			.findFirst().orElseThrow();
		assertEquals("complete", settle.targetNode(), "quest 1640 settle target");
		assertEquals(List.of(new QuestCondition.HasItem(182201790, 1)), settle.conditions(),
			"quest 1640 settle conditions");
		assertEquals(List.of(
			new QuestAction.RemoveItem(182201790, 1),
			new QuestAction.GrantReward("GOLD", 0, 2020L, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 492677L, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0)), settle.actions(),
			"quest 1640 settle actions");
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.CloseDialog()), settle.afterCommit(),
			"quest 1640 settle response");
		assertEquals("started", retry.targetNode(), "quest 1640 retry target");
		assertTrue(retry.conditions().isEmpty(), "quest 1640 retry conditions");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1353)), retry.afterCommit(),
			"quest 1640 retry response");

		QuestTransition completedDialog = talkRoutes(teleporter, "complete", 730033, 10000).getFirst();
		assertEquals("complete", completedDialog.targetNode());
		assertTrue(completedDialog.afterCommit().contains(new AfterCommitAction.CloseDialog()));
	}

	@Test
	void handlerProvenItemChoicesRetainTheirItemChecksAndRemovalActions() throws Exception {
		QuestDefinition definition = compile(1535);
		for (int[] choice : List.of(
			new int[] {10000, 182201818, 5},
			new int[] {10001, 182201819, 3},
			new int[] {10002, 182201820, 1})) {
			QuestTransition route = talkRoutes(definition, "started", 204580, choice[0]).getFirst();
			assertEquals(List.of(new QuestCondition.HasItem(choice[1], choice[2], true)), route.conditions());
			assertEquals(List.of(
				new QuestAction.RemoveItem(182201818, -1),
				new QuestAction.RemoveItem(182201819, -1),
				new QuestAction.RemoveItem(182201820, -1)), route.actions());
		}
	}

	@Test
	void quest24154UsesTheExistingV1ItemPresentationRoute() throws Exception {
		QuestDefinition definition = compile(24154);
		assertTrue(talkRoutes(definition, "started", 204809, 31).isEmpty());
		assertTrue(talkRoutes(definition, "started", 204809, 1353).isEmpty());
		List<QuestTransition> routes = talkRoutes(definition, "v1", 204809, 1353);
		assertEquals(1, routes.size());
		assertEquals(List.of(
			new QuestAction.GiveItem(182215463, 1),
			new QuestAction.GiveItem(185000006, 1)), routes.getFirst().actions());
	}

	@Test
	void legacyTemplateCloseControlsDoNotChangeQuestState() throws Exception {
		for (DialogRoute expected : List.of(
			new DialogRoute(1115, "unaccepted", 203072, 10000, "unaccepted",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(1131, "shugo", 799093, 10000, "shugo",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(1309, "reward", 203830, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(1323, "reward", 203939, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2107, "k1", 203516, 10000, "k1",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2321, "reward", 790018, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2435, "reward", 204390, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2578, "reward", 204746, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2670, "reward", 204208, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(26820, "s1", 806233, 10000, "s1",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(35025, "started", 798972, 10000, "started",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(13950, "unaccepted", 806075, 20000, "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog())))) {
			QuestDefinition definition = compile(expected.questId());
			List<QuestTransition> routes = talkRoutes(definition, expected.source(), expected.npcId(),
				expected.actionId());
			assertEquals(1, routes.size(), "quest " + expected.questId() + " action " + expected.actionId());
			assertEquals(expected.targetNode(), routes.getFirst().targetNode(),
				"quest " + expected.questId() + " target");
			assertEquals(expected.afterCommit(), routes.getFirst().afterCommit(),
				"quest " + expected.questId() + " response");
		}
	}

	@Test
	void laterItemReportNpcsShowTheTurnInPageAndBoundStartItemsAreGranted() throws Exception {
		for (int[] route : List.of(
			new int[] {16976, 801762},
			new int[] {26976, 801764},
			new int[] {16985, 804864},
			new int[] {26985, 804866})) {
			QuestDefinition definition = compile(route[0]);
			assertPage(definition, "s1", route[1], 1352);
		}
	}

	private static QuestDefinition compile(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId).definition();
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId, int pageId) {
		List<QuestTransition> routes = talkRoutes(definition, source, npcId, 31);
		assertEquals(1, routes.size(), "quest " + definition.id() + " page route count");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), routes.getFirst().afterCommit(),
			"quest " + definition.id() + " page response");
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId,
			int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(dialogId).equals(talk.dialogId()))
			.toList();
	}
}
