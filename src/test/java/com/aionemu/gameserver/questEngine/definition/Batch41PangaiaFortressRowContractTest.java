package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 41：潘盖亚要塞战三行族（14220 天族 / 24220 魔族）。
 * <p>
 * 两家客户端任务书都是三行、槽位 %0/%3/%6：行 0 和行 0 NPC 对话（`select1` 的 SETPRO1）、
 * 行 1 和“潘盖亚情报员”对话（`select2 -&gt; select2_1` 的 SETPRO2）、行 2 回接取 NPC
 * 报告（`select_success` 的 SELECT_QUEST_REWARD）。情报员 `STR_DIC_N_GAb1_Ag_all` 按入口位置
 * 有 4 个中立变体（802544/802545/802546/802547），两个任务共用。旧定义把行 1/行 2 挂在接取
 * NPC 上直跳，行 0 的 Astarin/Krondel 与 4 个变体完全没有路由。
 * </p>
 * <p>
 * Locks batch 41: the Pangaia fortress journal keeps one state per row, every informant variant
 * advances row 1, and only the accept NPC owns the row-2 report and reward window.
 * </p>
 */
class Batch41PangaiaFortressRowContractTest {

	private record PangaiaQuest(int questId, int acceptNpc, int row0Npc) {
	}

	private static final List<PangaiaQuest> FAMILY = List.of(
		new PangaiaQuest(14220, 802540, 802541),
		new PangaiaQuest(24220, 802542, 802543)
	);

	/** 客户端 strings 的 STR_DIC_N_GAb1_Ag_all 列出的 4 个入口变体。 */
	private static final List<Integer> INFORMANTS = List.of(802544, 802545, 802546, 802547);

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		for (PangaiaQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertNode(definition, contract.questId(), "started", QuestStatus.START, 0);
			assertNode(definition, contract.questId(), "s1", QuestStatus.START, 1);
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
	void acceptPageAndRowZeroOwnerAdvanceTheLadder() throws Exception {
		for (PangaiaQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertPageRoute(definition, contract.acceptNpc(), "unaccepted",
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
			assertPageRoute(definition, contract.row0Npc(), "started",
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
			List<QuestTransition> advance = dialogRoutes(definition, "started", contract.row0Npc(),
				QuestDialogAction.SETPRO1);
			assertEquals(1, advance.size(),
				() -> "quest " + contract.questId() + " row-0 advance must be unique");
			assertEquals("s1", advance.getFirst().targetNode(),
				() -> "quest " + contract.questId() + " row 0 must advance to row 1");
			assertTrue(advance.getFirst().afterCommit().contains(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				() -> "quest " + contract.questId() + " row-0 advance must refresh visibility");
		}
	}

	@Test
	void everyInformantVariantAdvancesRowOne() throws Exception {
		for (PangaiaQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			for (int informant : INFORMANTS) {
				assertPageRoute(definition, informant, "s1",
					QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
				assertPageRoute(definition, informant, "s1",
					QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
				List<QuestTransition> advance = dialogRoutes(definition, "s1", informant,
					QuestDialogAction.SETPRO2);
				assertEquals(1, advance.size(),
					() -> "quest " + contract.questId() + " informant " + informant
						+ " must advance row 1");
				assertEquals("reward", advance.getFirst().targetNode(),
					() -> "quest " + contract.questId() + " informant " + informant
						+ " must reach the report row");
			}
		}
	}

	@Test
	void rowTwoReportStaysOnTheAcceptNpcWithWindowOne() throws Exception {
		for (PangaiaQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> open = dialogRoutes(definition, "reward", contract.acceptNpc(),
				QuestDialogAction.QUEST_SELECT);
			assertEquals(1, open.size(),
				() -> "quest " + contract.questId() + " row-2 report page must be unique");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.DEFAULT_SUCCESS.id())), open.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " row 2 must open the client select_success page");
			List<QuestTransition> reward = dialogRoutes(definition, "reward", contract.acceptNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD);
			assertEquals(1, reward.size(),
				() -> "quest " + contract.questId() + " reward route must be unique");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), reward.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " must open reward window 1");

			List<QuestTransition> completions = definition.transitions().stream()
				.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
				.toList();
			assertTrue(completions.stream().allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == contract.acceptNpc()),
				() -> "quest " + contract.questId() + " completion must stay on the accept NPC");
			assertTrue(completions.stream().flatMap(route -> route.actions().stream())
					.anyMatch(new QuestAction.CompleteQuest(0)::equals),
				() -> "quest " + contract.questId() + " must complete at reward index 0");
		}
	}

	@Test
	void staleRewardSaveHealsToRowTwo() throws Exception {
		for (PangaiaQuest contract : FAMILY) {
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

	private static void assertPageRoute(QuestDefinition definition, int npcId, String source,
		QuestDialogAction action, QuestDialogPage page) {
		List<QuestTransition> routes = dialogRoutes(definition, source, npcId, action);
		assertEquals(1, routes.size(),
			() -> "route " + source + " + npc " + npcId + " + " + action + " must be unique");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			routes.getFirst().afterCommit(),
			() -> "route " + source + " + npc " + npcId + " + " + action + " page");
	}

	private static List<QuestTransition> dialogRoutes(QuestDefinition definition, String source,
		int npcId, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == action.id())
			.toList();
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch41PangaiaFortressRowContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
