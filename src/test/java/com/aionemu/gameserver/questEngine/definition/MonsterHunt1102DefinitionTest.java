package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full production-definition proof for the former MonsterHunt owner of quest 1102. */
class MonsterHunt1102DefinitionTest {

	@Test
	void definitionCoversTheCompleteLegacyDialogAndKillLifecycle() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestTransition> transitions = compiled.definition().transitions();

		// 客户端 1102 HTML 无 select1_1(1012) 页：select1 的按钮直接是 ASK_QUEST_ACCEPT(1007)，
		// 接取链不含 1012 翻页。节点标签是原版合成器的规范形（a0..a3 = 0..3 次击杀）。
		// The client 1102 HTML has no select1_1 (1012) page: select1 goes straight to
		// ASK_QUEST_ACCEPT; grid labels are the retail canonical form a0..a3.
		assertEquals(35, transitions.size());
		assertEquals(6, transitions.stream().filter(t -> t.event() instanceof QuestEvent.KillNpc).count());
		assertEquals(Set.of(210133, 210134), transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.KillNpc)
			.map(t -> ((QuestEvent.KillNpc) t.event()).npcId()).collect(Collectors.toSet()));
		assertEquals(Set.of(31, 1007, 1002, 20000, 1003, 1004, 20001, 1008),
			dialogIds(transitions, "unaccepted"));
		assertEquals(Set.of(31, 1009), dialogIds(transitions, "a3"));
		assertEquals(Set.of(-1, 1009), transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("reward"))
			.map(t -> ((QuestEvent.TalkToNpc) t.event()).dialogId()).collect(Collectors.toSet()));
		assertEquals(16, transitions.stream().filter(t -> t.targetNode().equals("complete")).count());
	}

	@Test
	void everyKillAdvancesOneStepAndSendsTheProgressPacket() throws Exception {
		CompiledQuestDefinition compiled = definition();
		int packed = 0;
		for (String source : List.of("a0", "a1", "a2")) {
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
		QuestSnapshot snapshot = new QuestSnapshot(7, 1102, QuestStatus.NONE, 0, Map.of());

		assertTrue(QuestMutationPlanner.plan(compiled, snapshot, accept).isEmpty());
		assertTrue(QuestMutationPlanner.plan(compiled,
			snapshot.withStartEligibility(QuestStartEligibility.rejected("PREREQUISITE")), accept).isEmpty());
		assertTrue(QuestMutationPlanner.plan(compiled,
			snapshot.withCompletedQuestIds(Set.of(1101))
				.withStartEligibility(QuestStartEligibility.allowed()), accept).isPresent());
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
	void packagedDefinitionContainsOnlyQuestSemanticsAndHasNoLegacyOwner() {
		// 1102 已退役：生产 XML 不再进仓（内容在 git 历史里），定义由原版驱动合成，
		// 合成结果只含任务语义（无证据/归属元数据）。
		// 1102 is retired: no production XML ships; the synthesized definition carries quest semantics only.
		assertFalse(QuestXmlFixtures.productionXmlPresent(1102),
			"retired quest must not keep a production XML");
		CompiledQuestDefinition compiled = definition();
		assertEquals(1102, compiled.id());
		assertFalse(compiled.definition().nodes().isEmpty());

		assertFalse(legacyScriptDataExists(), "quest_script_data directory must be fully removed");
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

	private CompiledQuestDefinition definition() {
		// 1102 已迁到原版驱动（XML 不进仓）；装载走生产视图。
		// 1102 is retail-driven now; the definition comes from the production view.
		return ProductionQuestDefinitions.definition(1102);
	}

	private InputStream resource(String path) {
		InputStream input = getClass().getResourceAsStream(path);
		if (input == null) throw new IllegalStateException("missing resource " + path);
		return input;
	}
	private static boolean legacyScriptDataExists() {
		return java.nio.file.Files.exists(
			java.nio.file.Path.of("src/main/resources/aion/data/static_data/quest_script_data"));
	}

}
