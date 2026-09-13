package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, "k1", startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own the report chain");
		assertEquals(1, talkRoutes(def, "k1", endNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "k1", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
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
		assertEquals(1, talkRoutes(def, "a4b6", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest13702ReportsAndCompletesAtLegacyEndNpc() throws Exception {
		QuestDefinition def = load("13702").definition();
		int startNpc = 802350;
		int endNpc = 802352;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, "unaccepted", endNpc, QuestDialogAction.QUEST_SELECT).isEmpty(), "end NPC must not have start route");
		assertTrue(talkRoutes(def, "k4", startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own report chain");
		assertEquals(1, talkRoutes(def, "k4", endNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "k4", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest2414ReportsAndCompletesAtEndNpcWithIntermediateRoute() throws Exception {
		QuestDefinition def = load("2414").definition();
		int startNpc = 204369;
		int midNpc = 204361;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", midNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started1", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started1", startNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
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
		assertEquals(1, talkRoutes(def, "step2", endNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "step2", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
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
		assertEquals(1, talkRoutes(def, "step2", endNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "step2", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest2654ReportsAndCompletesAtEndNpc() throws Exception {
		QuestDefinition def = load("2654").definition();
		int startNpc = 204775;
		int endNpc = 204655;

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, "started", startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own report chain");
		assertEquals(1, talkRoutes(def, "started", endNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
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
		assertEquals(1, talkRoutes(def, "step1", endNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "step1", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
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

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", midNpc, QuestDialogAction.QUEST_SELECT).size());
		assertTrue(talkRoutes(def, "step1", startNpc, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(), "start NPC must not own report chain");
		assertEquals(1, talkRoutes(def, "step1", endNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "step1", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
		assertEquals(List.of(endNpc), completionNpcs(def));
	}

	@Test
	void quest18737SupportsMultipleEndNpcChoices() throws Exception {
		QuestDefinition def = load("18737").definition();
		int startNpc = 804707;
		List<Integer> endNpcs = List.of(206378, 206379, 206380);

		assertEquals(1, talkRoutes(def, "unaccepted", startNpc, QuestDialogAction.QUEST_SELECT).size());
		for (int endNpc : endNpcs) {
			assertEquals(1, talkRoutes(def, "started", endNpc, QuestDialogAction.QUEST_SELECT).size());
			assertEquals(1, talkRoutes(def, "started", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
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
		assertEquals(1, talkRoutes(def, "started", endNpc, QuestDialogAction.QUEST_SELECT).size());
		assertEquals(1, talkRoutes(def, "started", endNpc, QuestDialogAction.SELECT_QUEST_REWARD).size());
		assertEquals(List.of(endNpc), completionNpcs(def));
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
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/" + qid + ".xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
