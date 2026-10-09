package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for routes filled from the complete origin/history template index. */
class LegacyTemplateMirrorRouteRegressionTest {

	private record DialogRoute(int questId, String source, int npcId, int actionId, String targetNode,
			List<AfterCommitAction> afterCommit) {
	}

	@Test
	void itemCollectingMirrorsUseTheClientOwnedReportAndTurnInProtocol() throws Exception {
		// 2237 仍是 XML_RETENTION 成员（原版 SimpleCollectItem 行的交付 NPC 三方不一致：
		// retail-xml-retention.xml：SEMANTIC_GAP:REPORT_NPC_DIVERGENCE），不进原版编译集合
		// （RetailQuestDriver.java:413-440：只有 RETAIL_TABLE 行进 retailOwned*，XML_RETENTION 只记
		// reasons），因此定义由 XML 驱动、不在 S1 面内——报告页 SELECT5(2375) 与 20002
		// (CHECK_USER_HAS_QUEST_ITEM_SIMPLE) 双 prio 检查对逐字保留在交付 NPC 832822 上
		// （prio0 成功→reward + 窗 1，prio1 失败→CloseDialog）；物件 700145 已在物件 owner 收口
		// （2026-10-08）中收敛为纯采集掉落（can-act + loot），不带任何对话/发页路由。
		// 该行的原版校验动作是 20002（SIMPLE 变体）：原版 collect 行与 quest.xml 均无 39 检查轴。
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

		// P0-2/P0-3/P0c-19 规范形主语（SimpleCollectItem 2527/3096、SimpleTalk 11003/80356/80365）已随
		// 各族原版采纳退役（XML 删除、生产视图无 IR）：IR 测试退役三式（QE-146）下，原本固定的交付路由
		// 断言（QUEST_SELECT 直翻 REWARD + 分档窗 + 39/20002 检查对缺席）改锚 native 注册面；交付形状
		// 语义归各族族门承担（RetailSimpleCollectItemFamilyGateTest/RetailSimpleTalkFamilyGateTest 等）。
		// The P0-2/P0-3/P0c-19 canonical-delivery subjects (SimpleCollectItem 2527/3096, SimpleTalk
		// 11003/80356/80365) were retired with their family adoptions (XML deleted, no production IR):
		// under the QE-146 IR-test retirement triage their delivery-route assertions are re-anchored to
		// the native registration surface; the delivery shapes live in the family gates now.
		for (int questId : new int[] {2527, 3096}) {
			assertRetiredOwnedByNativeLane(questId, id -> SimpleCollectItemHandler.instance().owns(id));
		}
		for (int questId : new int[] {11003, 80356, 80365}) {
			assertRetiredOwnedByNativeLane(questId, id -> SimpleTalkHandler.instance().owns(id));
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
			if (RetiredQuestIds.contains(expected.questId())) {
				// 15672/20032/25672/27510（DataDriven 族）已随族采纳退役、生产视图无 IR（QE-146）：
				// 本行的后续对话/关窗断言改锚 native 注册面；页语义归 DataDriven 族门承担。
				// 15672/20032/25672/27510 (DataDriven) are retired with no production IR (QE-146):
				// their follow-up dialog assertions are re-anchored to the native registration surface.
				assertRetiredOwnedByNativeLane(expected.questId(),
					id -> DataDrivenNativeRuntime.instance().owns(id));
				continue;
			}
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
	void legacyTemplateCloseControlsDoNotChangeQuestState() {
		// 原断言：这批旧模板的「关窗控制」（dialog 10000/20000 → CloseDialog）不改变任务状态。各主语已随
		// 族采纳退役（XML 删除、生产视图无 IR）：IR 测试退役三式（QE-146）下改锚 native 注册面——关窗
		// 出口语义归各族族门承担（SimpleTalk/SimpleUseItem/DataDriven）。
		// Originally: the legacy close controls (dialog 10000/20000 -> CloseDialog) must not change quest
		// state. Every subject has been retired with its family adoption (XML deleted, no production IR):
		// under the QE-146 triage the rows are re-anchored to the native registration surface, and the
		// close-exit semantics live in the family gates.
		for (int questId : new int[] {1115, 1131, 1323, 35025}) {
			assertRetiredOwnedByNativeLane(questId, id -> SimpleTalkHandler.instance().owns(id));
		}
		for (int questId : new int[] {1309, 2107, 2321, 2435, 2578, 2670}) {
			assertRetiredOwnedByNativeLane(questId, id -> SimpleUseItemHandler.instance().owns(id));
		}
		for (int questId : new int[] {26820, 13950}) {
			assertRetiredOwnedByNativeLane(questId, id -> DataDrivenNativeRuntime.instance().owns(id));
		}
	}

	@Test
	void laterItemReportNpcsShowTheTurnInPageAndBoundStartItemsAreGranted() {
		// 原断言：这批交付 NPC 在 s1 态下发交付页 select2(1352)（并发放绑定接取物）。四个主语均已随
		// DataDriven 族采纳退役（XML 删除、生产视图无 IR）：按 QE-146 改锚 native 注册面，页语义归族门。
		// Originally: these turn-in NPCs showed the select2(1352) page from s1 (with bound start items).
		// All four subjects were retired with the DataDriven adoption (XML deleted, no production IR):
		// re-anchored to the native registration surface per QE-146; the page semantics live in the gate.
		for (int questId : new int[] {16976, 26976, 16985, 26985}) {
			assertRetiredOwnedByNativeLane(questId, id -> DataDrivenNativeRuntime.instance().owns(id));
		}
	}

	/**
	 * IR 测试退役三式（QE-146，2026-10-05 清扫批口径）：退役行（retention owner=RETAIL_TABLE）生产视图无
	 * IR，凡 find/compile 其定义的路由断言必红——本类对退役主语改锚 native 注册面：先守卫「确属退役且
	 * 无定义」，再要求所属家族 handler owns。行为语义归 DD/族门承担；方法名保留（Playbook 引用不断）。
	 * QE-146 IR-test retirement triage: a retired row (RETAIL_TABLE owner) has no production IR, so its
	 * route assertions are re-anchored to the native registration surface: guard that the row is really
	 * retired with no definition, then require the owning family handler. Behavior semantics stay with the
	 * family lanes; method names are kept.
	 */
	private static void assertRetiredOwnedByNativeLane(int questId, NativeLane lane) {
		assertTrue(RetiredQuestIds.contains(questId),
			() -> "quest " + questId + " must really be retired before its IR assertion is re-anchored");
		assertTrue(ProductionQuestDefinitions.catalog().find(questId).isEmpty(),
			() -> "quest " + questId + " is retired but the production view still exposes a definition");
		assertTrue(lane.owns(questId), () -> "quest " + questId + " must be owned by its native lane");
	}

	private interface NativeLane {
		boolean owns(int questId);
	}

	private static QuestDefinition compile(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 原版 overlay）。
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
