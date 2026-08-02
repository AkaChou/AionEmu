package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.storage.ItemStorage;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.EvidenceRef;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.hasItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.removeItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;

/**
 * Real {@link QuestInventoryPort}: removals run against the live inventory and
 * the dirty items are persisted through {@link InventoryDAO#storeInTransaction}
 * on the caller-owned connection, so the removal commits atomically with the
 * quest state. Preflight fails closed on uncaptured or insufficient facts.
 */
class PlayerQuestInventoryPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;
	private static final int ITEM_A = 169001001;

	@Test
	void preflightFailsWhenFactsWereNotCaptured() throws Exception {
		PlayerQuestInventoryPort port = new PlayerQuestInventoryPort(playerId -> null, new RecordingDao());
		// inventory=null → inventoryCaptured=false,itemCount fail-closed
		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0,
			null, Map.of(), false, true, 0);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflight(connection(), snapshot, removals(ITEM_A, 1)));
		assertTrue(thrown.getMessage().contains("not captured"));
	}

	@Test
	void preflightFailsOnInsufficientKnownCount() throws Exception {
		PlayerQuestInventoryPort port = new PlayerQuestInventoryPort(playerId -> null, new RecordingDao());
		QuestSnapshot snapshot = snapshotWith(ITEM_A, 1);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflight(connection(), snapshot, removals(ITEM_A, 2)));
		assertTrue(thrown.getMessage().contains("insufficient"));
	}

	@Test
	void preflightPassesWhenKnownCountIsSufficient() throws Exception {
		PlayerQuestInventoryPort port = new PlayerQuestInventoryPort(playerId -> null, new RecordingDao());
		port.preflight(connection(), snapshotWith(ITEM_A, 5), removals(ITEM_A, 2));
	}

	@Test
	void applyRemovesItemsAndPersistsDirtyOnCallerConnection() throws Exception {
		Player player = playerWithInventory(ITEM_A, 5);
		RecordingDao dao = new RecordingDao();
		PlayerQuestInventoryPort port = new PlayerQuestInventoryPort(playerId -> player, dao);
		Connection connection = connection();

		QuestTransactionParticipant participant = port.apply(connection, snapshotWith(ITEM_A, 5), removals(ITEM_A, 2));

		// 真实从 live 背包移除
		assertEquals(3, player.getInventory().getItemCountByItemId(ITEM_A));
		// 通过调用方 Connection 持久化 dirty items,与任务状态同事务
		assertEquals(1, dao.transactions.size());
		assertSame(connection, dao.transactions.get(0).connection);
		assertTrue(dao.transactions.get(0).items.stream()
			.anyMatch(item -> item.getItemTemplate().getTemplateId() == ITEM_A));
		assertEquals(PersistentState.UPDATE_REQUIRED, player.getInventory().getPersistentState());
		participant.afterCommit();
		// 仅 commit 后清 dirty,下一次提交不重复写
		assertEquals(PersistentState.UPDATED, player.getInventory().getPersistentState());
	}

	@Test
	void rollbackRestoresRemovedItemAndDirtyState() throws Exception {
		Player player = playerWithInventory(ITEM_A, 5);
		PlayerQuestInventoryPort port = new PlayerQuestInventoryPort(playerId -> player, new RecordingDao());

		QuestTransactionParticipant participant = port.apply(
			connection(), snapshotWith(ITEM_A, 5), removals(ITEM_A, 2));
		assertEquals(3, player.getInventory().getItemCountByItemId(ITEM_A));

		participant.afterRollback();

		assertEquals(5, player.getInventory().getItemCountByItemId(ITEM_A));
		assertTrue(player.getInventory().getDeletedItems().isEmpty());
		assertEquals(PersistentState.UPDATED, player.getInventory().getPersistentState());
	}

	@Test
	void jdbcCommitFailureRestoresInventoryMutatedByRealPort() throws Exception {
		Player player = playerWithInventory(ITEM_A, 5);
		RecordingDao dao = new RecordingDao();
		PlayerQuestInventoryPort inventory = new PlayerQuestInventoryPort(playerId -> player, dao);
		QuestActionPort actions = new QuestActionPort() {
			@Override
			public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> required)
					throws SQLException {
				inventory.preflight(connection, snapshot,
					required.stream().filter(QuestAction.RemoveItem.class::isInstance)
						.map(QuestAction.RemoveItem.class::cast).toList());
			}

			@Override
			public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
					List<QuestAction> required) throws SQLException {
				return inventory.apply(connection, snapshot,
					required.stream().filter(QuestAction.RemoveItem.class::isInstance)
						.map(QuestAction.RemoveItem.class::cast).toList());
			}
		};
		CompiledQuestDefinition definition = quest(QUEST_ID)
			.evidence(new EvidenceRef("test", "real-port-commit-failure", "fixture"))
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).when(hasItem(ITEM_A, 5))
			.then(removeItem(ITEM_A, 2)).then(setVariable("step", 1)).goTo("reward").compile();
		List<String> jdbc = new ArrayList<>();

		assertThrows(SQLException.class, () -> new QuestExecutionCoordinator(new PlayerSerialExecutor()).execute(
			commitFailingConnection(jdbc), PLAYER_ID, definition, talkToNpc(700001),
			definition.definition().transitions().get(0),
			(connection, playerId, questId, event) -> snapshotWith(ITEM_A, 5), actions,
			new QuestStatePort() {
				@Override public void apply(Connection connection, int playerId, QuestMutationPlan plan) {
				}
				@Override public void publish(int playerId, QuestMutationPlan plan) {
					throw new AssertionError("publish must not run after failed commit");
				}
			}, (action, snapshot, plan) -> {
				throw new AssertionError("afterCommit must not run after failed commit");
			}));

		assertEquals(List.of("commit", "rollback"), jdbc);
		assertEquals(5, player.getInventory().getItemCountByItemId(ITEM_A));
		assertTrue(player.getInventory().getDeletedItems().isEmpty());
		assertEquals(PersistentState.UPDATED, player.getInventory().getPersistentState());
		assertEquals(1, dao.transactions.size());
	}

	@Test
	void applyFailsWhenPlayerIsUnavailableBeforeAnyDaoWrite() throws Exception {
		RecordingDao dao = new RecordingDao();
		PlayerQuestInventoryPort port = new PlayerQuestInventoryPort(playerId -> null, dao);

		assertThrows(SQLException.class,
			() -> port.apply(connection(), snapshotWith(ITEM_A, 5), removals(ITEM_A, 2)));
		assertEquals(0, dao.transactions.size());
	}

	@Test
	void applyFailsWhenLiveInventoryCannotSatisfyRemovalAndSkipsDao() throws Exception {
		// snapshot 声称有 5(preflight 通过),但 live 背包为空 → 移除失败
		Player player = playerWithInventory(); // empty
		RecordingDao dao = new RecordingDao();
		PlayerQuestInventoryPort port = new PlayerQuestInventoryPort(playerId -> player, dao);

		assertThrows(SQLException.class,
			() -> port.apply(connection(), snapshotWith(ITEM_A, 5), removals(ITEM_A, 2)));
		assertEquals(0, dao.transactions.size());
		// 失败不能留下部分脏标记
		assertEquals(PersistentState.UPDATED, player.getInventory().getPersistentState());
	}

	private static QuestSnapshot snapshotWith(int itemId, int count) {
		return new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of(itemId, count));
	}

	private static List<QuestAction.RemoveItem> removals(int itemId, int count) {
		return List.of(new QuestAction.RemoveItem(itemId, count));
	}

	private static Player playerWithInventory(int itemId, int count) throws Exception {
		Player player = emptyPlayer();
		PlayerStorage storage = storageWith(item(itemId, count));
		setField(Player.class, player, "inventory", storage);
		storage.setOwner(player);
		return player;
	}

	private static Player playerWithInventory() throws Exception {
		Player player = emptyPlayer();
		PlayerStorage storage = new PlayerStorage(StorageType.CUBE);
		storage.setOwner(player);
		setField(Player.class, player, "inventory", storage);
		return player;
	}

	private static Player emptyPlayer() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "questStateList", new QuestStateList());
		setField(Player.class, player, "equipment", new ObjenesisStd().newInstance(Equipment.class));
		// 生产环境 Player 加载后这些仓库必非 null;getDirtyItemsToUpdate 对它们未判空
		setField(Player.class, player, "regularWarehouse", new PlayerStorage(StorageType.REGULAR_WAREHOUSE));
		setField(Player.class, player, "accountWarehouse", new PlayerStorage(StorageType.ACCOUNT_WAREHOUSE));
		// Objenesis 绕过字段初始化器,petBag/cabinets 数组需显式补上
		setField(Player.class, player, "petBag",
			new Storage[StorageType.PET_BAG_MAX - StorageType.PET_BAG_MIN + 1]);
		setField(Player.class, player, "cabinets",
			new Storage[StorageType.HOUSE_WH_MAX - StorageType.HOUSE_WH_MIN + 1]);
		return player;
	}

	private static Item item(int itemId, int count) throws Exception {
		ItemTemplate template = new ObjenesisStd().newInstance(ItemTemplate.class);
		setField(ItemTemplate.class, template, "itemId", itemId);
		Item item = new ObjenesisStd().newInstance(Item.class);
		setField(Item.class, item, "itemTemplate", template);
		setField(Item.class, item, "itemCount", count);
		setField(Item.class, item, "persistentState", PersistentState.UPDATED);
		setField(AionObject.class, item, "objectId", itemId);
		return item;
	}

	private static PlayerStorage storageWith(Item item) throws Exception {
		ItemStorage itemStorage = new ObjenesisStd().newInstance(ItemStorage.class);
		Map<Integer, Item> slots = new LinkedHashMap<>();
		slots.put(item.getObjectId(), item);
		setField(ItemStorage.class, itemStorage, "items", slots);
		PlayerStorage storage = new PlayerStorage(StorageType.CUBE);
		setField(Storage.class, storage, "itemStorage", itemStorage);
		return storage;
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

	private static Connection commitFailingConnection(List<String> calls) {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> false;
				case "commit" -> {
					calls.add("commit");
					throw new SQLException("injected commit failure");
				}
				case "rollback" -> {
					calls.add("rollback");
					yield null;
				}
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

	private static final class RecordingDao extends InventoryDAO {
		private static final class Transaction {
			final Connection connection;
			final List<Item> items;

			Transaction(Connection connection, List<Item> items) {
				this.connection = connection;
				this.items = items;
			}
		}

		private final List<Transaction> transactions = new ArrayList<>();

		@Override
		public void storeInTransaction(Connection connection, List<Item> items, Integer playerId, Integer accountId,
				Integer legionId) {
			transactions.add(new Transaction(connection, items));
		}

		@Override
		public Storage loadStorage(int playerId, StorageType storageType) {
			throw new AssertionError("unexpected loadStorage");
		}

		@Override
		public List<Item> loadStorageDirect(int playerId, StorageType storageType) {
			throw new AssertionError("unexpected loadStorageDirect");
		}

		@Override
		public Equipment loadEquipment(Player player) {
			throw new AssertionError("unexpected loadEquipment");
		}

		@Override
		public List<Item> loadEquipment(int playerId) {
			throw new AssertionError("unexpected loadEquipment");
		}

		@Override
		public boolean store(Player player) {
			throw new AssertionError("unexpected store");
		}

		@Override
		public boolean store(Item item, Player player) {
			throw new AssertionError("unexpected store");
		}

		@Override
		public boolean store(List<Item> items, int playerId) {
			throw new AssertionError("unexpected store");
		}

		@Override
		public boolean store(List<Item> items, Integer playerId, Integer accountId, Integer legionId) {
			throw new AssertionError("unexpected store");
		}

		@Override
		public boolean deletePlayerItems(int playerId) {
			throw new AssertionError("unexpected deletePlayerItems");
		}

		@Override
		public void deleteAccountWH(int accountId) {
			throw new AssertionError("unexpected deleteAccountWH");
		}

		@Override
		public boolean supports(String databaseName, int majorVersion, int minorVersion) {
			return false;
		}

		@Override
		public int[] getUsedIDs() {
			return new int[0];
		}
	}
}
