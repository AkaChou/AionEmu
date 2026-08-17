package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.e2e.QuestE2ePacketValidator;
import com.aionemu.gameserver.questEngine.e2e.client.ClientActionRequest;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.e2e.client.QuestProtocolLoop;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eRuntime;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定客户端可见任务页面必须在已提交的任务状态同步之后发送。
 * Locks client-visible quest pages to be sent after the committed quest state is synchronized.
 */
class QuestPacketOrderRegressionTest {
	@Test
	void quest1573SynchronizesRewardStateBeforeDefaultSuccessPage() throws Exception {
		assertRouteContract(1573, "v2", projection(QuestStatus.START, 2), "reward",
			projection(QuestStatus.REWARD, 2), 730025, QuestDialogAction.QUEST_SELECT, null,
			List.of(), List.of(new QuestAction.RemoveItem(182201735, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())));
	}

	@Test
	void quest1607SynchronizesRewardStateBeforeRewardWindow() throws Exception {
		assertRouteContract(1607, "z4", projection(QuestStatus.START, 5), "reward",
			projection(QuestStatus.REWARD, 5), 204574, QuestDialogAction.SELECT_QUEST_REWARD, null,
			List.of(), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
	}

	@Test
	void quest2392SynchronizesEachSelectedItemBranchBeforeItsRewardWindow() throws Exception {
		assertItemRewardRoute(QuestDialogAction.SETPRO1, 182204159, "r1", 1,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertItemRewardRoute(QuestDialogAction.SETPRO2, 182204160, "r2", 2,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);
		assertItemRewardRoute(QuestDialogAction.SETPRO3, 182204161, "r3", 3,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW3);
	}

	@Test
	void quest2533SynchronizesRewardStateBeforeReportPage() throws Exception {
		assertRouteContract(2533, "v1", projection(QuestStatus.START, 1), "reward",
			projection(QuestStatus.REWARD, 1), 204801, QuestDialogAction.QUEST_SELECT, null,
			List.of(), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())));
	}

	@Test
	void quest10032SynchronizesConsumedItemBeforeSuccessPage() throws Exception {
		assertRouteContract(10032, "s7", projection(QuestStatus.START, 7), "s7",
			projection(QuestStatus.START, 7), 799503, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, 0,
			List.of(new QuestCondition.QuestVariableIs("var0", 7), new QuestCondition.HasItem(182215620, 1)),
			List.of(new QuestAction.RemoveItem(182215620, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())));
	}

	@Test
	void quest24153SynchronizesRewardStateBeforeRewardWindow() throws Exception {
		assertRouteContract(24153, "started", projection(QuestStatus.START, 0), "reward",
			projection(QuestStatus.REWARD, 0), 204787, QuestDialogAction.SELECT_QUEST_REWARD, null,
			List.of(), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
	}

	@Test
	void protocolLoopSendsCommittedStateBeforeEveryRepairedPage() throws Exception {
		assertProtocolPacketOrder(1573, "v2", QuestDialogAction.QUEST_SELECT.id(), null);
		assertProtocolPacketOrder(1607, "z4", QuestDialogAction.SELECT_QUEST_REWARD.id(), null);
		assertProtocolPacketOrder(2392, "started", QuestDialogAction.SETPRO1.id(), 0);
		assertProtocolPacketOrder(2392, "started", QuestDialogAction.SETPRO2.id(), 0);
		assertProtocolPacketOrder(2392, "started", QuestDialogAction.SETPRO3.id(), 0);
		assertProtocolPacketOrder(2533, "v1", QuestDialogAction.QUEST_SELECT.id(), null);
		assertProtocolPacketOrder(10032, "s7", QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 0);
		assertProtocolPacketOrder(24153, "started", QuestDialogAction.SELECT_QUEST_REWARD.id(), null);
	}

	private static void assertItemRewardRoute(QuestDialogAction action, int itemId, String target, int variable,
			QuestDialogPage page) throws Exception {
		assertRouteContract(2392, "started", projection(QuestStatus.START, 0), target,
			projection(QuestStatus.REWARD, variable), 798085, action, 0,
			List.of(new QuestCondition.HasItem(itemId, 1)), List.of(new QuestAction.RemoveItem(itemId, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(page.id())));
	}

	private static void assertProtocolPacketOrder(int questId, String source, int dialogId,
			Integer priority) throws Exception {
		CompiledQuestDefinition definition = compiledDefinition(questId);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.filter(candidate -> Objects.equals(priority, candidate.priority()))
			.findFirst().orElseThrow();
		QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) transition.event();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			int objectId = runtime.expectedDialogTargetObjectId();
			try (QuestProtocolLoop protocol = new QuestProtocolLoop(runtime)) {
				QuestHeadlessClient.DispatchOutcome outcome = protocol.dispatch(
					ClientActionRequest.dialog(questId, talk.npcId(), objectId, dialogId));
				assertTrue(outcome.handled(), outcome::toString);
				assertFalse(outcome.failed(), outcome::toString);
				QuestE2ePacketValidator.Result validation = QuestE2ePacketValidator.validate(
					definition, transition, objectId, outcome.packets());
				assertTrue(validation.valid(), () -> questId + ": " + validation);
				List<ServerPacketObservation.Type> packetTypes = outcome.packets().stream()
					.map(ServerPacketObservation::type).toList();
				int syncIndex = packetTypes.indexOf(ServerPacketObservation.Type.QUEST_ACTION);
				int pageIndex = packetTypes.indexOf(ServerPacketObservation.Type.DIALOG_WINDOW);
				assertTrue(syncIndex >= 0 && pageIndex > syncIndex,
					() -> questId + ": packet order=" + packetTypes);
			}
		}
	}

	private static void assertRouteContract(int questId, String source, NodeProjection sourceProjection,
			String target, NodeProjection targetProjection, int npcId, QuestDialogAction action, Integer priority,
			List<QuestCondition> conditions, List<QuestAction> actions, List<AfterCommitAction> afterCommit)
			throws Exception {
		QuestDefinition definition = definition(questId);
		QuestTransition transition = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.filter(candidate -> Objects.equals(priority, candidate.priority()))
			.findFirst().orElseThrow();

		assertEquals(source, transition.sourceNode());
		assertEquals(sourceProjection, node(definition, source).projection());
		assertEquals(new QuestEvent.TalkToNpc(npcId, action.id()), transition.event());
		assertEquals(priority, transition.priority());
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(target, transition.targetNode());
		assertEquals(targetProjection, node(definition, target).projection());
		assertEquals(afterCommit, transition.afterCommit());
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static NodeProjection projection(QuestStatus status, int var0) {
		return new NodeProjection(status, Map.of("var0", var0));
	}

	private static QuestDefinition definition(int questId) throws Exception {
		return compiledDefinition(questId).definition();
	}

	private static CompiledQuestDefinition compiledDefinition(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestPacketOrderRegressionTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource));
		}
	}
}
