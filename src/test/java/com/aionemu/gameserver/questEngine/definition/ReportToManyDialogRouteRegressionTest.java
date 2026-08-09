package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportToManyDialogRouteRegressionTest {
	private static final List<ExpectedRoute> EXPECTED_ROUTES = List.of(
		new ExpectedRoute(1971, 203820, 1352),
		new ExpectedRoute(1971, 203812, 2375),
		new ExpectedRoute(3201, 804601, 1352),
		new ExpectedRoute(3201, 204534, 2375),
		new ExpectedRoute(3914, 203752, 1352),
		new ExpectedRoute(3914, 203384, 2375),
		new ExpectedRoute(4201, 205233, 1352),
		new ExpectedRoute(4201, 204791, 2375),
		new ExpectedRoute(4914, 204182, 1352),
		new ExpectedRoute(4914, 203385, 2375),
		new ExpectedRoute(18210, 205985, 2375),
		new ExpectedRoute(24120, 730038, 1352),
		new ExpectedRoute(24120, 802441, 2375),
		new ExpectedRoute(28210, 205986, 2375),
		new ExpectedRoute(35025, 799800, 2375),
		new ExpectedRoute(35025, 799801, 2375),
		new ExpectedRoute(39000, 800501, 1352),
		new ExpectedRoute(39000, 800500, 2375),
		new ExpectedRoute(49000, 800503, 1352),
		new ExpectedRoute(80268, 831172, 2375),
		new ExpectedRoute(80269, 831174, 2375),
		new ExpectedRoute(80270, 831178, 2375),
		new ExpectedRoute(80271, 831180, 2375),
		new ExpectedRoute(80310, 831384, 1003),
		new ExpectedRoute(80311, 831387, 1003),
		new ExpectedRoute(80324, 831447, 2375),
		new ExpectedRoute(80325, 831447, 2375),
		new ExpectedRoute(80326, 831448, 2375),
		new ExpectedRoute(80327, 831448, 2375),
		new ExpectedRoute(80344, 831787, 2375),
		new ExpectedRoute(80351, 831797, 2375),
		new ExpectedRoute(80353, 831818, 2375),
		new ExpectedRoute(80354, 831818, 2375),
		new ExpectedRoute(80355, 831818, 2375),
		new ExpectedRoute(80360, 831806, 2375),
		new ExpectedRoute(80362, 831830, 2375),
		new ExpectedRoute(80363, 831830, 2375),
		new ExpectedRoute(80364, 831830, 2375),
		new ExpectedRoute(80575, 832041, 2375),
		new ExpectedRoute(80577, 832040, 2375),
		new ExpectedRoute(80578, 832041, 2375),
		new ExpectedRoute(80580, 832040, 2375),
		new ExpectedRoute(80623, 832870, 2375),
		new ExpectedRoute(80624, 832871, 2375),
		new ExpectedRoute(80683, 832965, 2375),
		new ExpectedRoute(80686, 832976, 2375),
		new ExpectedRoute(80708, 833503, 10002),
		new ExpectedRoute(80721, 833543, 10002),
		new ExpectedRoute(80722, 833545, 10002),
		new ExpectedRoute(80787, 833671, 10002),
		new ExpectedRoute(80788, 833672, 10002),
		new ExpectedRoute(80789, 833671, 10002),
		new ExpectedRoute(80790, 833672, 10002),
		new ExpectedRoute(80791, 833673, 10002),
		new ExpectedRoute(80792, 833674, 10002),
		new ExpectedRoute(80793, 833673, 10002),
		new ExpectedRoute(80794, 833674, 10002),
		new ExpectedRoute(80868, 833825, 10002),
		new ExpectedRoute(80876, 834463, 10002),
		new ExpectedRoute(80952, 835553, 10002));

	@Test
	void migratedReportNpcsOpenTheirPageFromStartDialog() throws Exception {
		Map<Integer, QuestDefinition> definitions = new HashMap<>();
		for (ExpectedRoute expected : EXPECTED_ROUTES) {
			QuestDefinition definition = definitions.computeIfAbsent(expected.questId(), questId -> load(questId));
			Set<String> startedNodes = definition.nodes().stream()
				.filter(node -> node.projection().status() == QuestStatus.START)
				.map(QuestNode::label)
				.collect(Collectors.toSet());

			assertTrue(definition.transitions().stream().anyMatch(transition ->
				startedNodes.contains(transition.sourceNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == expected.npcId()
					&& Integer.valueOf(31).equals(talk.dialogId())
					&& transition.afterCommit().contains(
						new AfterCommitAction.ShowQuestDialog(expected.pageId()))),
				() -> "missing START_DIALOG route for quest " + expected.questId()
					+ ", npc " + expected.npcId() + ", page " + expected.pageId());
		}
	}

	private static QuestDefinition load(int questId) {
		try (InputStream input = ReportToManyDialogRouteRegressionTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new AssertionError("missing quest " + questId + " resource");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		} catch (Exception e) {
			throw new AssertionError("failed to load quest " + questId, e);
		}
	}

	private record ExpectedRoute(int questId, int npcId, int pageId) {
	}
}
