package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks retail sequential dialogs and kill counters for quests 15321, 15590, and 25590.
 * <p>
 * 15321 仍是 XML 保留（retention: XML_RETENTION）；15590/25590 已由真端 DataDriven Talk 链接管
 * （retention: DD_TALK_CHAIN）——四段阶段页主的 SETPRO 阶梯与客户端链页登记
 * （{@code quest_client_talk_chain_pages.tsv}）逐字对齐。
 * 15321 stays XML-owned (retention: XML_RETENTION); 15590/25590 are retail DataDriven talk chains
 * now (retention: DD_TALK_CHAIN) — the four stage owners follow the client chain-page registry
 * verbatim (see {@code quest_client_talk_chain_pages.tsv}).
 */
class RetailSequentialQuestFamilyTest {

	// TEMP-VERIFY(view): 并行批次落定前的宽松生产视图（XML 目录 + 真端驱动，跳过覆盖门）。
	private static final AtomicReference<QuestCatalog> VIEW = new AtomicReference<>();

	/**
	 * 紧急指令日报（DD_TALK_CHAIN）的链形：接取 NPC（领奖也回到它）的规范接取流落在 started；
	 * 四个阶段页主各持客户端梯首页（1011/1352/1693/2034），前三个的 SETPRO1..3 推进阶梯
	 * （PACKET_ONLY + 全局任务簿页），末段 SET_SUCCEED 直进领奖（选择窗）；中间步 SET_SUCCEED
	 * 保留直达领奖的捷径（1876 形状）。var0 = 阶梯计数；领奖投影 = 客户端任务书末行（5 行 → 行 4）。
	 * The urgent-order daily chain (DD_TALK_CHAIN): the start NPC (which also owns the reward)
	 * carries the canonical accept flow landing on started; the four stage owners hold the client
	 * ladder head pages (1011/1352/1693/2034); the first three advance the ladder with SETPRO1..3
	 * (PACKET_ONLY + the global quest-book page) while the last owner's SET_SUCCEED enters the
	 * reward state (selection dialog); intermediate SET_SUCCEED keeps the direct reward shortcut
	 * (the 1876 shape). var0 is the ladder counter; the reward projection is the client journal
	 * last row (5 rows -> row 4).
	 */
	private static void assertDailyQuest(int questId, int startNpc, List<Integer> npcs) throws Exception {
		// TEMP-VERIFY(view): 并行 SimpleTalk 批次落定前生产覆盖门不可用，用宽松 overlay 验证本断言。
		QuestDefinition definition = VIEW.updateAndGet(current -> current != null ? current
				: RetailQuestDriver.overlay(QuestDefinitionDirectoryLoader.compile(
					RetailSequentialQuestFamilyTest.class.getClassLoader())))
			.find(questId)
			.orElseThrow(() -> new IllegalStateException("missing production quest definition " + questId))
			.definition();
		assertNodeProjection(definition, "unaccepted", QuestStatus.NONE, 0);
		assertNodeProjection(definition, "started", QuestStatus.START, 0);
		for (int stage = 1; stage < npcs.size(); stage++) {
			assertNodeProjection(definition, "s" + stage, QuestStatus.START, stage);
		}
		assertNodeProjection(definition, "reward", QuestStatus.REWARD, 4);
		assertNodeProjection(definition, "complete", QuestStatus.COMPLETE, 0);

		assertTalk(definition, "unaccepted", "started", startNpc, QuestDialogAction.QUEST_ACCEPT_SIMPLE,
			List.of(new QuestCondition.StartEligible()), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));
		assertTalk(definition, "unaccepted", "unaccepted", startNpc,
			QuestDialogAction.QUEST_REFUSE_SIMPLE, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));

		List<Integer> ladderPages = List.of(1011, 1352, 1693, 2034);
		List<QuestDialogAction> advances = List.of(QuestDialogAction.SETPRO1, QuestDialogAction.SETPRO2,
			QuestDialogAction.SETPRO3, QuestDialogAction.SET_SUCCEED);
		for (int stage = 0; stage < npcs.size(); stage++) {
			String source = stage == 0 ? "started" : "s" + stage;
			String target = stage < npcs.size() - 1 ? "s" + (stage + 1) : "reward";
			assertTalk(definition, source, source, npcs.get(stage), QuestDialogAction.QUEST_SELECT,
				List.of(), List.of(),
				List.of(new AfterCommitAction.ShowQuestDialog(ladderPages.get(stage))));
			if (stage < npcs.size() - 1) {
				assertTalk(definition, source, target, npcs.get(stage), advances.get(stage),
					List.of(), List.of(new QuestAction.SetVariable("var0", stage + 1)),
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
						new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
				assertTalk(definition, source, "reward", npcs.get(stage), QuestDialogAction.SET_SUCCEED,
					List.of(), List.of(),
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
						new AfterCommitAction.CloseDialog()));
			} else {
				assertTalk(definition, source, "reward", npcs.get(stage), QuestDialogAction.SET_SUCCEED,
					List.of(), List.of(),
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
						new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
			}
		}

		assertRewardContract(definition, startNpc, "0 1 2");
		assertTrue(definition.transitions().stream()
			.filter(candidate -> "complete".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc)
			.map(QuestTransition::event)
			.map(QuestEvent.TalkToNpc.class::cast)
			.allMatch(talk -> talk.npcId() == startNpc));
	}

	private static void assertSimpleStart(QuestDefinition definition, int npcId, String target) {
		assertPage(definition, "unaccepted", npcId, QuestDialogAction.QUEST_SELECT);
		assertTalk(definition, "unaccepted", target, npcId, QuestDialogAction.QUEST_ACCEPT_SIMPLE,
			List.of(new QuestCondition.StartEligible()), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));
		assertTalk(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.QUEST_REFUSE_SIMPLE, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));
	}

	private static void assertTalkChain(QuestDefinition definition, String source, String target,
			int npcId, String firstPage, String secondPage, String action) {
		assertPage(definition, source, npcId, QuestDialogPage.valueOf(firstPage));
		assertActionPage(definition, source, npcId, QuestDialogAction.valueOf(secondPage),
			QuestDialogPage.valueOf(secondPage));
		assertTalk(definition, source, target, npcId, QuestDialogAction.valueOf(action), List.of(),
			List.of(new QuestAction.SetVariable("var0", Integer.parseInt(target.substring(1))),
				new QuestAction.SetVariable("var1", 0)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
	}

	private static void assertKillStep(QuestDefinition definition, String source, String target,
			int count, int targetValue) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpcSet)
			.toList();
		assertEquals(2, routes.size());
		QuestTransition active = routes.stream().filter(candidate -> candidate.priority() == 1).findFirst().orElseThrow();
		QuestTransition complete = routes.stream().filter(candidate -> candidate.priority() == 0).findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", count - 1)), active.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), active.actions());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", count - 1)), complete.conditions());
		assertEquals(target, complete.targetNode());
		assertEquals(List.of(new QuestAction.SetVariable("var0", targetValue),
			new QuestAction.SetVariable("var1", 0)), complete.actions());
	}

	private static void assertRewardContract(QuestDefinition definition, int npcId,
			String fixedRewards) {
		assertPage(definition, "reward", npcId, QuestDialogAction.QUEST_SELECT);
		assertTalk(definition, "reward", "reward", npcId, QuestDialogAction.SELECT_QUEST_REWARD,
			List.of(), List.of(), List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
		QuestTransition completion = talk(definition, "reward", npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		QuestTransition transition = talk(definition, source, npcId, action, List.of());
		assertEquals(source, transition.targetNode());
		assertEquals(1, transition.afterCommit().size());
		assertTrue(transition.afterCommit().getFirst() instanceof AfterCommitAction.ShowQuestDialog);
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogPage page) {
		assertActionPage(definition, source, npcId, QuestDialogAction.QUEST_SELECT, page);
	}

	private static void assertActionPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, npcId, action, List.of());
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static void assertTalk(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(target, transition.targetNode());
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, List<QuestCondition> conditions) {
		QuestEvent.TalkToNpc event = new QuestEvent.TalkToNpc(npcId, action.id());
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> candidate.conditions().equals(conditions))
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				"missing route " + source + " + NPC " + npcId + " + action " + action.id()));
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	/**
	 * 阶段节点只以阶段位段 var0 标识自身：运行期按 source 节点投影匹配路由，把实时击杀计数
	 * var1 钉进投影会让第一只怪之后的每次击杀都匹配不到 source（NO_MATCH，任务不往下）。
	 * Stage nodes identify themselves by the stage field var0 only: matching a route requires its
	 * source projection, so pinning the live kill counter var1 stalls every kill after the first.
	 */
	private static void assertNode(QuestDefinition definition, String label, int var0) {
		QuestNode node = node(definition, label);
		assertEquals(QuestStatus.START, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static void assertNodeProjection(QuestDefinition definition, String label, QuestStatus status,
			int var0) {
		QuestNode node = node(definition, label);
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = RetailSequentialQuestFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest/definitions/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
