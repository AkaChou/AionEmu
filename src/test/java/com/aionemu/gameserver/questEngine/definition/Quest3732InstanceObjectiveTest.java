package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eRuntime;

/**
 * 验证任务 3732 摧毁诺克萨纳要塞城门并击败神将后进入奖励阶段。
 * Verifies quest 3732 enters its reward phase after destroying the gate and defeating the general.
 */
class Quest3732InstanceObjectiveTest {
	@Test
	void destroyingTheGateAndDefeatingTheGeneralUnlocksBrandoReport() throws Exception {
		CompiledQuestDefinition compiled = load();
		QuestDefinition definition = compiled.definition();

		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 1)),
			definition.nodes().stream().filter(node -> node.label().equals("reward")).findFirst().orElseThrow()
				.projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 1)),
			definition.nodes().stream().filter(node -> node.label().equals("gate-destroyed")).findFirst().orElseThrow()
				.projection());
		QuestTransition kill = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("started"))
			.filter(transition -> transition.targetNode().equals("gate-destroyed"))
			.filter(transition -> transition.event().equals(new QuestEvent.KillNpc(256694)))
			.findFirst().orElseThrow();
		assertEquals(List.of(), kill.conditions());
		assertEquals(List.of(), kill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			kill.afterCommit());

		QuestTransition general = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("gate-destroyed"))
			.filter(transition -> transition.targetNode().equals("reward"))
			.filter(transition -> transition.event().equals(new QuestEvent.KillNpc(256693)))
			.findFirst().orElseThrow();
		assertEquals(List.of(), general.conditions());
		assertEquals(List.of(), general.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			general.afterCommit());

		QuestTransition openReport = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward"))
			.filter(transition -> transition.targetNode().equals("reward"))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(800518,
				QuestDialogAction.QUEST_SELECT.id())))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			openReport.afterCommit());

		QuestTransition report = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward"))
			.filter(transition -> transition.targetNode().equals("reward"))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(800518,
				QuestDialogAction.SELECT_QUEST_REWARD.id())))
			.findFirst().orElseThrow();
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		assertTrue(definition.transitions().stream().noneMatch(transition -> transition.sourceNode().equals("started")
			&& transition.targetNode().equals("reward")
			&& transition.event().equals(new QuestEvent.TalkToNpc(800518,
				QuestDialogAction.SELECT_QUEST_REWARD.id()))));

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(compiled)) {
			runtime.prepare(kill);
			assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(256694)).handled());
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 1),
				definition.progressLayout().unpack(runtime.state().packedVariables()));
			assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(256693)).handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
			assertEquals(Map.of("var0", 1),
				definition.progressLayout().unpack(runtime.state().packedVariables()));
		}
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest3732InstanceObjectiveTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/3732.xml")) {
			if (input == null) {
				throw new AssertionError("missing quest 3732 resource");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
