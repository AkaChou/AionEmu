package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runtime parity for the former Java owners 80030 and 80033. */
class QuestCharmedEventDefinitionTest {
	private static final Path DIR = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");

	@Test
	void metadataMatchesLegacyAndRetailAuthority() throws Exception {
		assertMetadata(80030, "[Event] An Unwelcome Gaze", "ELYOS");
		assertMetadata(80033, "[Event] Averting The Gaze", "ASMODIANS");
	}

	@Test
	void charmCardSchedulesRaceScopedExternalEventRefresh() throws Exception {
		assertSchedule(80030, 80029, Race.ELYOS, 80030, 80034, 80035, 80036);
		assertSchedule(80033, 80032, Race.ASMODIANS, 80033, 80037, 80038, 80039);
	}

	@Test
	void delayedRefreshStartsSelfAndRestartsCompletedChildFromLiveInventory() throws Exception {
		CompiledQuestDefinition self = load(80030);
		QuestTransition selfRefresh = transition(self.definition(), "unaccepted", "started",
			QuestEvent.EventQuestRefresh.class);
		QuestSnapshot selfSnapshot = new QuestSnapshot(7, 80030, QuestStatus.NONE, 0,
			Map.of(164002015, 1)).withStartEligibility(QuestStartEligibility.allowed());
		assertEquals(QuestStatus.START, QuestMutationPlanner.plan(self, selfSnapshot,
			new QuestEvent.EventQuestRefresh(), selfRefresh).orElseThrow().nextStatus());

		CompiledQuestDefinition child = load(80034);
		QuestTransition restart = transition(child.definition(), "complete", "started",
			QuestEvent.EventQuestRefresh.class);
		QuestSnapshot childSnapshot = new QuestSnapshot(7, 80034, QuestStatus.COMPLETE, 0,
			Map.of(164002016, 10)).withStartEligibility(QuestStartEligibility.allowed());
		assertEquals(QuestStatus.START, QuestMutationPlanner.plan(child, childSnapshot,
			new QuestEvent.EventQuestRefresh(), restart).orElseThrow().nextStatus());
	}

	@Test
	void dialogAndInactiveAsmodianFailureRemainExplicit() throws Exception {
		CompiledQuestDefinition definition = load(80033);
		QuestTransition reward = talk(definition.definition(), "started", "reward", 799781, 1009, true);
		assertEquals(List.of(new QuestAction.RemoveItem(164002015, 1)), reward.actions());
		assertTrue(talk(definition.definition(), "reward", "complete", 799781, 8, false).actions().stream()
			.anyMatch(QuestAction.CompleteQuest.class::isInstance));

		QuestTransition inactive = definition.definition().transitions().stream()
			.filter(t -> "unaccepted".equals(t.sourceNode()) && t.event() instanceof QuestEvent.UseItem
				&& t.conditions().contains(new QuestCondition.EventActive(80032, false)))
			.findFirst().orElseThrow();
		QuestSnapshot snapshot = new QuestSnapshot(7, 80033, QuestStatus.NONE, 0, Map.of())
			.withEventActivities(Map.of(80032, false));
		assertTrue(QuestMutationPlanner.plan(definition, snapshot, new QuestEvent.UseItem(188051133), inactive)
			.orElseThrow().requiredActions().contains(new QuestAction.BlockDefaultItemUse()));
	}

	private static void assertMetadata(int questId, String name, String race) throws Exception {
		QuestMetadata metadata = load(questId).definition().metadata();
		assertEquals(name, metadata.name());
		assertEquals(10, metadata.minLevel());
		assertEquals(Set.of(race), metadata.permittedRaces());
		assertEquals(List.of(new QuestItemRequirement(164002015, 1)), metadata.itemRequirements());
		assertEquals(List.of(new QuestItemRequirement(164002015, 1)), metadata.inventoryItems());
		assertEquals(List.of(new QuestReward("ITEM", 169610037, 1)), metadata.rewards());
	}

	private static void assertSchedule(int questId, int activeQuestId, Race race, int... targets)
			throws Exception {
		CompiledQuestDefinition definition = load(questId);
		QuestTransition use = definition.definition().transitions().stream()
			.filter(t -> "unaccepted".equals(t.sourceNode()) && t.event() instanceof QuestEvent.UseItem
				&& t.conditions().contains(new QuestCondition.EventActive(activeQuestId)))
			.findFirst().orElseThrow();
		assertTrue(use.conditions().contains(new QuestCondition.PlayerRaceIs(race)));
		AfterCommitAction.ScheduleEventQuestRefresh schedule = use.afterCommit().stream()
			.filter(AfterCommitAction.ScheduleEventQuestRefresh.class::isInstance)
			.map(AfterCommitAction.ScheduleEventQuestRefresh.class::cast).findFirst().orElseThrow();
		assertEquals(10, schedule.seconds());
		assertTrue(Arrays.equals(targets, schedule.questIds()));

		QuestSnapshot snapshot = new QuestSnapshot(7, questId, QuestStatus.NONE, 0, Map.of())
			.withRace(race).withEventActivities(Map.of(activeQuestId, true));
		assertTrue(QuestMutationPlanner.plan(definition, snapshot, new QuestEvent.UseItem(188051133), use)
			.isPresent());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			Class<? extends QuestEvent> eventType) {
		return definition.transitions().stream().filter(t -> source.equals(t.sourceNode())
			&& target.equals(t.targetNode()) && eventType.isInstance(t.event())).findFirst().orElseThrow();
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target,
			int npcId, int dialogId, boolean requiresItem) {
		return definition.transitions().stream().filter(t -> source.equals(t.sourceNode())
			&& target.equals(t.targetNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
			&& talk.npcId() == npcId && Integer.valueOf(dialogId).equals(talk.dialogId())
			&& (!requiresItem || t.conditions().contains(new QuestCondition.HasItem(164002015, 1))))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(DIR.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
