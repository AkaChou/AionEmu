package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientActionRequest;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.e2e.client.QuestProtocolLoop;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eRuntime;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通过真实 CM_DIALOG_SELECT 协议验证重发断路器：客户端反复下发同一个没有后续页的动作时，
 * 服务端在阈值内关闭对话窗口、停止继续路由，并且不改变任务状态。
 * Verifies the resend breaker through the real CM_DIALOG_SELECT protocol: when the client keeps sending the
 * same dialog action that has no continuation, the server closes the dialog window at the threshold, stops
 * routing the selection again, and leaves the quest state untouched.
 */
class QuestDialogLoopBreakerProductionFlowTest {
	private static final int QUEST_ID = 990066;
	private static final int NPC_ID = 203534;
	/**
	 * 与 CM_DIALOG_SELECT 的断路器阈值一致：连续相同选择达到该次数即判定循环。
	 * Mirrors the CM_DIALOG_SELECT breaker threshold: identical consecutive selections that mark a loop.
	 */
	private static final int LOOP_THRESHOLD = 4;
	/**
	 * 高于生产最小重发间隔（800ms），复现真实客户端约 2 秒的自动重发节奏。
	 * Above the production minimum resend gap (800 ms) so the test reproduces the client resend cadence.
	 */
	private static final long LOOP_RESEND_GAP_MILLIS = 900;

	@Test
	void breaksTheResendLoopAfterTheConfiguredNumberOfIdenticalSelections() throws Exception {
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(definitionXml().getBytes(StandardCharsets.UTF_8)));
		QuestTransition entry = definition.definition().transitions().getFirst();

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(entry);
			assertTrue(runtime.dispatchPrepared().handled());
			int objectId = runtime.expectedDialogTargetObjectId();
			try (QuestProtocolLoop protocol = new QuestProtocolLoop(runtime)) {
				for (int attempt = 1; attempt < LOOP_THRESHOLD; attempt++) {
					dispatchUnansweredSelect(protocol, objectId, attempt);
				}

				QuestHeadlessClient.DispatchOutcome breaker = selectUnroutedAction(protocol, objectId);

				assertFalse(breaker.handled(), breaker::toString);
				assertEquals(QuestStatus.START, runtime.state().status());
				assertTrue(breaker.packets().stream().anyMatch(packet ->
					packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW
						&& packet.dialogId() == 0 && packet.targetObjectId() == 0), breaker::toString);
			}
		}
	}

	@Test
	void reopeningTheDialogRestartsTheResendCounter() throws Exception {
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(definitionXml().getBytes(StandardCharsets.UTF_8)));
		QuestTransition entry = definition.definition().transitions().getFirst();

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(entry);
			assertTrue(runtime.dispatchPrepared().handled());
			int objectId = runtime.expectedDialogTargetObjectId();
			try (QuestProtocolLoop protocol = new QuestProtocolLoop(runtime)) {
				for (int attempt = 1; attempt < LOOP_THRESHOLD; attempt++) {
					dispatchUnansweredSelect(protocol, objectId, attempt);
				}

				// 真实 CM_SHOW_DIALOG：重新打开对话必须清零重发计数。
				// A real CM_SHOW_DIALOG reopens the dialog and must reset the resend counter.
				protocol.dispatch(ClientActionRequest.useObject(QUEST_ID, NPC_ID, objectId));
				for (int attempt = 1; attempt < LOOP_THRESHOLD; attempt++) {
					dispatchUnansweredSelect(protocol, objectId, attempt);
				}

				QuestHeadlessClient.DispatchOutcome breaker = selectUnroutedAction(protocol, objectId);

				assertFalse(breaker.handled(), breaker::toString);
				assertTrue(breaker.packets().stream().anyMatch(packet ->
					packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW
						&& packet.dialogId() == 0 && packet.targetObjectId() == 0), breaker::toString);
			}
		}
	}

	private static void dispatchUnansweredSelect(QuestProtocolLoop protocol, int objectId, int attempt)
			throws InterruptedException {
		int unansweredAttempt = attempt;
		QuestHeadlessClient.DispatchOutcome unanswered = selectUnroutedAction(protocol, objectId);
		assertFalse(unanswered.handled(), unanswered::toString);
		assertTrue(unanswered.packets().isEmpty(),
			() -> "attempt " + unansweredAttempt + " answered: " + unanswered.packets());
		Thread.sleep(LOOP_RESEND_GAP_MILLIS);
	}

	private static QuestHeadlessClient.DispatchOutcome selectUnroutedAction(QuestProtocolLoop protocol, int objectId) {
		return protocol.dispatch(ClientActionRequest.dialog(QUEST_ID, NPC_ID, objectId,
			QuestDialogAction.SELECT_QUEST_REWARD.id()));
	}

	private static String definitionXml() {
		return """
			<quest-definition id="990066" version="1">
			  <metadata name="loop-breaker" display-name-id="1" min-level="0" max-level="99" category="QUEST"/>
			  <progress><bit-field name="var0" offset="0" width="3" min="0" max="7" persistence="PERSISTENT" scope="LOCAL"/></progress>
			  <nodes>
			    <node label="started" status="START"/>
			  </nodes>
			  <transitions>
			    <transition source="started" target="started">
			      <event><dialog type="TALK_TO_NPC" npc-id="203534" action="QUEST_SELECT"/></event>
			      <after-commit><dialog type="SHOW_QUEST_PAGE" page="SELECT2"/></after-commit>
			    </transition>
			  </transitions>
			</quest-definition>
			""";
	}
}
