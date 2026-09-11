package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks the legacy FountainRewards contract across all six production fountain quests.
 */
class FountainRewardQuestFamilyTest {

	private static final int COIN = 186000469;
	private static final List<Case> CASES = List.of(
		new Case(15205, "ELYOS", 55, 2, List.of(701429, 804788)),
		new Case(25205, "ASMODIANS", 55, 2, List.of(701430, 804759)),
		new Case(15667, "ELYOS", 66, 3, List.of(805778)),
		new Case(25667, "ASMODIANS", 66, 3, List.of(805753)),
		new Case(1717, "ELYOS", 25, 1, List.of(806559)),
		new Case(2717, "ASMODIANS", 25, 1, List.of(806560)));

	@Test
	void fountainQuestsUseTheTemplateCoinAndRewardContract() throws Exception {
		for (Case contract : CASES) {
			QuestDefinition definition = definition(contract.questId());
			assertEquals(contract.minLevel(), definition.metadata().minLevel());
			assertEquals(List.of(contract.race()), definition.metadata().permittedRaces().stream().toList());
			assertEquals(List.of(new QuestItemRequirement(COIN, 100)),
				definition.metadata().itemRequirements());
			assertEquals(List.of(new QuestItemRequirement(COIN, 1)),
				definition.metadata().inventoryItems());
			assertEquals(List.of(), definition.metadata().rewards());
			assertEquals(List.of(new QuestBonus("MEDAL", contract.bonusLevel(), null)),
				definition.metadata().bonuses());
			assertEquals(List.of(
				new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of("var0", 0))),
				new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, Map.of("var0", 0))),
				new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, Map.of("var0", 0)))),
				definition.nodes());

			for (int npcId : contract.startNpcs()) {
				for (String source : List.of("unaccepted", "complete")) {
					assertPage(definition, source, npcId, QuestDialogAction.USE_OBJECT,
						QuestDialogPage.SELECT1);
					assertPage(definition, source, npcId, QuestDialogAction.QUEST_SELECT,
						QuestDialogPage.SELECT1);
					assertTalk(definition, source, "reward", npcId,
						QuestDialogAction.SETPRO1,
						List.of(new QuestCondition.StartEligible(),
							new QuestCondition.HasItem(COIN, 100)),
						List.of(), List.of(
							new AfterCommitAction.SyncQuestState(
								QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
							new AfterCommitAction.ShowQuestDialog(
								QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
				}
				assertTalk(definition, "reward", "complete", npcId,
					QuestDialogAction.SELECTED_QUEST_NOREWARD,
					List.of(new QuestCondition.HasItem(COIN, 100)),
					List.of(new QuestAction.RemoveItem(COIN, 100),
						new QuestAction.CompleteQuest(0)),
					List.of(new AfterCommitAction.RefreshPlayerStats(),
						new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
						new AfterCommitAction.ShowQuestSelectionDialog(
							QuestDialogPage.SELECT_QUEST.id())));
			}
			assertEquals(contract.startNpcs().size(),
				definition.transitions().stream()
					.filter(candidate -> "reward".equals(candidate.sourceNode()))
					.filter(candidate -> "complete".equals(candidate.targetNode()))
					.count());
		}
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertTalk(definition, source, source, npcId, action,
			List.of(new QuestCondition.HasItem(COIN, 1)), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(page.id())));
	}

	private static void assertTalk(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		QuestEvent.TalkToNpc event = new QuestEvent.TalkToNpc(npcId, action.id());
		QuestTransition transition = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> candidate.conditions().equals(conditions))
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				"missing route " + source + " + NPC " + npcId + " + action " + action.id()));
		assertEquals(target, transition.targetNode());
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = FountainRewardQuestFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private record Case(int questId, String race, int minLevel, int bonusLevel,
			List<Integer> startNpcs) {
	}
}
