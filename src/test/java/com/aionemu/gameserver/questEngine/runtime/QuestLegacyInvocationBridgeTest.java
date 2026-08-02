package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

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
	void protocolOnlySuccessWithoutStateIsRecordedAsUnmatched() throws Exception {
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
		assertTrue(!owner.conditionMatched());
		assertEquals(QuestRouteResult.HANDLED, owner.result());
		assertEquals(1, owner.afterCommit().size());
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
