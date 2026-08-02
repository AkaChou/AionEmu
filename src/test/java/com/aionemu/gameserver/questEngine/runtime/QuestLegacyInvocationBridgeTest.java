package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestLegacyInvocationBridgeTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@Test
	void diagnosticSinkFailureCannotChangeLegacyBooleanResult() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestLegacyInvocationBridge bridge = new QuestLegacyInvocationBridge(ignored -> {
			throw new IllegalStateException("observation store unavailable");
		});

		boolean result = bridge.invoke(player, QUEST_ID, "TALK_TO_NPC", QuestDispatchContract.EXCLUSIVE,
			() -> {
				player.getQuestStateList().getQuestState(QUEST_ID).setStatus(QuestStatus.REWARD);
				return true;
			}, (value, stateChanged, recorder) -> value ? QuestRouteResult.HANDLED : QuestRouteResult.NOT_HANDLED);

		assertTrue(result);
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(QUEST_ID).getStatus());
	}

	@Test
	void diagnosticObservationCapturesTypedStateAndResultWithoutChangingHandlerResult() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		AtomicInteger records = new AtomicInteger();
		QuestLegacyObservationStore store = new QuestLegacyObservationStore();
		QuestLegacyInvocationBridge bridge = new QuestLegacyInvocationBridge(observation -> {
			records.incrementAndGet();
			store.record(observation);
		});

		HandlerResult result = bridge.invoke(player, QUEST_ID, "USE_ITEM", QuestDispatchContract.FIRST_NON_UNKNOWN,
			() -> {
				player.getQuestStateList().getQuestState(QUEST_ID).setQuestVar(1);
				return HandlerResult.SUCCESS;
			}, (value, stateChanged, recorder) -> value == HandlerResult.SUCCESS
				? QuestRouteResult.HANDLED : QuestRouteResult.UNKNOWN);

		assertEquals(HandlerResult.SUCCESS, result);
		assertEquals(1, records.get());
		QuestLegacyInvocation invocation = store.snapshot().get(0);
		QuestShadowObservation.Owner owner = invocation.observation().owners().get(QUEST_ID);
		assertTrue(owner.conditionMatched());
		assertEquals(QuestStatus.START, owner.nextStatus());
		assertEquals(1, owner.nextPackedVariables());
		assertEquals(QuestRouteResult.HANDLED, owner.result());
	}

	@Test
	void protocolOnlySuccessWithoutStateUsesCanonicalUnacceptedProjection() throws Exception {
		Player player = emptyPlayer();
		QuestLegacyObservationStore store = new QuestLegacyObservationStore();
		QuestLegacyInvocationBridge bridge = new QuestLegacyInvocationBridge(store);

		boolean result = bridge.invoke(player, QUEST_ID, "TALK_TO_NPC", QuestDispatchContract.EXCLUSIVE,
			() -> {
				QuestLegacyObservationContext.afterCommitAction(QUEST_ID, new AfterCommitAction.CloseDialog());
				return true;
			}, (value, stateChanged, recorder) -> QuestRouteResult.HANDLED);

		assertTrue(result);
		QuestShadowObservation.Owner owner = store.snapshot().get(0).observation().owners().get(QUEST_ID);
		assertTrue(owner.conditionMatched());
		assertEquals(QuestStatus.NONE, owner.nextStatus());
		assertEquals(0, owner.nextPackedVariables());
		assertEquals(QuestRouteResult.HANDLED, owner.result());
		assertEquals(1, owner.afterCommit().size());
	}

	@Test
	void unchangedStateProjectionDoesNotTurnUnhandledRouteIntoConditionMatch() throws Exception {
		Player player = playerWithState(QuestStatus.COMPLETE, 0);
		QuestLegacyObservationStore store = new QuestLegacyObservationStore();
		QuestLegacyInvocationBridge bridge = new QuestLegacyInvocationBridge(store);

		boolean result = bridge.invoke(player, QUEST_ID, "TALK_TO_NPC", QuestDispatchContract.EXCLUSIVE,
			() -> false,
			(value, stateChanged, recorder) -> QuestRouteResult.NOT_HANDLED);

		assertTrue(!result);
		QuestShadowObservation.Owner owner = store.snapshot().get(0).observation().owners().get(QUEST_ID);
		assertTrue(!owner.conditionMatched());
		assertEquals(QuestStatus.COMPLETE, owner.nextStatus());
		assertEquals(0, owner.nextPackedVariables());
		assertEquals(QuestRouteResult.NOT_HANDLED, owner.result());
	}

	@Test
	void sharedLegacyDialogHelpersRecordTheirActualTypedProtocolShapes() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestLegacyObservationStore store = new QuestLegacyObservationStore();
		QuestLegacyInvocationBridge bridge = new QuestLegacyInvocationBridge(store);
		QuestHandler handler = new DialogHandler();
		QuestEnv env = new QuestEnv(null, player, QUEST_ID, 31);

		boolean result = bridge.invoke(player, QUEST_ID, "TALK_TO_NPC", QuestDispatchContract.EXCLUSIVE,
			() -> {
				handler.sendQuestDialog(env, 1011);
				handler.sendQuestSelectionDialog(env);
				handler.closeDialogWindow(env);
				return true;
			}, (value, stateChanged, recorder) -> QuestRouteResult.HANDLED);

		assertTrue(result);
		QuestShadowObservation.Owner owner = store.snapshot().get(0).observation().owners().get(QUEST_ID);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011),
			new AfterCommitAction.ShowQuestSelectionDialog(10), new AfterCommitAction.CloseDialog()),
			owner.afterCommit());
	}

	private static final class DialogHandler extends QuestHandler {
		private DialogHandler() {
			super(QUEST_ID);
		}

		@Override
		public void register() {
		}
	}

	private static Player playerWithState(QuestStatus status, int packedVariables) throws Exception {
		Player player = emptyPlayer();
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		QuestStateList states = new QuestStateList();
		states.addQuest(QUEST_ID, new QuestState(QUEST_ID, status, packedVariables, 0,
				(Timestamp) null, null, null));
		setField(Player.class, player, "questStateList", states);
		return player;
	}

	private static Player emptyPlayer() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "questStateList", new QuestStateList());
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
