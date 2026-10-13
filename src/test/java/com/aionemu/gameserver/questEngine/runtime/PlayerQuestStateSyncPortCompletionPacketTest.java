package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_COMPLETED_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 完成同步的包序列守卫：发 action2（UPDATE status=COMPLETE）+ action3（把该行移入已完成列表），
 * 且 action3 必须与 {@link SM_QUEST_COMPLETED_LIST} 成对。只发 action2 ⇒ 完成行留在活动列表，
 * 可重复任务下轮接取的 action1 累积成多行；只发 action3 ⇒ 客户端弹「任务失败」（真机 80788）。
 * <p>
 * Completion-sync packet guard: the UPDATE (action2) + action3 (move the row into the completed
 * list) + {@link SM_QUEST_COMPLETED_LIST}, with action3 always paired with the list. action2 alone
 * leaves the row in the active list (duplicate rows on re-accept); action3 alone pops "quest failed".
 */
class PlayerQuestStateSyncPortCompletionPacketTest {

	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 80788;

	@Test
	void completionSyncPairsTheAbandonActionWithTheCompletedList() throws Exception {
		Player player = player(QUEST_ID, QuestStatus.REWARD, 0);

		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.REWARD, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(QUEST_ID, QuestStatus.COMPLETE, 0,
			List.of(new QuestAction.CompleteQuest(0)), List.of());

		PlayerQuestStateSyncPort port = new PlayerQuestStateSyncPort(playerId -> player,
			id -> null, ignored -> null, ignored -> { }, ignored -> { }, ignored -> { });
		assertTrue(port.sync(snapshot, plan, QuestStateSyncMode.COMPLETION));

		List<AionServerPacket> sent = packetQueue(player.getClientConnection());

		List<SM_QUEST_ACTION> questActions = sent.stream()
			.filter(SM_QUEST_ACTION.class::isInstance)
			.map(SM_QUEST_ACTION.class::cast)
			.toList();
		assertEquals(2, questActions.size(), "完成同步发两个任务动作包（action2 UPDATE + action3）");
		assertEquals(2, intField(questActions.get(0), "action"), "先发 action2（UPDATE status=COMPLETE）");
		assertEquals(3, intField(questActions.get(1), "action"),
			"再发 action3（把该行移入已完成列表）——必须与已完成列表成对");
		assertTrue(sent.stream().anyMatch(SM_QUEST_COMPLETED_LIST.class::isInstance),
			"完成同步必须发 SM_QUEST_COMPLETED_LIST（与 action3 成对）");
	}

	private static Player player(int questId, QuestStatus status, int packedVariables) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		QuestStateList states = new QuestStateList();
		QuestState state = new QuestState(questId, status, packedVariables, 0, null, null, null);
		state.setPersistentState(PersistentState.UPDATED);
		states.addQuest(questId, state);
		setField(Player.class, player, "questStateList", states);
		setField(Player.class, player, "clientConnection", packetConnection());
		return player;
	}

	private static AionConnection packetConnection() throws Exception {
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		RecordingTransport transport = new RecordingTransport();
		transport.connection = connection;
		setField(AConnection.class, connection, "transport", transport);
		setField(AConnection.class, connection, "guard", new Object());
		setField(AionConnection.class, connection, "sendMsgQueue", new ArrayList<AionServerPacket>());
		return connection;
	}

	@SuppressWarnings("unchecked")
	private static List<AionServerPacket> packetQueue(AionConnection connection) throws Exception {
		Field field = AionConnection.class.getDeclaredField("sendMsgQueue");
		field.setAccessible(true);
		return (List<AionServerPacket>) field.get(connection);
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value)
			throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static int intField(Object target, String name) throws Exception {
		Field field = SM_QUEST_ACTION.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(target);
	}

	private static final class RecordingTransport implements ConnectionTransport {
		private AionConnection connection;

		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		@Override
		public void enableWriteInterest() {
		}

		@Override
		public void close(boolean forced) {
		}

		@Override
		public boolean onlyClose() {
			return true;
		}
	}
}
