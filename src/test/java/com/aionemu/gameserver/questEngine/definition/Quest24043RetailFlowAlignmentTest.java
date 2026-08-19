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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the legacy arrival gate and the per-NPC client dialog chain for quest 24043.
 */
class Quest24043RetailFlowAlignmentTest {
	@Test
	void waitsForTheLuredMonsterAndKeepsEachDialogOwner() {
		CompiledQuestDefinition compiled = load();
		QuestDefinition definition = compiled.definition();

		QuestTransition attack = transition(definition, "s2", new QuestEvent.AttackNpc(253610));
		assertEquals("s2", attack.targetNode());
		assertTrue(attack.actions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.WatchLuredNpcCoordinate(
			675.524475f, 1541.412354f, 1610.466553f, 30, QuestLureCompletion.KILL)), attack.afterCommit());

		QuestSnapshot s2 = new QuestSnapshot(7, 24043, QuestStatus.START, 2, Map.of());
		assertEquals(QuestStatus.START,
			QuestMutationPlanner.plan(compiled, s2, attack.event(), attack).orElseThrow().nextStatus());

		QuestTransition reached = transition(definition, "s2", new QuestEvent.NpcReachTarget());
		assertEquals("s3", reached.targetNode());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), reached.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 3)), reached.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			reached.afterCommit());

		assertPage(definition, "s1", 278086, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertPage(definition, "s3", 278039, QuestDialogAction.SELECT4_1, QuestDialogPage.SELECT4_1);
		assertPage(definition, "s4", 279027, QuestDialogAction.SELECT5_1, QuestDialogPage.SELECT5_1);
		assertPage(definition, "s5", 204210, QuestDialogAction.SELECT6_1, QuestDialogPage.SELECT6_1);

		QuestTransition movie = route(definition, "s6", 279027, QuestDialogAction.SELECT7_1);
		assertEquals("s6", movie.targetNode());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 6)), movie.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(182215373, 1)), movie.actions());
		assertEquals(List.of(new AfterCommitAction.PlayMovie(293),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT7_1.id())), movie.afterCommit());

		assertTrue(routes(definition, "started", 278003).stream()
			.allMatch(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.SELECT1_1.id()));
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, QuestDialogPage page) {
		QuestTransition route = route(definition, source, npcId, action);
		assertEquals(source, route.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), route.afterCommit());
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
			.filter(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).dialogId() == action.id())
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

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/24043.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest24043RetailFlowAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
