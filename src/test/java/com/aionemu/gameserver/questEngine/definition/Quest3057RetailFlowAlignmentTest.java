package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 3057 的旧 handler 到点击杀、唯一 NPC owner 和客户端奖励页面合同。
 * Verifies quest 3057's legacy kill-on-arrival effect, unique NPC owner, and client reward-page contract.
 */
class Quest3057RetailFlowAlignmentTest {
	private static final int START_AND_REWARD_NPC = 798213;
	private static final int LURED_NPC = 214576;

	@Test
	void completesCursedZiriusOnlyAfterLuredNpcReachesTheLegacyCoordinate() {
		CompiledQuestDefinition compiled = load();
		QuestDefinition definition = compiled.definition();

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		QuestTransition start = route(definition, "unaccepted", START_AND_REWARD_NPC, QuestDialogAction.QUEST_SELECT);
		assertEquals("started", start.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT_NONE.id())),
			start.afterCommit());
		assertTrue(routes(definition, "unaccepted", LURED_NPC).isEmpty());
		assertTrue(routes(definition, "started", START_AND_REWARD_NPC).isEmpty());
		assertTrue(routes(definition, "started", LURED_NPC).isEmpty());
		assertTrue(routes(definition, "reward", LURED_NPC).isEmpty());

		QuestTransition attack = transition(definition, "started", new QuestEvent.AttackNpc(LURED_NPC));
		assertEquals("started", attack.targetNode());
		assertEquals(List.of(new AfterCommitAction.WatchLuredNpcCoordinate(
			1691.41f, 219.09f, 72.62f, 30, QuestLureCompletion.KILL)), attack.afterCommit());

		QuestTransition reached = transition(definition, "started", new QuestEvent.NpcReachTarget());
		assertEquals("reward", reached.targetNode());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), reached.afterCommit());
		assertFalse(definition.transitions().stream().anyMatch(candidate -> candidate.event() instanceof QuestEvent.EnterZone));

		QuestTransition report = route(definition, "reward", START_AND_REWARD_NPC, QuestDialogAction.USE_OBJECT);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			report.afterCommit());
		QuestTransition preview = route(definition, "reward", START_AND_REWARD_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		List<QuestTransition> completions = routes(definition, "reward", START_AND_REWARD_NPC).stream()
			.filter(candidate -> dialogId(candidate) >= QuestDialogAction.SELECTED_QUEST_REWARD1.id())
			.filter(candidate -> dialogId(candidate) <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id())
			.toList();
		assertEquals(13, completions.size());
		assertTrue(completions.stream().allMatch(candidate -> candidate.targetNode().equals("complete")
			&& candidate.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance)
			&& candidate.afterCommit().equals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())))));

		QuestSnapshot started = new QuestSnapshot(7, 3057, QuestStatus.START, 0, Map.of());
		assertEquals(QuestStatus.START, QuestMutationPlanner.plan(compiled, started, attack.event(), attack)
			.orElseThrow().nextStatus());
		assertEquals(QuestStatus.REWARD, QuestMutationPlanner.plan(compiled, started, reached.event(), reached)
			.orElseThrow().nextStatus());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, matches.size(), source + " " + event);
		return matches.getFirst();
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action) {
		List<QuestTransition> matches = routes(definition, source, npcId).stream()
			.filter(candidate -> dialogId(candidate) == action.id())
			.toList();
		assertEquals(1, matches.size(), source + " " + npcId + " " + action);
		return matches.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	private static int dialogId(QuestTransition transition) {
		return ((QuestEvent.TalkToNpc) transition.event()).dialogId();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/3057.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest3057RetailFlowAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
