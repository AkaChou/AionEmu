package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the statue-spawn dialog chains for Silentera Canyon quests 30056, 30156, and 30157.
 */
class SilenteraSpawnedDialogFamilyTest {

	private static final List<Case> CASES = List.of(
		new Case(30056, 798929, 203901, 700569, 799034, "dirvisia",
			List.of(new QuestItemRequirement(182209223, 1),
				new QuestItemRequirement(182209224, 1)),
			143620, 0, List.of(2, 3, 4, 5)),
		new Case(30156, 799234, 204304, 700570, 799339, "sinigalla-nep",
			List.of(new QuestItemRequirement(182209253, 1)),
			71810, 1, List.of(2, 3)),
		new Case(30157, 204304, 799234, 700570, 799339, "sinigalla-vili",
			List.of(new QuestItemRequirement(182209254, 1)),
			71810, 2, List.of(2, 3)));

	@Test
	void statueSpawnedNpcChainsMatchTheLegacyContracts() throws Exception {
		for (Case contract : CASES) {
			QuestDefinition definition = definition(contract.questId());
			assertEquals(contract.workItems(), definition.metadata().questWorkItems());
			assertEquals(contract.gold(), definition.metadata().rewards().getFirst().amount());
			assertEquals(contract.rewardMode(), definition.metadata().startConditions().getFirst().rewardMode());
			assertNode(definition, "s0", QuestStatus.START, 0);
			assertNode(definition, "s1", QuestStatus.START, 1);
			assertNode(definition, "reward", QuestStatus.REWARD, 1);

			assertTalk(definition, "unaccepted", "s0", contract.startNpc(),
				QuestDialogAction.QUEST_ACCEPT_1,
				List.of(new QuestCondition.StartEligible()),
				List.of(new QuestAction.GiveItem(contract.workItem(), 1)),
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())));
			assertTrue(definition.transitions().stream().anyMatch(candidate ->
				"s0".equals(candidate.sourceNode()) && "s0".equals(candidate.targetNode())
					&& candidate.event() instanceof QuestEvent.CanAct canAct
					&& canAct.templateId() == contract.objectNpc()
					&& "ACTION_ITEM_USE".equals(canAct.actionType())));

			QuestTransition spawn = talk(definition, "s0", contract.objectNpc(),
				QuestDialogAction.USE_OBJECT,
				List.of(new QuestCondition.HasItem(contract.workItem(), 1)));
			assertEquals("s1", spawn.targetNode());
			assertEquals(List.of(new QuestAction.RemoveItem(contract.workItem(), 1),
				new QuestAction.SetVariable("var0", 1)), spawn.actions());
			assertTrue(spawn.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.SpawnNpc npc
					&& npc.slot().equals(contract.slot())
					&& npc.templateId() == contract.spawnedNpc()));
			assertEquals(List.of(
				spawn.afterCommit().get(0),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), spawn.afterCommit());

			assertPage(definition, "s1", contract.spawnedNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
			assertPage(definition, "s1", contract.spawnedNpc(),
				QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
			assertPage(definition, "s1", contract.spawnedNpc(),
				QuestDialogAction.SELECT2_1_1, QuestDialogPage.SELECT2_1_1);
			QuestTransition stageComplete = talk(definition, "s1", contract.spawnedNpc(),
				QuestDialogAction.SETPRO1, List.of());
			assertEquals("reward", stageComplete.targetNode());
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)),
				stageComplete.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.DespawnNpc(contract.slot()),
				new AfterCommitAction.CloseDialog()), stageComplete.afterCommit());

			assertPage(definition, "reward", contract.rewardNpc(),
				QuestDialogAction.USE_OBJECT, QuestDialogPage.SELECT5);
			assertPage(definition, "reward", contract.rewardNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
			QuestTransition preview = talk(definition, "reward", contract.rewardNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD, List.of());
			assertEquals("reward", preview.targetNode());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
			assertCompletionChoices(definition, contract);
		}
	}

	private static void assertCompletionChoices(QuestDefinition definition, Case contract) {
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(candidate -> "complete".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc)
			.toList();
		assertEquals(contract.choices().size(), completions.size());
		assertTrue(completions.stream()
			.map(QuestTransition::event)
			.map(QuestEvent.TalkToNpc.class::cast)
			.allMatch(talk -> talk.npcId() == contract.rewardNpc()));
		assertTrue(completions.stream()
			.allMatch(candidate -> candidate.actions().stream()
				.anyMatch(QuestAction.CompleteQuest.class::isInstance)));
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, npcId, action, List.of());
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			transition.afterCommit());
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

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = SilenteraSpawnedDialogFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private record Case(int questId, int startNpc, int rewardNpc, int objectNpc,
			int spawnedNpc, String slot, List<QuestItemRequirement> workItems, long gold,
			int rewardMode, List<Integer> choices) {
		private int workItem() {
			return workItems.getFirst().itemId();
		}
	}
}
