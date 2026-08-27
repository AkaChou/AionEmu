package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 18501 两个哈拉梅尔交互物的类型化使用合同。
 * Locks the typed use contract for quest 18501's two Haramel interaction objects.
 */
class Quest18501InteractionObjectTest {
	@Test
	void bothOdiumObjectsExposeTheActionItemGateAndDropBackedTalkRoute() {
		CompiledQuestDefinition definition = load();
		assertEquals(QuestStatus.START, node(definition, "started").projection().status());
		assertEquals(Map.of("var0", 0), node(definition, "started").projection().variables());

		for (int[] object : new int[][]{{700833, 182212001}, {700951, 182212002}}) {
			int objectId = object[0];
			int itemId = object[1];
			QuestTransition gate = route(definition, new QuestEvent.CanAct(objectId, "ACTION_ITEM_USE"));
			assertEquals("started", gate.sourceNode());
			assertEquals("started", gate.targetNode());
			assertEquals(List.of(), gate.actions());
			assertEquals(List.of(), gate.afterCommit());

			QuestTransition use = route(definition, new QuestEvent.TalkToNpc(objectId, -1));
			assertEquals("started", use.sourceNode());
			assertEquals("started", use.targetNode());
			assertEquals(List.of(), use.actions());
			assertEquals(List.of(), use.afterCommit());

			assertTrue(definition.definition().metadata().drops().stream().anyMatch(drop ->
				drop.npcId() == objectId && drop.itemId() == itemId
					&& drop.chance() == 100 && drop.eachMember() && drop.collectingStep() == 0));
			assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
				"unaccepted".equals(transition.sourceNode())
					&& ((transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == objectId)
						|| (transition.event() instanceof QuestEvent.CanAct canAct
							&& canAct.templateId() == objectId))));
		}
	}

	@Test
	void finishDialogOnTheAcceptConfirmPageReturnsToTheSelectionList() {
		CompiledQuestDefinition definition = load();
		QuestTransition finish = definition.definition().transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode())
				&& QuestEvent.matches(transition.event(),
					new QuestEvent.TalkToNpc(799522, QuestDialogAction.FINISH_DIALOG.id())))
			.findFirst().orElseThrow();
		assertEquals("started", finish.targetNode());
		assertEquals(List.of(), finish.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			finish.afterCommit());
	}

	private static QuestTransition route(CompiledQuestDefinition definition, QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode())
				&& "started".equals(transition.targetNode())
				&& QuestEvent.matches(transition.event(), event))
			.findFirst().orElseThrow();
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/18501.xml";
		try (InputStream input = Quest18501InteractionObjectTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new AssertionError("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("failed to load " + resource, e);
		}
	}
}
