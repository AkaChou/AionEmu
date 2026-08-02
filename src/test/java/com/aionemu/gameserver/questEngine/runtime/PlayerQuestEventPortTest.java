package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real {@link QuestEventPort}: the pre-event player facts are frozen into a
 * snapshot that later phases read, so the whole event commit works on a single
 * consistent view of the player state.
 */
class PlayerQuestEventPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@Test
	void snapshotCapturesFrozenPlayerFacts() throws Exception {
		Player player = playerWithQuestState(QuestStatus.LOCKED, 0x42);
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID, event());

		assertEquals(PLAYER_ID, snapshot.playerId());
		assertEquals(QUEST_ID, snapshot.questId());
		assertEquals(QuestStatus.LOCKED, snapshot.status());
		assertEquals(0x42, snapshot.packedVariables());
		// 有背包即认为 inventory/currency 事实已捕获
		assertTrue(snapshot.inventoryCaptured());
		assertTrue(snapshot.currenciesCaptured());
	}

	@Test
	void snapshotCapturesNoneStateWhenQuestNotStarted() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID, event());

		assertEquals(QuestStatus.NONE, snapshot.status());
		assertEquals(0, snapshot.packedVariables());
	}

	@Test
	void talkSnapshotCarriesAuthoritativeInteractionObjectId() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID,
			new QuestEvent.TalkToNpc(203700, 1011, 900007));

		assertEquals(900007, snapshot.interactionObjectId());
	}

	@Test
	void snapshotFailsWhenPlayerIsUnavailable() {
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> null);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.snapshot(connection(), PLAYER_ID, QUEST_ID, event()));
		assertTrue(thrown.getMessage().contains("player is unavailable"));
	}

	private static QuestEvent event() {
		return new QuestEvent.TalkToNpc(203700);
	}

	private static Player playerWithQuestState(QuestStatus status, int packedVars) throws Exception {
		Player player = emptyPlayer();
		QuestState state = new QuestState(QUEST_ID, status, packedVars, 0, null, null, null);
		player.getQuestStateList().addQuest(QUEST_ID, state);
		return player;
	}

	private static Player emptyPlayer() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "questStateList", new QuestStateList());
		setField(Player.class, player, "equipment", new ObjenesisStd().newInstance(Equipment.class));
		setField(Player.class, player, "regularWarehouse", new PlayerStorage(StorageType.REGULAR_WAREHOUSE));
		setField(Player.class, player, "accountWarehouse", new PlayerStorage(StorageType.ACCOUNT_WAREHOUSE));
		setField(Player.class, player, "petBag",
			new Storage[StorageType.PET_BAG_MAX - StorageType.PET_BAG_MIN + 1]);
		setField(Player.class, player, "cabinets",
			new Storage[StorageType.HOUSE_WH_MAX - StorageType.HOUSE_WH_MIN + 1]);
		PlayerStorage inventory = new PlayerStorage(StorageType.CUBE);
		inventory.setOwner(player);
		setField(Player.class, player, "inventory", inventory);
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> false;
				case "toString" -> "test-connection";
				default -> defaultValue(method.getReturnType());
			});
	}

	private static Object defaultValue(Class<?> type) {
		if (!type.isPrimitive()) {
			return null;
		}
		if (type == boolean.class) {
			return false;
		}
		if (type == int.class || type == short.class || type == byte.class) {
			return 0;
		}
		if (type == long.class) {
			return 0L;
		}
		if (type == float.class) {
			return 0F;
		}
		if (type == double.class) {
			return 0D;
		}
		if (type == char.class) {
			return '\0';
		}
		return null;
	}
}
