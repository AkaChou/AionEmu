package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Full vertical proof for the current ReportTo owner of quest 1101. */
class ReportTo1101DefinitionTest {
	/** 生产视图（XML 目录 + 真端 overlay）；1101 已迁到真端驱动，目录本身不再持有它。 */
	private static final int QUEST_ID = 1101;

	@Test
	void productionDirectoryCompilesTheTaskDefinition() throws Exception {
		QuestCatalog catalog = productionCatalog();
		assertTrue(catalog.find(1101).isPresent());
		// 1102 已迁到真端驱动：不再由 XML 目录拥有，生产证据 = 真端合成定义（旧 XML 在 git 历史里）。
		// 1102 is retail-driven: no longer XML-owned; the evidence is the synthesized retail definition.
		assertTrue(catalog.find(1102).isPresent());
	}

	@Test
	void definitionCoversEveryHandleableReportToDialogPath() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestTransition> transitions = compiled.definition().transitions();

		// 客户端 1101 HTML 无 select1_1(1012) 页：select1 按钮直接是 ASK_QUEST_ACCEPT(1007)。
		// 真端合成器另加报告 NPC 的关窗出口（select6 失败页的 FINISH_DIALOG=1008），故为 30 条。
		// The client 1101 HTML has no select1_1 (1012) page; the retail synthesis adds the report
		// NPC's close-dialog exit (select6 failure page), hence 30 transitions.
		assertEquals(30, transitions.size());
		assertTrue(transitions.stream().allMatch(t -> t.event() instanceof QuestEvent.TalkToNpc talk
			&& talk.dialogId() != null));
		assertEquals(Set.of(31, 1007, 1002, 20000, 1003, 1004, 20001, 1008),
			dialogIds(transitions, "unaccepted", 203049));
		assertEquals(Set.of(1008), dialogIds(transitions, "started", 203049));
		assertEquals(Set.of(31, 1009, 1008), dialogIds(transitions, "started", 203057));
		Set<Integer> rewardDialogs = IntStream.rangeClosed(8, 23).boxed()
			.collect(Collectors.toCollection(java.util.LinkedHashSet::new));
		assertEquals(rewardDialogs,
			transitions.stream().filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete"))
				.map(t -> ((QuestEvent.TalkToNpc) t.event()).dialogId())
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new)));
		assertEquals(Set.of(-1, 1009), transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("reward"))
			.map(t -> ((QuestEvent.TalkToNpc) t.event()).dialogId()).collect(Collectors.toSet()));
	}

	@Test
	void retiredQuestIsNoLongerXmlOwned() {
		// 1101 已退役：生产 XML 不在仓库里（内容在 git 历史里），定义由真端驱动合成，
		// 且合成结果不含任何"证据/归属"元数据。
		// 1101 is retired: no production XML ships any more and the definition is synthesized.
		assertFalse(QuestXmlFixtures.productionXmlPresent(QUEST_ID),
			"retired quest must not keep a production XML");
		CompiledQuestDefinition compiled = definition();
		assertEquals(QUEST_ID, compiled.id());
		assertFalse(compiled.definition().nodes().isEmpty());
	}

	@Test
	void productionCatalogDoesNotRetainTheLegacy1101Owner() throws Exception {
		assertFalse(legacyScriptDataExists(), "quest_script_data directory must be fully removed");
		QuestCatalog catalog = productionCatalog();
		assertTrue(catalog.find(1101).isPresent());
		assertTrue(catalog.find(1102).isPresent());
	}

	@Test
	void acceptanceFailsClosedWithoutEligibilityAndUsesTheCorrectProtocol() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestTransition accept = transition(compiled, "unaccepted", 203049, 1002);
		QuestSnapshot unknown = new QuestSnapshot(7, 1101, QuestStatus.NONE, 0, Map.of());
		QuestSnapshot rejected = unknown.withStartEligibility(QuestStartEligibility.rejected("LEVEL"));
		QuestSnapshot allowed = unknown.withStartEligibility(QuestStartEligibility.allowed());

		assertTrue(QuestMutationPlanner.plan(compiled, unknown, accept).isEmpty());
		assertTrue(QuestMutationPlanner.plan(compiled, rejected, accept).isEmpty());
		assertTrue(QuestMutationPlanner.plan(compiled, allowed, accept).isPresent());
		assertTrue(accept.actions().isEmpty(), "target node already defines the accepted state");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(1003)), accept.afterCommit());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()),
			transition(compiled, "unaccepted", 203049, 20000).afterCommit());

		// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）。真端行 1101 只有
		// acquired=Elpis / reward=Mires 两列（Quest_SimpleTalk.xml:262-267），零 item_check——交付 =
		// QUEST_SELECT(31) 空门直翻领奖态并下发第 1 档奖励窗；报告页 SELECT5 与 1009 中转随页链退场。
		// P0-3 S1: the canonical delivery is the empty-gate QUEST_SELECT(31) into REWARD with the first
		// reward window; the SELECT5 report page and the 1009 hop are gone.
		QuestTransition reward = transition(compiled, "started", 203057,
			QuestDialogAction.QUEST_SELECT.id());
		QuestMutationPlan rewardPlan = QuestMutationPlanner.plan(compiled,
			new QuestSnapshot(7, 1101, QuestStatus.START, 0, Map.of()), reward).orElseThrow();
		assertTrue(reward.conditions().isEmpty(), "no item gate without item_check");
		assertTrue(reward.actions().isEmpty(), "target node already defines reward status and var0");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), reward.afterCommit());
		assertEquals(QuestStatus.REWARD, rewardPlan.nextStatus());
		// 领奖投影 = 客户端任务书末行行号：1101 只有 1 行 → 0（XML 时代的 var0=1 指向不存在的行）。
		// The reward projection is the client journal's last row index: one row -> 0.
		assertEquals(0, rewardPlan.nextPackedVariables());
	}

	@Test
	void everyCompletionPathUsesTypedRewardsAndCompleteLifecycle() throws Exception {
		CompiledQuestDefinition compiled = definition();
		// 发放顺序 = 真端 quest.xml 的奖励字段顺序（GOLD/EXP/ITEM）；XML 时代的
		// fixed-reward-indices="2 3 4 0 1"（道具先行）是旧 DSL 的排序痕迹，退役后不再保留。
		// Grant order follows the retail quest.xml field order.
		List<QuestAction> expected = List.of(
			new QuestAction.GrantReward("GOLD", 0, 120, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 130, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 164002010, 20),
			new QuestAction.GrantReward("ITEM", 164002011, 20),
			new QuestAction.GrantReward("ITEM", 164002057, 20),
			new QuestAction.CompleteQuest(0));
		List<AfterCommitAction> afterCommit = List.of(new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10));

		List<QuestTransition> completions = compiled.definition().transitions().stream()
			.filter(t -> t.targetNode().equals("complete")).toList();
		assertEquals(16, completions.size());
		for (QuestTransition completion : completions) {
			assertEquals(expected, completion.actions());
			assertEquals(afterCommit, completion.afterCommit());
		}
	}

	@Test
	void noFivePathFixtureCanBeMistakenForReplacementProof() throws Exception {
		CompiledQuestDefinition compiled = definition();
		assertNotEquals(5, compiled.definition().transitions().size());
		assertEquals(3, compiled.definition().metadata().rewards().stream()
			.filter(reward -> reward.kind().equals("ITEM")).count());
	}

	@Test
	void mutationPlannerBuildsAPlanForEveryProductionPath() throws Exception {
		CompiledQuestDefinition compiled = definition();
		for (QuestTransition transition : compiled.definition().transitions()) {
			QuestEvent.TalkToNpc route = (QuestEvent.TalkToNpc) transition.event();
			QuestSnapshot snapshot = switch (transition.sourceNode()) {
                case "unaccepted" -> new QuestSnapshot(7, 1101, QuestStatus.NONE, 0, Map.of());
                case "started" -> new QuestSnapshot(7, 1101, QuestStatus.START, 0, Map.of());
                case "reward" -> new QuestSnapshot(7, 1101, QuestStatus.REWARD, 0, Map.of());
                default -> throw new AssertionError("unexpected source " + transition.sourceNode());
            };
            snapshot = snapshot.withStartEligibility(QuestStartEligibility.allowed());
			QuestEvent event = new QuestEvent.TalkToNpc(route.npcId(), route.dialogId(), 900007);

			assertTrue(QuestMutationPlanner.plan(compiled, snapshot, event, transition).isPresent(),
				"no plan for " + transition.sourceNode() + ":" + route.dialogId());
		}
	}

	private static Set<Integer> dialogIds(List<QuestTransition> transitions, String source, int npcId) {
		return transitions.stream().filter(t -> t.sourceNode().equals(source))
			.map(t -> (QuestEvent.TalkToNpc) t.event()).filter(t -> t.npcId() == npcId)
			.map(QuestEvent.TalkToNpc::dialogId).collect(Collectors.toSet());
	}

	private static QuestTransition transition(CompiledQuestDefinition compiled, String source, int npcId,
			int dialogId) {
		return compiled.definition().transitions().stream().filter(t -> t.sourceNode().equals(source))
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == dialogId)
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition definition() {
		return ProductionQuestDefinitions.definition(QUEST_ID);
	}

	/** 生产视图 = XML 目录 + 真端 overlay（退役任务由真端定义提供）。 / Production view with the retail overlay. */
	private static QuestCatalog productionCatalog() {
		return ProductionQuestDefinitions.catalog();
	}
	private static boolean legacyScriptDataExists() {
		return java.nio.file.Files.exists(
			java.nio.file.Path.of("src/main/resources/aion/data/static_data/quest_script_data"));
	}

}
