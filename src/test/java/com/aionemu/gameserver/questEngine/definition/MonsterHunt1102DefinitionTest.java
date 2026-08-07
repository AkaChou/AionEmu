package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full production-definition proof for the former MonsterHunt owner of quest 1102. */
class MonsterHunt1102DefinitionTest {
	private static final String DEFINITION =
		"/aion/data/static_data/quest_definition/quests/1102.xml";

	@Test
	void definitionCoversTheCompleteLegacyDialogAndKillLifecycle() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestTransition> transitions = compiled.definition().transitions();

		assertEquals(34, transitions.size());
		assertEquals(6, transitions.stream().filter(t -> t.event() instanceof QuestEvent.KillNpc).count());
		assertEquals(Set.of(210133, 210134), transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.KillNpc)
			.map(t -> ((QuestEvent.KillNpc) t.event()).npcId()).collect(Collectors.toSet()));
		assertEquals(Set.of(31, 1007, 1002, 20000, 1003, 1004, 20001, 1008),
			dialogIds(transitions, "unaccepted"));
		assertEquals(Set.of(31, 1009), dialogIds(transitions, "target-count-reached"));
		assertEquals(Set.of(-1, 1009), transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("reward"))
			.map(t -> ((QuestEvent.TalkToNpc) t.event()).dialogId()).collect(Collectors.toSet()));
		assertEquals(16, transitions.stream().filter(t -> t.targetNode().equals("complete")).count());
	}

	@Test
	void everyKillAdvancesOneStepAndSendsTheProgressPacket() throws Exception {
		CompiledQuestDefinition compiled = definition();
		int packed = 0;
		for (String source : List.of("started", "one-kill", "two-kills")) {
			QuestTransition transition = compiled.definition().transitions().stream()
				.filter(t -> t.sourceNode().equals(source))
				.filter(t -> t.event().equals(new QuestEvent.KillNpc(210134)))
				.findFirst().orElseThrow();
			var plan = QuestMutationPlanner.plan(compiled,
				new QuestSnapshot(7, 1102, QuestStatus.START, packed, Map.of()),
				transition).orElseThrow();
			packed++;
			assertEquals(packed, plan.nextPackedVariables());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				transition.afterCommit());
		}
	}

	@Test
	void acceptanceFailsClosedWithoutTheQuestTemplatePreconditions() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestTransition accept = talk(compiled, "unaccepted", 1002);
		QuestSnapshot snapshot = new QuestSnapshot(7, 1102, QuestStatus.NONE, 0, Map.of())
			.withCompletedQuestIds(Set.of(1101));

		assertTrue(QuestMutationPlanner.plan(compiled, snapshot, accept).isEmpty());
		assertTrue(QuestMutationPlanner.plan(compiled,
			snapshot.withStartEligibility(QuestStartEligibility.rejected("PREREQUISITE")), accept).isEmpty());
		assertTrue(QuestMutationPlanner.plan(compiled,
			snapshot.withStartEligibility(QuestStartEligibility.allowed()), accept).isPresent());
	}

	@Test
	void completionUsesTheTypedRewardsAndLifecycle() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestAction> expected = List.of(
			new QuestAction.GrantReward("GOLD", 0, 400, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 180, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0));
		List<AfterCommitAction> afterCommit = List.of(new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10));

		for (QuestTransition transition : compiled.definition().transitions().stream()
				.filter(t -> t.targetNode().equals("complete")).toList()) {
			assertEquals(expected, transition.actions());
			assertEquals(afterCommit, transition.afterCommit());
		}
	}

	@Test
	void packagedDefinitionContainsOnlyQuestSemanticsAndHasNoLegacyOwner() throws Exception {
		String definition;
		try (InputStream input = resource(DEFINITION)) {
			definition = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
		assertFalse(definition.contains("<evidence"));
		assertFalse(definition.contains("ownership="));

		String legacy;
		try (InputStream input = resource("/aion/data/static_data/quest_script_data/poeta.xml")) {
			legacy = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
		assertFalse(legacy.contains("id=\"1102\""));
	}

	private static Set<Integer> dialogIds(List<QuestTransition> transitions, String source) {
		return transitions.stream().filter(t -> t.sourceNode().equals(source))
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc)
			.map(t -> ((QuestEvent.TalkToNpc) t.event()).dialogId()).collect(Collectors.toSet());
	}

	private static QuestTransition talk(CompiledQuestDefinition compiled, String source, int dialogId) {
		return compiled.definition().transitions().stream().filter(t -> t.sourceNode().equals(source))
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203057 && talk.dialogId() == dialogId)
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = resource(DEFINITION)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private InputStream resource(String path) {
		InputStream input = getClass().getResourceAsStream(path);
		if (input == null) throw new IllegalStateException("missing resource " + path);
		return input;
	}
}
