package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定船长击杀链任务（3711/4711）步骤 NPC 的跳步防护合同：进行中状态向步骤 NPC 发送
 * SELECT_QUEST_REWARD 不得进入 REWARD（NPC_REPORT 展开的无条件报告边已移除），同时保留
 * QUEST_SELECT 的 SELECT2 页面入口和完整步骤链到最后一击。
 * Locks the step-NPC skip-guard contract for the captain-kill chain quests (3711/4711): sending
 * SELECT_QUEST_REWARD to a step NPC mid-chain must not enter REWARD (the unconditional report edge
 * expanded from NPC_REPORT was removed), while the QUEST_SELECT SELECT2 page entry and the full
 * step chain down to the final kill stay intact.
 */
class QuestStepNpcSkipGuardTest {
	private static final int ACTION_STEP_NPC = 730196;
	private static final int CAPTAIN = 214823;

	@Test
	void quest3711StepNpcRejectsPrematureRewardReportAndKeepsChain() throws Exception {
		CompiledQuestDefinition definition = load(3711);
		assertSkipGuarded(definition, 279045, 730196, CAPTAIN, 2);
	}

	@Test
	void quest4711StepNpcRejectsPrematureRewardReportAndKeepsChain() throws Exception {
		CompiledQuestDefinition definition = load(4711);
		assertSkipGuarded(definition, 279042, 730196, CAPTAIN, 2);
	}

	private static void assertSkipGuarded(CompiledQuestDefinition definition, int talkStepNpc,
			int actionStepNpc, int killNpc, int finalStepValue) throws Exception {
		// 步骤 NPC 不再有无条件 SELECT_QUEST_REWARD started->reward 边。
		assertTrue(definition.definition().transitions().stream()
			.filter(candidate -> candidate.event().equals(
				new QuestEvent.TalkToNpc(actionStepNpc, QuestDialogAction.SELECT_QUEST_REWARD.id())))
			.filter(candidate -> "started".equals(candidate.sourceNode())
				&& "reward".equals(candidate.targetNode()))
			.findFirst().isEmpty(), "unconditional skip edge must not exist");

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			// 走到步骤链末端:talk 步骤 SETPRO1 -> action 步骤 SETPRO2(var0=finalStepValue)。
			QuestTransition firstHandoff = dialogRoute(definition, talkStepNpc, QuestDialogAction.SETPRO1);
			runtime.prepare(firstHandoff);
			assertTrue(runtime.dispatchPrepared().handled());
			QuestTransition secondHandoff = dialogRoute(definition, actionStepNpc, QuestDialogAction.SETPRO2);
			runtime.prepare(secondHandoff);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(Map.of("var0", finalStepValue), unpack(definition, runtime));

			// 中间态直接索要奖励必须无响应且不改变状态。
			QuestHeadlessClient.DispatchOutcome skip = runtime.dispatchWorld(
				new QuestEvent.TalkToNpc(actionStepNpc, QuestDialogAction.SELECT_QUEST_REWARD.id()));
			assertFalse(skip.handled(), "premature reward report was handled");
			assertEquals(QuestStatus.START, runtime.state().status());

			// 页面入口保留:QUEST_SELECT 仍返回 SELECT2 页面供 SETPRO 按钮交互。
			QuestTransition pageEntry = definition.definition().transitions().stream()
				.filter(candidate -> candidate.event().equals(
					new QuestEvent.TalkToNpc(actionStepNpc, QuestDialogAction.QUEST_SELECT.id())))
				.filter(candidate -> "started".equals(candidate.sourceNode()))
				.findFirst().orElseThrow();
			runtime.prepare(pageEntry);
			QuestHeadlessClient.DispatchOutcome page = runtime.dispatchPrepared();
			assertTrue(page.handled());
			assertTrue(page.packets().stream().anyMatch(packet ->
					packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW
						&& packet.dialogId() == QuestDialogPage.SELECT2.id()),
				"SELECT2 page packet missing");

			// 最后一击照常进入 REWARD。
			runtime.prepare(killRoute(definition, killNpc));
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
		}
	}

	private static QuestTransition dialogRoute(CompiledQuestDefinition definition, int npcId,
			QuestDialogAction action) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.findFirst().orElseThrow();
	}

	private static QuestTransition killRoute(CompiledQuestDefinition definition, int npcId) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpc single
				? single.npcId() == npcId
				: candidate.event() instanceof QuestEvent.KillNpcSet kills && kills.npcIds().contains(npcId))
			.findFirst().orElseThrow();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestE2eRuntime runtime) {
		return definition.definition().progressLayout().unpack(runtime.state().packedVariables());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				QuestStepNpcSkipGuardTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
