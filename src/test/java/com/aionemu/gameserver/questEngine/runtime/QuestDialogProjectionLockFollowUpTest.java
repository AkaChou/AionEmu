package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 DIALOG_PROJECTION_LOCK 批次（1614/11216）的步骤节点对话路由合同：
 * var0=1 阶段的 QUEST_SELECT 必须挂在 v1 源节点上，玩家才能看到承载后续按钮的 SELECT2(1352) 页面；
 * started 节点保留 var0=0 步骤编码投影。
 * Locks the step-node dialog routing contract of the DIALOG_PROJECTION_LOCK batch (1614/11216):
 * the var0=1 QUEST_SELECT route must live on the v1 source node so players can reach the
 * SELECT2(1352) page that carries the follow-up buttons; the started node keeps its var0=0
 * step-encoding projection.
 */
class QuestDialogProjectionLockFollowUpTest {
	private static final int BELBUA = 204645;
	private static final int SULINIA = 799017;

	@Test
	void quest1614ShowsTheEscortPageFromTheV1Node() throws Exception {
		CompiledQuestDefinition definition = load(1614);
		assertEquals(Map.of("var0", 0), node(definition, "started").projection().variables());

		QuestTransition escortPage = dialogRoute(definition, BELBUA, QuestDialogAction.QUEST_SELECT, "v1");
		assertEquals("v1", escortPage.sourceNode());
		assertEquals("v1", escortPage.targetNode());
		assertEquals(List.of(), escortPage.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			escortPage.afterCommit());

		QuestTransition startStep = dialogRoute(definition, BELBUA, QuestDialogAction.SETPRO1, "started");
		assertEquals("v1", startStep.targetNode());

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(startStep);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(Map.of("var0", 1), variables(definition, runtime));
			runtime.prepare(escortPage);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertTrue(outcome.handled());
			assertTrue(outcome.packets().stream().anyMatch(QuestDialogProjectionLockFollowUpTest::isSelect2Page),
				"SELECT2 page packet was not sent");
		}
	}

	@Test
	void quest11216ShowsTheCollectionReportPageFromTheV1Node() throws Exception {
		CompiledQuestDefinition definition = load(11216);
		assertEquals(Map.of("var0", 0), node(definition, "started").projection().variables());

		QuestTransition reportPage = dialogRoute(definition, SULINIA, QuestDialogAction.QUEST_SELECT, "v1");
		assertEquals("v1", reportPage.sourceNode());
		assertEquals("v1", reportPage.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			reportPage.afterCommit());

		QuestTransition acceptStep = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event().equals(
				new QuestEvent.TalkToNpc(SULINIA, QuestDialogAction.SELECT1_1.id())))
			.filter(candidate -> "started".equals(candidate.sourceNode()))
			.findFirst().orElseThrow();
		assertEquals("v1", acceptStep.targetNode());

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(acceptStep);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(Map.of("var0", 1), variables(definition, runtime));
			runtime.prepare(reportPage);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertTrue(outcome.handled());
			assertTrue(outcome.packets().stream().anyMatch(QuestDialogProjectionLockFollowUpTest::isSelect2Page),
				"SELECT2 page packet was not sent");
		}
	}

	private static boolean isSelect2Page(ServerPacketObservation packet) {
		return packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW
			&& packet.dialogId() == QuestDialogPage.SELECT2.id();
	}

	private static QuestTransition dialogRoute(CompiledQuestDefinition definition, int npcId,
			QuestDialogAction action, String sourceNode) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.filter(candidate -> candidate.sourceNode().equals(sourceNode))
			.findFirst().orElseThrow();
	}

	private static Map<String, Integer> variables(CompiledQuestDefinition definition, QuestE2eRuntime runtime) {
		return definition.definition().progressLayout().unpack(runtime.state().packedVariables());
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				QuestDialogProjectionLockFollowUpTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
