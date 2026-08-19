package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the multi-page client chain and state-gated NPC ownership for quest 2284. */
class Quest2284ClientDialogAlignmentTest {
	@Test
	void keepsTheEscortConversationInItsClientPageOrder() {
		QuestDefinition definition = load().definition();

		assertPage(definition, "started", 798040, QuestDialogAction.SELECT2_1,
			QuestDialogPage.SELECT2_1);
		assertPage(definition, "started", 798040, QuestDialogAction.SELECT2_1_1,
			QuestDialogPage.SELECT2_1_1);
		assertPage(definition, "started", 798040, QuestDialogAction.SELECT2_1_1_1,
			QuestDialogPage.SELECT2_1_1_1);

		assertPage(definition, "step1", 798041, QuestDialogAction.SELECT3_1,
			QuestDialogPage.SELECT3_1);
		assertTrue(routes(definition, "started", 798041).isEmpty());
		assertTrue(routes(definition, "step2", 798040).isEmpty());
		assertTrue(routes(definition, "step2", 798041).isEmpty());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, QuestDialogPage page) {
		List<QuestTransition> matches = routes(definition, source, npcId).stream()
			.filter(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).dialogId() == action.id())
			.toList();
		assertEquals(1, matches.size(), source + " " + npcId + " " + action);
		QuestTransition route = matches.getFirst();
		assertEquals(source, route.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), route.afterCommit());
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/2284.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest2284ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
