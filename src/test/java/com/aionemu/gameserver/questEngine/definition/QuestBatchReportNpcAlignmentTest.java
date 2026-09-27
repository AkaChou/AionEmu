package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批量锁定 2-NPC 与多 NPC 报告/完成节点归属合同。
 * Locks 2-NPC and multi-NPC report/completion owner contracts across representative repaired quests.
 */
class QuestBatchReportNpcAlignmentTest {

	@Test
	void quest2485ReportsAndCompletesAtLegacyEndNpc() throws Exception {
		QuestDefinition def = load("2485").definition();
		int startNpc = 203331;
		int endNpc = 204407;
		/* 2485 走真端网格合成器：单槽 1 杀 = 满段节点名 a1（旧 XML 的 k1 标签已退役）。 */
		/* 2485 is grid-composed: the saturated single-slot node is the grid name a1, not k1. */
		String full = "a1";

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, full, startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own the report chain");
		assertEquals(1, talkRoutes(def, full, endNpc, QuestDialogAction.QUEST_SELECT).size());
		// P0-2 规范形：满段交付 = QUEST_SELECT，1009 中转删除。
		// Canonical since P0-2: the full-node delivery is QUEST_SELECT; the 1009 hop is gone.
		assertTrue(talkRoutes(def, full, endNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
			"canonical removed the full-node 1009 hand-in");
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest3329ReportsAndCompletesAtLegacyEndNpc() throws Exception {
		QuestDefinition def = load("3329").definition();
		int startNpc = 203909;
		int endNpc = 203956;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, "unaccepted", endNpc, QuestDialogAction.QUEST_SELECT).isEmpty(), "end NPC must not have start route");
		assertTrue(talkRoutes(def, "a4b6", startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own report chain");
		assertEquals(1, talkRoutes(def, "a4b6", endNpc, QuestDialogAction.QUEST_SELECT).size());
		// P0-2 规范形：满段交付 = QUEST_SELECT，1009 中转删除。
		// Canonical since P0-2: the full-node delivery is QUEST_SELECT; the 1009 hop is gone.
		assertTrue(talkRoutes(def, "a4b6", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
			"canonical removed the full-node 1009 hand-in");
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest13702ReportsAndCompletesAtLegacyEndNpc() throws Exception {
		QuestDefinition def = load("13702").definition();
		int startNpc = 802350;
		int endNpc = 802352;
		/* 13702 自 P0c-6 起走真端网格合成器：5 段计数槽的节点名是网格名（4 杀 = a4），不是串行阶梯名。 */
		/* 13702 is grid-composed since P0c-6: the saturated single-slot node is the grid name a4. */
		String full = "a4";

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, "unaccepted", endNpc, QuestDialogAction.QUEST_SELECT).isEmpty(), "end NPC must not have start route");
		assertTrue(talkRoutes(def, full, startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own report chain");
		assertEquals(1, talkRoutes(def, full, endNpc, QuestDialogAction.QUEST_SELECT).size());
		// P0-2 规范形：满段交付 = QUEST_SELECT，1009 中转删除（13702 同）。
		// Canonical since P0-2: the full-node delivery is QUEST_SELECT; the 1009 hop is gone (13702 alike).
		assertTrue(talkRoutes(def, full, endNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
			"canonical removed the full-node 1009 hand-in");
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest2414ReportsAndCompletesAtEndNpcWithIntermediateRoute() throws Exception {
		QuestDefinition def = load("2414").definition();
		int startNpc = 204369;
		int midNpc = 204361;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", midNpc, QuestDialogAction.QUEST_SELECT).size());
		// S2 规范形：交付搬到 NPC_REPORT 块 source（started1）的 QUEST_SELECT(31)，1009 中转退场。
		// S2 canonical: the delivery moves onto the report block source's QUEST_SELECT(31); the 1009 relay is gone.
		assertCanonicalChainDelivery(def, "started1", startNpc);
		assertEquals(List.of(startNpc), completionNpcs(def));
	}

	@Test
	void quest1605ReportsAndCompletesAtEndNpcWithProgressChains() throws Exception {
		QuestDefinition def = load("1605").definition();
		int startNpc = 204576;
		int mid1 = 204530;
		int mid2 = 204501;
		int endNpc = 204577;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", mid1, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "step1", mid2, QuestDialogAction.QUEST_SELECT).size());
		// S2 规范形：交付搬到 NPC_REPORT 块 source（step2）的 QUEST_SELECT(31)，1009 中转退场。
		// S2 canonical: the delivery moves onto the report block source's QUEST_SELECT(31); the 1009 relay is gone.
		assertCanonicalChainDelivery(def, "step2", endNpc);
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest1648ReportsAndCompletesAtEndNpcWithProgressChains() throws Exception {
		QuestDefinition def = load("1648").definition();
		int startNpc = 204545;
		int mid1 = 204612;
		int mid2 = 204500;
		int endNpc = 204590;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", mid1, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "step1", mid2, QuestDialogAction.QUEST_SELECT).size());
		// S2 规范形：交付搬到 NPC_REPORT 块 source（step2）的 QUEST_SELECT(31)，1009 中转退场。
		// S2 canonical: the delivery moves onto the report block source's QUEST_SELECT(31); the 1009 relay is gone.
		assertCanonicalChainDelivery(def, "step2", endNpc);
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest2654ReportsAndCompletesAtEndNpc() throws Exception {
		QuestDefinition def = load("2654").definition();
		int startNpc = 204775;
		// 真端表行 reward_npc_name=AkanNamed_50_Al(212314) 是交付权威（P0c-16 复験裁定：真端对、
		// XML 错）——老 XML 挂在 204655（Lasberg_Normal_Q1647，别家任务 Q1647 的 NPC）属漂移。
		// The retail row's reward_npc_name=AkanNamed_50_Al(212314) owns the turn-in (P0c-16
		// re-adjudication: retail is right, XML drifted); the legacy 204655 belongs to quest 1647.
		int endNpc = 212314;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, "started", startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own report chain");
		// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）。真端行 2654 只有
		// acquired=Betoni / reward=AkanNamed_50_Al 两列（Quest_SimpleTalk.xml:4326-4330），零 item_check
		// ——交付 = QUEST_SELECT(31) 空门直翻领奖态并按档位查表下发奖励窗；报告页 SELECT5 与
		// 1009 中转随页链退场。
		// P0-3 S1: the canonical delivery is the empty-gate QUEST_SELECT(31) into REWARD with the
		// tiered window; the SELECT5 report page and the 1009 hop are gone.
		List<QuestTransition> delivery = talkRoutes(def, "started", endNpc, QuestDialogAction.QUEST_SELECT);
		assertEquals(1, delivery.size(), "quest 2654 canonical delivery route count");
		assertEquals("reward", delivery.getFirst().targetNode(), "quest 2654 delivery target");
		assertTrue(delivery.getFirst().conditions().isEmpty(), "quest 2654 delivery gate");
		assertTrue(delivery.getFirst().actions().isEmpty(), "quest 2654 delivery actions");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(deliveryWindow(def))), delivery.getFirst().afterCommit(),
			"quest 2654 delivery window");
		assertTrue(talkRoutes(def, "started", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
			"canonical removed the full-node 1009 hand-in");
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest30061ReportsAndCompletesAtEndNpcWithIntermediateRoute() throws Exception {
		QuestDefinition def = load("30061").definition();
		int startNpc = 800165;
		int midNpc = 798927;
		int endNpc = 799381;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", midNpc, QuestDialogAction.QUEST_SELECT).size());
		// S2 规范形：交付搬到 NPC_REPORT 块 source（step1）的 QUEST_SELECT(31)，1009 中转退场。
		// S2 canonical: the delivery moves onto the report block source's QUEST_SELECT(31); the 1009 relay is gone.
		assertCanonicalChainDelivery(def, "step1", endNpc);
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest4011ReportsAndCompletesAtEndNpcWithIntermediateRoutes() throws Exception {
		QuestDefinition def = load("4011").definition();
		int startNpc = 730139;
		int mid1 = 205132;
		int mid2 = 203522;
		int endNpc = 205132;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", mid1, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "step1", mid2, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "step2", endNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "step2", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest50008ReportsAndCompletesAtEndNpc() throws Exception {
		QuestDefinition def = load("50008").definition();
		int startNpc = 831038;
		int endNpc = 831036;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, "started", startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own report chain");
		assertEquals(1, talkRoutes(def, "started", endNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest11139ReportsAndCompletesAtEndNpc() throws Exception {
		QuestDefinition def = load("11139").definition();
		int startNpc = 799075;
		int midNpc = 798971;
		int endNpc = 798979;
		/* 11139 走真端链式登记：单步推进槽（Cicero 的 SETPRO1@10000）落在节点 s1，旧 XML 的 step1 标签已退役。 */
		/* 11139 is chain-registry driven: the single advance slot (Cicero's SETPRO1) lands on node s1, not step1. */
		String stage = "s1";

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", midNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, stage, startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own report chain");
		// S2 规范形：交付搬到 NPC_REPORT 块 source（s1）的 QUEST_SELECT(31)，1009 中转退场。
		// S2 canonical: the delivery moves onto the report block source's QUEST_SELECT(31); the 1009 relay is gone.
		assertCanonicalChainDelivery(def, stage, endNpc);
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest18737SupportsMultipleEndNpcChoices() throws Exception {
		QuestDefinition def = load("18737").definition();
		int startNpc = 804707;
		List<Integer> endNpcs = List.of(206378, 206379, 206380);
		// P0-2 DD 尾片：按真端规范形重锚。交付 owner 集逐 owner 各发一份 QUEST_SELECT 交付边——
		// 直翻领奖态 + 分档奖励窗（QE-028 查表）；成功页与 1009 中转随页链删除。
		// P0-2 DD tail slice: re-anchored to the retail canonical shape. The reward-owner set emits one
		// QUEST_SELECT delivery edge per owner — straight to REWARD plus the tiered reward window — and
		// the success page and the 1009 hop are gone.
		int rewardWindow = QuestDialogPage.rewardWindowForTier(def.metadata().rewardGroups().size() - 1)
			.orElse(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1).id();

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		for (int endNpc : endNpcs) {
			List<QuestTransition> delivery = talkRoutes(def, "started", endNpc, QuestDialogAction.QUEST_SELECT);
			assertEquals(1, delivery.size());
			assertEquals("reward", delivery.getFirst().targetNode());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(rewardWindow)), delivery.getFirst().afterCommit());
			assertTrue(talkRoutes(def, "started", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
				"canonical removed the 1009 hop");
		}
		assertEquals(endNpcs, completionNpcs(def));
	}

	@Test
	void quest28601ReportsAndCompletesAtLegacyEndNpc() throws Exception {
		QuestDefinition def = load("28601").definition();
		int startNpc = 204702;
		int endNpc = 205234;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, "started", startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own report chain");
		// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）。真端行 28601 是单步发物行
		// （give_item=ITEM_QUEST_28601A 1，Quest_SimpleTalk.xml:9719-9724）且零 item_check——交付 =
		// QUEST_SELECT(31) 空门直翻领奖态 + 档位奖励窗；报告页 SELECT5 与 1009 中转随页链退场。
		// P0-3 S1: the canonical delivery is the empty-gate QUEST_SELECT(31) into REWARD with the
		// tiered window; the SELECT5 report page and the 1009 hop are gone.
		List<QuestTransition> delivery = talkRoutes(def, "started", endNpc, QuestDialogAction.QUEST_SELECT);
		assertEquals(1, delivery.size(), "quest 28601 canonical delivery route count");
		assertEquals("reward", delivery.getFirst().targetNode(), "quest 28601 delivery target");
		assertTrue(delivery.getFirst().conditions().isEmpty(), "quest 28601 delivery gate");
		assertTrue(delivery.getFirst().actions().isEmpty(), "quest 28601 delivery actions");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(deliveryWindow(def))), delivery.getFirst().afterCommit(),
			"quest 28601 delivery window");
		assertTrue(talkRoutes(def, "started", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
			"canonical removed the full-node 1009 hand-in");
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	/**
	 * S2 链式规范形交付断言（NPC_REPORT 块 source → REWARD）：{@code QUEST_SELECT}(31) 空门直翻领奖态
	 * 并下发档位奖励窗，priority 为空；报告页 SELECT5 与 1009 中转随页链退场。本批任务在真端模板表
	 * 均无 {@code item_check}，故规范门为空。
	 * S2 canonical chain delivery: the empty-gated {@code QUEST_SELECT}(31) from the report block source
	 * flips REWARD and shows the tiered window with a null priority; the SELECT5 report page and the 1009
	 * relay retire. Every quest here lacks the retail {@code item_check} flag, so the gate is empty.
	 */
	private static void assertCanonicalChainDelivery(QuestDefinition definition, String source, int endNpc) {
		List<QuestTransition> delivery = talkRoutes(definition, source, endNpc, QuestDialogAction.QUEST_SELECT);
		assertEquals(1, delivery.size(), () -> definition.id() + " canonical delivery route count");
		QuestTransition handIn = delivery.getFirst();
		assertEquals("reward", handIn.targetNode(), () -> definition.id() + " delivery target");
		assertTrue(handIn.conditions().isEmpty(), () -> definition.id() + " delivery gate");
		assertTrue(handIn.actions().isEmpty(), () -> definition.id() + " delivery actions");
		assertNull(handIn.priority(), () -> definition.id() + " delivery priority");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(deliveryWindow(definition))), handIn.afterCommit(),
			() -> definition.id() + " delivery window");
		assertTrue(talkRoutes(definition, source, endNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
			() -> definition.id() + " canonical removed the 1009 hand-in");
	}

	/**
	 * 规范形交付窗页（P0-3 S1）：单步面 completeFlow 拒绝多奖励档，档位式只落在第 1 档窗
	 * （零奖励组同样兜底该窗），故与合成器 {@code deliveryWindowPage} 同值。
	 * The canonical delivery window: single-step rows never carry ≥2 tiers, so the tier lookup and
	 * the zero-group fallback both land on the first reward window.
	 */
	private static int deliveryWindow(QuestDefinition definition) {
		return QuestDialogPage.rewardWindowForTier(definition.metadata().rewardGroups().size() - 1)
			.orElse(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1).id();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& talk.dialogId() == action.id())
			.toList();
	}

	private static List<Integer> completionNpcs(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.distinct().toList();
	}

	private static CompiledQuestDefinition load(String qid) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(Integer.parseInt(qid));
	}
}
