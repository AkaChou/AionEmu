package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-alignment coverage for the four Inggison item-collecting daily owners. */
class InggisonDailyQuestDefinitionTest {
	private static final Map<Integer, ExpectedQuest> EXPECTED = Map.of(
		11064, new ExpectedQuest(1125064, 799043, 182206853, List.of(216003, 216004), 4931644),
		11065, new ExpectedQuest(1125065, 799043, 182206851, List.of(215990, 215991), 6112882),
		11066, new ExpectedQuest(1125066, 799049, 182206854, List.of(215923, 215924), 4931644),
		11067, new ExpectedQuest(1125067, 799049, 182206852, List.of(215927, 215928), 6112882));

	@Test
	void dailyOwnersPreserveRetailMetadataDropsAndExactTurnInRemoval() {
		for (Map.Entry<Integer, ExpectedQuest> entry : EXPECTED.entrySet()) {
			int questId = entry.getKey();
			ExpectedQuest expected = entry.getValue();
			QuestDefinition definition = load(questId).definition();
			QuestMetadata metadata = definition.metadata();

			assertEquals(questId, definition.id());
			assertEquals(expected.displayNameId(), metadata.displayNameId());
			assertEquals(999, metadata.minLevel());
			assertEquals(999, metadata.maxLevel());
			assertEquals(Set.of("ELYOS"), metadata.permittedRaces());
			assertEquals("QUEST", metadata.category());
			assertTrue(metadata.cannotShare());
			assertEquals(new RepeatPolicy(20, 0, true, false), metadata.repeatPolicy());
			assertEquals(Set.of("ALL"), metadata.repeatCycles());
			assertEquals(List.of(new QuestItemRequirement(expected.itemId(), 10)), metadata.itemRequirements());
			assertEquals(List.of(
				new QuestReward("EXP", 0, expected.experience()),
				new QuestReward("AP", 0, 400),
				new QuestReward("ITEM", 186000469, 54)), metadata.rewards());
			assertEquals(expected.dropNpcIds().stream()
				.map(npcId -> new QuestDrop(npcId, expected.itemId(), 100, true, 0)).toList(), metadata.drops());

			List<QuestTransition> transitions = definition.transitions();
			List<QuestTransition> successfulTurnIns = transitions.stream()
				.filter(transition -> transition.sourceNode().equals("started")
					&& transition.targetNode().equals("reward")
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == expected.npcId()
					&& Set.of(39, 20002).contains(talk.dialogId()))
				.toList();
			assertEquals(2, successfulTurnIns.size());
			assertTrue(successfulTurnIns.stream().allMatch(transition ->
				transition.conditions().contains(new QuestCondition.HasItem(expected.itemId(), 10))
					&& transition.actions().contains(new QuestAction.RemoveItem(expected.itemId(), 10))
					&& transition.actions().stream().noneMatch(action -> action.equals(
						new QuestAction.RemoveItem(expected.itemId(), QuestAction.RemoveItem.ALL)))
					&& transition.afterCommit().contains(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY))
					&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(5))));

			QuestTransition insufficient39 = talk(transitions, "started", expected.npcId(), 39, "started");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2716)), insufficient39.afterCommit());
			QuestTransition insufficient20002 = talk(transitions, "started", expected.npcId(), 20002, "started");
			assertEquals(List.of(new AfterCommitAction.CloseDialog()), insufficient20002.afterCommit());

			QuestTransition directReport = transitions.stream()
				.filter(transition -> transition.sourceNode().equals("started")
					&& transition.targetNode().equals("reward")
					&& transition.event().equals(new QuestEvent.TalkToNpc(expected.npcId(), 10255)))
				.findFirst().orElseThrow();
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), directReport.afterCommit());

			List<QuestTransition> completions = transitions.stream()
				.filter(transition -> transition.sourceNode().equals("reward")
					&& transition.targetNode().equals("complete")
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == expected.npcId())
				.toList();
			assertEquals(16, completions.size());
			assertTrue(completions.stream().allMatch(transition ->
				transition.actions().contains(new QuestAction.GrantReward("EXP", 0, expected.experience(),
					QuestRewardAmountMode.QUEST_BASE))
					&& transition.actions().contains(new QuestAction.GrantReward("AP", 0, 400,
						QuestRewardAmountMode.QUEST_BASE))
					&& transition.actions().contains(new QuestAction.GrantReward("ITEM", 186000469, 54))
					&& transition.actions().contains(new QuestAction.CompleteQuest(0))));
			assertTrue(completions.stream().allMatch(transition -> transition.afterCommit().contains(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION))));
		}
	}

	@Test
	void productionDefinitionsRemainCatalogOwnedAndLegacyNodesAreGone() throws Exception {
		String catalog;
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quest_definition_catalog.xml")) {
			catalog = new String(Objects.requireNonNull(input).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
		for (int questId : EXPECTED.keySet()) {
			assertEquals(1, occurrences(catalog, "id=\"" + questId + "\""));
			String legacy;
			try (InputStream input = getClass().getResourceAsStream(
					"/aion/data/static_data/quest_script_data/inggison.xml")) {
				legacy = new String(Objects.requireNonNull(input).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
			}
			assertFalse(legacy.contains("id=\"" + questId + "\""));
		}
	}

	private static int occurrences(String text, String token) {
		return text.split(java.util.regex.Pattern.quote(token), -1).length - 1;
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source, int npcId,
		Integer dialogId, String target) {
		return transitions.stream()
			.filter(transition -> transition.sourceNode().equals(source)
				&& transition.targetNode().equals(target)
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& Objects.equals(talk.dialogId(), dialogId))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = InggisonDailyQuestDefinitionTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource));
		} catch (Exception e) {
			throw new AssertionError("failed to compile quest " + questId, e);
		}
	}

	private record ExpectedQuest(int displayNameId, int npcId, int itemId, List<Integer> dropNpcIds,
		long experience) {
	}
}
