package com.aionemu.gameserver.services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.controllers.PlayerController;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_COMPLETED_LIST;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 简易二转完成任务（1006/2008）的客户端包序列守卫，口径逐字对齐 GiveStigma 家族：完成 = action2
 * （UPDATE status=COMPLETE）+ action3（把该行移入已完成列表，与 {@link SM_QUEST_COMPLETED_LIST} 成对，
 * 该行随后被翻转，列表必须携带翻转后的行）；无行时先 action1 插行再翻转。
 * 缺列表 ⇒ 完成行滞留活动列表直到重登；只发 action3 ⇒ 客户端弹「任务失败」（真机 80788）。
 * <p>
 * Completion packet guard for the simple 2nd-class turn-in (1006/2008), mirroring the GiveStigma
 * family: completion = action2 (UPDATE status=COMPLETE) + action3 paired with
 * {@link SM_QUEST_COMPLETED_LIST} (the row is flipped before the list is built); a missing row is
 * inserted with action1 first. A missing list leaves the row in the active list; action3 alone
 * pops "quest failed" (live client 80788).
 */
class ClassChangeServiceCompletionPacketTest {
	private static final int QUEST_ID = 1006;

	/** 无行路径：先 action1 插行、再 action2 翻转，三件套两遍收尾（GiveStigma 家族同形）。 /
	 * No-row path: action1 inserts, action2 flips, the trio tail runs twice (the GiveStigma shape). */
	@Test
	void completingWithoutARowInsertsThenFlipsWithTheTrio() throws Exception {
		Player player = playerWithController();

		completeQuest(player, QUEST_ID);

		assertEquals(List.of("a1", "a2", "a3", "list", "a3", "list"), tokens(player));
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.COMPLETE, state.getStatus());
		assertEquals(1, state.getCompleteCount());
	}

	/** 已有行路径：action2 翻转，action3 与已完成列表成对，列表携带翻转后的行。 /
	 * Existing-row path: action2 flips, action3 pairs with the completed list, which carries the flipped row. */
	@Test
	void completingAnExistingRowFlipsWithTheTrio() throws Exception {
		Player player = playerWithController();
		player.getQuestStateList().addQuest(QUEST_ID,
			new QuestState(QUEST_ID, QuestStatus.REWARD, 0, 0, null, 0, null));

		completeQuest(player, QUEST_ID);

		assertEquals(List.of("a2", "a3", "list"), tokens(player));
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.COMPLETE, state.getStatus());
		assertEquals(1, state.getCompleteCount());
		List<QuestState> completed = completedList(player);
		assertNotNull(completed, "完成必须发 SM_QUEST_COMPLETED_LIST（与 action3 成对）");
		assertEquals(List.of(QUEST_ID), completed.stream().map(QuestState::getQuestId).toList());
		assertEquals(1, completed.getFirst().getCompleteCount());
	}

	private static void completeQuest(Player player, int questId) throws Exception {
		Method method = ClassChangeService.class.getDeclaredMethod("completeQuest", Player.class, int.class);
		method.setAccessible(true);
		method.invoke(null, player, questId);
	}

	private static List<String> tokens(Player player) throws Exception {
		List<String> tokens = new ArrayList<>();
		for (AionServerPacket packet : packetQueue(player)) {
			if (packet instanceof SM_QUEST_ACTION action) {
				tokens.add("a" + intField(action, "action"));
			} else if (packet instanceof SM_QUEST_COMPLETED_LIST) {
				tokens.add("list");
			}
		}
		return tokens;
	}

	@SuppressWarnings("unchecked")
	private static List<QuestState> completedList(Player player) throws Exception {
		List<QuestState> rows = null;
		for (AionServerPacket packet : packetQueue(player)) {
			if (packet instanceof SM_QUEST_COMPLETED_LIST) {
				Field field = SM_QUEST_COMPLETED_LIST.class.getDeclaredField("allQuests");
				field.setAccessible(true);
				rows = (List<QuestState>) field.get(packet);
			}
		}
		return rows;
	}

	private static Player playerWithController() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", 10001);
		setField(Player.class, player, "questStateList", new QuestStateList());
		setField(Player.class, player, "clientConnection", packetConnection());
		CountingController controller = new CountingController();
		setField(VisibleObject.class, player, "controller", controller);
		controller.setOwner(player);
		return player;
	}

	/** 只记刷新次数的控制器：附近任务刷新是可见性轴（0x7f）的尾巴。 / A refresh-counting controller: the nearby refresh is the visibility axis (0x7f) tail. */
	private static final class CountingController extends PlayerController {
		private int refreshes;

		@Override
		public void updateNearbyQuests() {
			refreshes++;
		}
	}

	private static AionConnection packetConnection() throws Exception {
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		setField(AConnection.class, connection, "transport", new RecordingTransport());
		setField(AConnection.class, connection, "guard", new Object());
		setField(AionConnection.class, connection, "sendMsgQueue", new ArrayList<AionServerPacket>());
		return connection;
	}

	@SuppressWarnings("unchecked")
	private static List<AionServerPacket> packetQueue(Player player) throws Exception {
		Field field = AionConnection.class.getDeclaredField("sendMsgQueue");
		field.setAccessible(true);
		return (List<AionServerPacket>) field.get(player.getClientConnection());
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value)
			throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static int intField(SM_QUEST_ACTION target, String name) throws Exception {
		Field field = SM_QUEST_ACTION.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(target);
	}

	/** 只记录写入兴趣的传输（包直接进队列）。 / A transport that only records write interest (packets queue directly). */
	private static final class RecordingTransport implements ConnectionTransport {

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
