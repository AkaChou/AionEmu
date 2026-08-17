package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientActionRequest;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.e2e.client.QuestProtocolLoop;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eRuntime;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通过真实对话协议和生产 dispatcher 验证 9550/9553 的装备物品起始条件。
 * Verifies quests 9550/9553 equipped-item start conditions through the real dialog protocol and production dispatcher.
 */
class QuestEquippedStartProductionFlowTest {
	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");
	private static final int REQUIRED_ITEM_ID = 125040015;

	@Test
	void soloriusQuestsRequireTheClientVisibleEquippedItemBeforeAcceptance() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		assertVisibleAction(oracle, 9550, 1011, QuestDialogAction.SELECT1_1.id());
		assertVisibleAction(oracle, 9550, 1012, QuestDialogAction.ASK_QUEST_ACCEPT.id());
		assertVisibleAction(oracle, 9550, 4, QuestDialogAction.QUEST_ACCEPT_1.id());
		assertVisibleAction(oracle, 9553, 1011, QuestDialogAction.ASK_QUEST_ACCEPT.id());
		assertVisibleAction(oracle, 9553, 4, QuestDialogAction.QUEST_ACCEPT_1.id());

		assertAcceptsWithEquippedItem(9550, 203738);
		assertAcceptsWithEquippedItem(9553, 204218);
		assertRejectsWithoutEquippedItem(9550, 203738, true);
		assertRejectsWithoutEquippedItem(9553, 204218, true);
		assertRejectsWithoutEquippedItem(9550, 203738, false);
		assertRejectsWithoutEquippedItem(9553, 204218, false);
	}

	private static void assertAcceptsWithEquippedItem(int questId, int npcId) throws Exception {
		CompiledQuestDefinition definition = definition(questId);
		QuestTransition accept = acceptTransition(definition, npcId);
		assertEquippedMetadata(definition);

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(accept);
			assertFalse(runtime.unsupportedFacts());
			int objectId = runtime.expectedDialogTargetObjectId();
			try (QuestProtocolLoop protocol = new QuestProtocolLoop(runtime)) {
				QuestHeadlessClient.DispatchOutcome outcome = protocol.dispatch(ClientActionRequest.dialog(
					questId, npcId, objectId, QuestDialogAction.QUEST_ACCEPT_1.id()));

				assertTrue(outcome.handled(), outcome::toString);
				assertFalse(outcome.failed(), outcome::toString);
				assertTrue(outcome.stateChanged(), outcome::toString);
				assertEquals(QuestStatus.START, runtime.state().status());
				assertEquals(0, runtime.state().packedVariables());
				QuestE2ePacketValidator.Result packets = QuestE2ePacketValidator.validate(
					definition, accept, objectId, outcome.packets());
				assertTrue(packets.valid(), packets::toString);
				assertPacketOrder(outcome.packets(), questId, objectId);
			}
		}
	}

	private static void assertRejectsWithoutEquippedItem(int questId, int npcId, boolean captured)
			throws Exception {
		CompiledQuestDefinition definition = definition(questId);
		QuestTransition accept = acceptTransition(definition, npcId);
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(accept);
			if (captured) {
				runtime.replaceEquippedItemFacts(Map.of());
			} else {
				runtime.clearCapturedEquipmentFacts();
			}
			int objectId = runtime.expectedDialogTargetObjectId();
			try (QuestProtocolLoop protocol = new QuestProtocolLoop(runtime)) {
				QuestHeadlessClient.DispatchOutcome outcome = protocol.dispatch(ClientActionRequest.dialog(
					questId, npcId, objectId, QuestDialogAction.QUEST_ACCEPT_1.id()));

				assertFalse(outcome.handled(), outcome::toString);
				assertFalse(outcome.failed(), outcome::toString);
				assertFalse(outcome.stateChanged(), outcome::toString);
				assertEquals(QuestStatus.NONE, runtime.state().status());
				assertTrue(outcome.packets().stream().noneMatch(packet ->
					packet.type() == ServerPacketObservation.Type.QUEST_ACTION
						|| packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW), outcome::toString);
			}
		}
	}

	private static void assertEquippedMetadata(CompiledQuestDefinition definition) {
		assertEquals(1, definition.definition().metadata().startConditionGroups().size());
		var conditions = definition.definition().metadata().startConditionGroups().getFirst().conditions();
		assertEquals(1, conditions.size());
		assertEquals("equipped", conditions.getFirst().type());
		assertEquals(REQUIRED_ITEM_ID, conditions.getFirst().questId());
	}

	private static void assertPacketOrder(List<ServerPacketObservation> packets, int questId, int objectId) {
		int syncIndex = firstIndex(packets, ServerPacketObservation.Type.QUEST_ACTION);
		int pageIndex = firstIndex(packets, ServerPacketObservation.Type.DIALOG_WINDOW);
		assertTrue(syncIndex >= 0 && pageIndex > syncIndex, packets::toString);
		ServerPacketObservation page = packets.get(pageIndex);
		assertEquals(questId, page.questId());
		assertEquals(objectId, page.targetObjectId());
		assertEquals(1003, page.dialogId());
	}

	private static int firstIndex(List<ServerPacketObservation> packets, ServerPacketObservation.Type type) {
		for (int index = 0; index < packets.size(); index++) {
			if (packets.get(index).type() == type) {
				return index;
			}
		}
		return -1;
	}

	private static void assertVisibleAction(ClientResourceOracle oracle, int questId, int pageId, int actionId) {
		assertTrue(oracle.pageExists(questId, pageId));
		assertTrue(oracle.actionExists(actionId));
		assertTrue(oracle.visibleActions(questId, pageId).stream()
			.anyMatch(action -> action.actionId() == actionId));
	}

	private static QuestTransition acceptTransition(CompiledQuestDefinition definition, int npcId) {
		return definition.definition().transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode()))
			.filter(transition -> "started".equals(transition.targetNode()))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(
				npcId, QuestDialogAction.QUEST_ACCEPT_1.id())))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestEquippedStartProductionFlowTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
