package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objenesis.ObjenesisStd;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader.SimpleTalkRow;

/**
 * 原生 SimpleTalk 车道测试夹具：真端行取数、假背包端口、Objenesis 玩家/NPC、对话窗口包捕获。
 * <p>
 * 只服务 P3 重锚（计划 §8.9）后的断言面：断言值全部来自真端表行 + 静态数据 id + 客户端页契约，
 * 夹具不合成语义、不改写行数据。
 * <p>
 * Test fixture for the native SimpleTalk lane: retail row accessors, a recording inventory port,
 * Objenesis player/NPC builders and dialog-window packet capture. Assertions must stay sourced from
 * the retail rows, static data ids and the client page contract; the fixture synthesizes nothing.
 */
public final class NativeTalkFixture {

	private NativeTalkFixture() {
	}

	/** 生产处理器（真端表行 + 生产背包端口）。 / The production handler. */
	public static SimpleTalkHandler handler() {
		return SimpleTalkHandler.instance();
	}

	/** 注入假背包端口的处理器（物品通道断言用）。 / A handler with the given inventory port. */
	public static SimpleTalkHandler handler(NativeInventoryPort inventory) {
		try {
			return new SimpleTalkHandler(NativeQuestTableLoader.instance(), NativeNpcNameResolver.instance(),
				RetailItemNameIndex.loadItemTemplates(), NativeQuestXmlTable.instance(), inventory);
		} catch (java.io.IOException e) {
			throw new AssertionError("retail item index unreadable", e);
		}
	}

	/** 注入假背包端口与完成口的处理器（领奖段门禁用）。 /
	 * A handler with the given inventory and completion ports (used by the claim-segment gates). */
	public static SimpleTalkHandler handler(NativeInventoryPort inventory, NativeReportRewardFlow rewardFlow) {
		try {
			return new SimpleTalkHandler(NativeQuestTableLoader.instance(), NativeNpcNameResolver.instance(),
				RetailItemNameIndex.loadItemTemplates(), NativeQuestXmlTable.instance(), inventory,
				NativeQuestOwnerResolver.instance().xmlOnlyIds(), NativeMoviePort.live(), rewardFlow);
		} catch (java.io.IOException e) {
			throw new AssertionError("retail item index unreadable", e);
		}
	}

	/** 真端行（缺行 fail-closed）。 / The retail row (missing rows fail closed). */
	public static SimpleTalkRow row(int questId) {
		return handler().requireRow(questId);
	}

	/**
	 * 真端表车道的接取入口页（信页优先；页 4 只由页动作 1007 打开）。
	 * The native-lane accept entry page (letter page first; page 4 is 1007-only).
	 */
	public static int clientEntryPage(int questId) {
		return QuestDialogContract.loadDefault().retailEntryPage(questId);
	}

	/** 页动作 1007(ASK_QUEST_ACCEPT) 的目标页（客户端未声明即 -1）。 / The ask window page for action 1007. */
	public static int askWindowPage(int questId) {
		return QuestDialogContract.loadDefault().askWindowPage(questId);
	}

	/** 客户端任务页是否声明该页。 / Whether the client task HTML declares the page. */
	public static boolean clientDeclares(int questId, int pageId) {
		return QuestDialogContract.loadDefault().hasButtonPage(questId, pageId);
	}

	/** 对话环境。 / A dialog environment. */
	public static QuestEnv dialog(Player player, int npcId, int questId, int dialogId) {
		return new QuestEnv(npc(npcId), player, questId, dialogId);
	}

	/** 建 START 态任务行。 / Seeds a START-state quest row. */
	public static QuestState start(Player player, int questId) {
		return add(player, questId, QuestStatus.START, 0);
	}

	/** 建指定状态的任务行。 / Seeds a quest row in the given status. */
	public static QuestState add(Player player, int questId, QuestStatus status, int vars) {
		QuestState state = new QuestState(questId, status, vars, 0, null, 0, null);
		player.getQuestStateList().addQuest(questId, state);
		return state;
	}

	/** 把前置任务标记为已完成。 / Marks the prerequisite quests complete. */
	public static void completePrerequisites(Player player, int... questIds) {
		for (int questId : questIds) {
			QuestState state = new QuestState(questId, QuestStatus.COMPLETE, 0, 0, null, 0, null);
			state.setCompleteCount(1);
			player.getQuestStateList().addQuest(questId, state);
		}
	}

	/** 测试玩家（默认天族战士 20 级，带包捕获连接）。 / The test player (Elyos warrior level 20, packet capture on). */
	public static Player player() {
		return player(Race.ELYOS, PlayerClass.WARRIOR, 20);
	}

	/** 参数化测试玩家（种族/职业/等级）。 / A parameterized test player. */
	public static Player player(Race race, PlayerClass playerClass, int level) {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", 10001);
		PlayerCommonData commonData = new PlayerCommonData(10001);
		commonData.setRace(race);
		commonData.setGender(Gender.MALE);
		// setPlayerClass/setLevel 需要经验表就绪（单测无服务栈）⇒ 直接写字段。
		// setPlayerClass/setLevel need the exp table (no service stack in unit tests) ⇒ write fields directly.
		setField(PlayerCommonData.class, commonData, "playerClass", playerClass);
		setField(PlayerCommonData.class, commonData, "level", level);
		setField(Player.class, player, "playerCommonData", commonData);
		setField(Player.class, player, "questStateList", new QuestStateList());
		setField(Player.class, player, "clientConnection", packetConnection());
		return player;
	}

	/** 对话窗口页序列（最近一次清空之后）。 / Dialog-window pages sent since the last clear. */
	public static List<Integer> dialogPages(Player player) {
		List<Integer> pages = new ArrayList<>();
		for (AionServerPacket packet : packets(player)) {
			if (packet instanceof SM_DIALOG_WINDOW dialog) {
				pages.add(intField(SM_DIALOG_WINDOW.class, dialog, "dialogID"));
			}
		}
		return pages;
	}

	/** 清空包队列。 / Clears the packet queue. */
	public static void clearPackets(Player player) {
		packets(player).clear();
	}

	/** 断言收到唯一一页。 / Asserts exactly one dialog page was sent. */
	public static void assertOnlyDialogPage(Player player, int expected) {
		assertEquals(List.of(expected), dialogPages(player), "下发的对话页");
	}

	/** NPC 桩（按真端 npc_id 建模板）。 / An NPC stub carrying the retail npc id. */
	public static Npc npc(int npcId) {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		setField(AionObject.class, npc, "objectId", 9999000 + npcId);
		NpcTemplate template = new NpcTemplate();
		setField(NpcTemplate.class, template, "npcId", npcId);
		setField(VisibleObject.class, npc, "objectTemplate", template);
		return npc;
	}

	/** 记录式假背包端口。 / A recording fake inventory port. */
	public static final class RecordingInventory implements NativeInventoryPort {
		/** 只读空实现（不需要物品通道的用例）。 / A read-only empty port for cases without item traffic. */
		public static final RecordingInventory EMPTY = new RecordingInventory();

		private final Map<Integer, Long> held = new LinkedHashMap<>();
		private final List<String> calls = new ArrayList<>();

		/** 授予（测试夹具直接持有）。 / Seed a hold. */
		public void hold(int itemId, long count) {
			held.put(itemId, count);
		}

		/** 清零（持有量与调用记录）。 / Clears holds and calls. */
		public void clear() {
			held.clear();
			calls.clear();
		}

		/** 物品通道调用记录（{@code give:id:count} / {@code remove:id:count}）。 / Item channel calls. */
		public List<String> calls() {
			return List.copyOf(calls);
		}

		@Override
		public long count(com.aionemu.gameserver.model.gameobjects.player.Player player, int itemId) {
			return held.getOrDefault(itemId, 0L);
		}

		@Override
		public void give(com.aionemu.gameserver.model.gameobjects.player.Player player, int itemId, int count) {
			calls.add("give:" + itemId + ":" + count);
			held.merge(itemId, (long) count, Long::sum);
		}

		@Override
		public boolean remove(com.aionemu.gameserver.model.gameobjects.player.Player player, int itemId, int count) {
			calls.add("remove:" + itemId + ":" + count);
			long current = held.getOrDefault(itemId, 0L);
			if (current < count) {
				return false;
			}
			held.put(itemId, current - count);
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<AionServerPacket> packets(Player player) {
		AionConnection connection = player.getClientConnection();
		return (List<AionServerPacket>) readField(AionConnection.class, connection, "sendMsgQueue");
	}

	private static AionConnection packetConnection() {
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		setField(AConnection.class, connection, "transport", new RecordingTransport());
		setField(AConnection.class, connection, "guard", new Object());
		setField(AionConnection.class, connection, "sendMsgQueue", new ArrayList<AionServerPacket>());
		return connection;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) {
		try {
			Field field = declaringClass.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static Object readField(Class<?> declaringClass, Object target, String name) {
		try {
			Field field = declaringClass.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(target);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static int intField(Class<?> declaringClass, Object target, String name) {
		try {
			Field field = declaringClass.getDeclaredField(name);
			field.setAccessible(true);
			return field.getInt(target);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** 只记录写入兴趣的传输（包直接进队列）。 / A transport that only records write interest. */
	private static final class RecordingTransport implements ConnectionTransport {

		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		@Override
		public void enableWriteInterest() {
			// 单测无事件循环：包已入队，写兴趣是空操作。 / No event loop in tests: the packet is queued already.
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
