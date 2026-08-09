package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runtime parity for the former Java owners 80020 and 80021. */
class QuestSoloriusEventDefinitionTest {
	private static final Path DIR = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");

	@Test
	void metadataMatchesLegacyAndRetailAuthority() throws Exception {
		assertMetadata(80020, "[Event] Solorius Joy", 1180020, "ELYOS", 182214012, 182214013,
			164002020, 160010097);
		assertMetadata(80021, "[Event] Festive Us", 1180021, "ASMODIANS", 182214014, 182214015,
			164002021, 160010098);
	}

	@Test
	void dialogChainPreservesItemsEmotionsAndRewardProtocol() throws Exception {
		assertFlow(80020, 799769, 799768, 203170, 203140, 182214012, 182214013);
		assertFlow(80021, 799784, 799783, 203618, 203650, 182214014, 182214015);
	}

	@Test
	void repeatAndEventExpiryRoutesRemainExplicit() throws Exception {
		for (int questId : new int[] {80020, 80021}) {
			QuestDefinition definition = load(questId);
			assertTrue(definition.transitions().stream().anyMatch(transition ->
				"complete".equals(transition.sourceNode()) && "s0".equals(transition.targetNode())
					&& transition.conditions().contains(new QuestCondition.StartEligible())
					&& transition.conditions().contains(new QuestCondition.EventActive(true))));
			for (String source : List.of("s0", "s1", "s2", "reward", "complete")) {
				assertTrue(definition.transitions().stream().anyMatch(transition ->
					source.equals(transition.sourceNode()) && "unaccepted".equals(transition.targetNode())
						&& transition.event() instanceof QuestEvent.LevelUp
						&& transition.conditions().contains(new QuestCondition.EventActive(false))
						&& transition.actions().contains(new QuestAction.AbandonQuest())));
			}
		}
	}

	private static void assertMetadata(int questId, String name, int displayNameId, String race,
			int wageItem, int invitationItem, int rewardItemA, int rewardItemB) throws Exception {
		QuestMetadata metadata = load(questId).metadata();
		assertEquals(name, metadata.name());
		assertEquals(displayNameId, metadata.displayNameId());
		assertEquals(10, metadata.minLevel());
		assertEquals(Set.of(race), metadata.permittedRaces());
		assertEquals(5, metadata.repeatPolicy().maxRepeatCount());
		assertEquals(List.of(new QuestItemRequirement(wageItem, 1),
			new QuestItemRequirement(invitationItem, 2)), metadata.questWorkItems());
		assertEquals(List.of(new QuestReward("GOLD", 0, 20000), new QuestReward("EXP", 0, 10000),
			new QuestReward("ITEM", rewardItemA, 1), new QuestReward("ITEM", rewardItemB, 1)),
			metadata.rewards());
		assertEquals(List.of(new QuestReward("ITEM", 188051108, 1)), metadata.extendedRewards());
	}

	private static void assertFlow(int questId, int rewardNpc, int firstNpc, int secondNpc, int thirdNpc,
			int wageItem, int invitationItem) throws Exception {
		QuestDefinition definition = load(questId);
		QuestTransition accept = talk(definition, "unaccepted", "s0", rewardNpc, 1002);
		assertTrue(accept.actions().contains(new QuestAction.GiveItem(wageItem, 1)));

		QuestTransition first = talk(definition, "s0", "s1", firstNpc, 10000);
		assertEquals(List.of(new QuestAction.GiveItem(invitationItem, 2),
			new QuestAction.RemoveItem(wageItem, 1)), first.actions());
		QuestTransition secondEmotion = talk(definition, "s1", "s1", secondNpc, 1694);
		assertEquals(new AfterCommitAction.BroadcastInteractionNpcEmotion(QuestNpcEmotion.NO),
			secondEmotion.afterCommit().getFirst());
		assertEquals(List.of(new QuestAction.RemoveItem(invitationItem, 1)),
			talk(definition, "s1", "s2", secondNpc, 10001).actions());
		QuestTransition thirdEmotion = talk(definition, "s2", "s2", thirdNpc, 2035);
		assertEquals(new AfterCommitAction.BroadcastInteractionNpcEmotion(QuestNpcEmotion.PANIC),
			thirdEmotion.afterCommit().getFirst());
		assertEquals(List.of(new QuestAction.RemoveItem(invitationItem, 1)),
			talk(definition, "s2", "reward", thirdNpc, 10002).actions());

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)),
			talk(definition, "reward", "reward", rewardNpc, -1).afterCommit());
		QuestTransition completion = talk(definition, "reward", "complete", rewardNpc, 8);
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertEquals(List.of(AfterCommitAction.RefreshPlayerStats.class,
			AfterCommitAction.SyncQuestState.class, AfterCommitAction.ShowQuestSelectionDialog.class),
			completion.afterCommit().stream().map(Object::getClass).toList());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target,
			int npcId, int dialogId) {
		return definition.transitions().stream().filter(transition -> source.equals(transition.sourceNode())
			&& target.equals(transition.targetNode()) && transition.event() instanceof QuestEvent.TalkToNpc talk
			&& talk.npcId() == npcId && Integer.valueOf(dialogId).equals(talk.dialogId())).findFirst().orElseThrow();
	}

	private static QuestDefinition load(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(DIR.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
