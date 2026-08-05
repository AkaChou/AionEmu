package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.AbyssRankingResult;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.storage.ItemStorage;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real {@link QuestCurrencyPort}: kinah/AP/GP/DP rewards are applied to the live
 * player and each currency is persisted through its transactional DAO on the
 * caller-owned connection, so the rewards commit atomically with the quest state.
 */
class PlayerQuestCurrencyPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@Test
	void emptyCurrencyActionsDoNotRequireCapturedBalances() throws Exception {
		PlayerQuestCurrencyPort port = port(playerId -> null, new RecordingInventoryDao(),
			new RecordingAbyssRankDao(), new RecordingPlayerDao());
		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0,
			Map.of(), null, true, false, 0);

		port.preflight(connection(), snapshot, List.of());
		port.apply(connection(), snapshot, List.of());
		port.preflightDebits(connection(), snapshot, List.of());
		port.preflightSets(connection(), snapshot, List.of());
	}

	@Test
	void grantFailsClosedBeforeMutationWhenLiveApWouldOverflow() throws Exception {
		Player player = playerWithCurrency(0, 0, 0, 0);
		setField(AbyssRank.class, player.getAbyssRank(), "currentAp", Integer.MAX_VALUE);
		PlayerQuestCurrencyPort port = port(playerId -> player, new RecordingInventoryDao(),
			new RecordingAbyssRankDao(), new RecordingPlayerDao());

		assertThrows(SQLException.class,
			() -> port.apply(connection(), snapshotCaptured(), List.of(reward("AP", 1))));
		assertEquals(Integer.MAX_VALUE, player.getAbyssRank().getAp());
	}

	@Test
	void preflightFailsWhenCurrencyFactsWereNotCaptured() throws Exception {
		PlayerQuestCurrencyPort port = port(playerId -> null, new RecordingInventoryDao(),
			new RecordingAbyssRankDao(), new RecordingPlayerDao());
		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0,
			Map.of(), null, true, false, 0);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflight(connection(), snapshot, List.of(reward("GOLD", 500))));
		assertTrue(thrown.getMessage().contains("not captured"));
	}

	@Test
	void preflightFailsOnUnsupportedCurrencyKind() throws Exception {
		PlayerQuestCurrencyPort port = port(playerId -> null, new RecordingInventoryDao(),
			new RecordingAbyssRankDao(), new RecordingPlayerDao());
		QuestSnapshot snapshot = snapshotCaptured();

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflight(connection(), snapshot, List.of(reward("CP", 1))));
		assertTrue(thrown.getMessage().contains("no transactional currency store"));
	}

	@Test
	void preflightPassesForSupportedCurrencyKinds() throws Exception {
		PlayerQuestCurrencyPort port = port(playerId -> null, new RecordingInventoryDao(),
			new RecordingAbyssRankDao(), new RecordingPlayerDao());
		port.preflight(connection(), snapshotCaptured(),
			List.of(reward("GOLD", 500), reward("AP", 100), reward("GP", 50), reward("DP", 20)));
	}

	@Test
	void preflightDebitsRejectsInsufficientCapturedBalance() throws Exception {
		PlayerQuestCurrencyPort port = port(playerId -> null, new RecordingInventoryDao(),
			new RecordingAbyssRankDao(), new RecordingPlayerDao());
		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0,
			Map.of(), Map.of(QuestRewardKind.GOLD, 100L));

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflightDebits(connection(), snapshot,
				List.of(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 60),
					new QuestAction.DecreaseCurrency(QuestRewardKind.KINAH, 60))));
		assertTrue(thrown.getMessage().contains("insufficient"));
	}

	@Test
	void applyGrantsKinahAndPersistsOnCallerConnection() throws Exception {
		Player player = playerWithCurrency(0, 0, 0, 0);
		RecordingInventoryDao inventoryDao = new RecordingInventoryDao();
		PlayerQuestCurrencyPort port = port(playerId -> player, inventoryDao,
			new RecordingAbyssRankDao(), new RecordingPlayerDao());
		Connection connection = connection();

		QuestTransactionParticipant participant = port.apply(
			connection, snapshotCaptured(), List.of(reward("GOLD", 500)));

		assertEquals(500, player.getInventory().getKinah());
		assertEquals(1, inventoryDao.transactions.size());
		assertSame(connection, inventoryDao.transactions.get(0).connection);
		assertTrue(inventoryDao.transactions.get(0).items.stream()
			.anyMatch(item -> item.getItemTemplate().isKinah()));
		assertEquals(PersistentState.UPDATE_REQUIRED, player.getInventory().getPersistentState());
		participant.afterCommit();
		assertEquals(PersistentState.UPDATED, player.getInventory().getPersistentState());
	}

	@Test
	void applyGrantsApAndGpAndPersistsAbyssRankOnCallerConnection() throws Exception {
		Player player = playerWithCurrency(0, 0, 0, 0);
		RecordingAbyssRankDao abyssRankDao = new RecordingAbyssRankDao();
		PlayerQuestCurrencyPort port = port(playerId -> player, new RecordingInventoryDao(),
			abyssRankDao, new RecordingPlayerDao());
		Connection connection = connection();

		QuestTransactionParticipant participant = port.apply(connection, snapshotCaptured(),
			List.of(reward("AP", 100), reward("GP", 50)));

		AbyssRank rank = player.getAbyssRank();
		assertEquals(100, rank.getAp());
		assertEquals(50, rank.getGp());
		assertEquals(1, abyssRankDao.calls.size());
		assertSame(connection, abyssRankDao.calls.get(0));
		assertEquals(PLAYER_ID, abyssRankDao.lastPlayerId);
		assertEquals(PersistentState.UPDATE_REQUIRED, rank.getPersistentState());
		participant.afterCommit();
		// 已持久化:下一次 store 不再重复写
		assertEquals(PersistentState.UPDATED, rank.getPersistentState());
	}

	@Test
	void rollbackRestoresKinahApGpAndDp() throws Exception {
		Player player = playerWithCurrency(10, 20, 30, 40);
		PlayerQuestCurrencyPort port = port(playerId -> player, new RecordingInventoryDao(),
			new RecordingAbyssRankDao(), new RecordingPlayerDao());

		QuestTransactionParticipant participant = port.apply(connection(), snapshotCaptured(),
			List.of(reward("GOLD", 500), reward("AP", 100), reward("GP", 50), reward("DP", 20)));
		assertEquals(540, player.getInventory().getKinah());
		assertEquals(110, player.getAbyssRank().getAp());
		assertEquals(70, player.getAbyssRank().getGp());
		assertEquals(50, player.getCommonData().getDp());

		participant.afterRollback();

		assertEquals(40, player.getInventory().getKinah());
		assertEquals(10, player.getAbyssRank().getAp());
		assertEquals(20, player.getAbyssRank().getGp());
		assertEquals(30, player.getCommonData().getDp());
		assertEquals(PersistentState.UPDATED, player.getInventory().getPersistentState());
	}

	@Test
	void applyGrantsDpAndPersistsCommonDataOnCallerConnection() throws Exception {
		Player player = playerWithCurrency(0, 0, 0, 0);
		RecordingPlayerDao playerDao = new RecordingPlayerDao();
		PlayerQuestCurrencyPort port = port(playerId -> player, new RecordingInventoryDao(),
			new RecordingAbyssRankDao(), playerDao);
		Connection connection = connection();

		port.apply(connection, snapshotCaptured(), List.of(reward("DP", 20)));

		assertEquals(20, player.getCommonData().getDp());
		assertEquals(1, playerDao.calls.size());
		assertSame(connection, playerDao.calls.get(0));
		assertEquals(PLAYER_ID, playerDao.lastPlayerId);
		assertSame(player.getCommonData(), playerDao.lastPcd);
	}

	@Test
	void applyFailsWhenPlayerIsUnavailableBeforeAnyDaoWrite() throws Exception {
		RecordingInventoryDao inventoryDao = new RecordingInventoryDao();
		RecordingAbyssRankDao abyssRankDao = new RecordingAbyssRankDao();
		RecordingPlayerDao playerDao = new RecordingPlayerDao();
		PlayerQuestCurrencyPort port = port(playerId -> null, inventoryDao, abyssRankDao, playerDao);

		assertThrows(SQLException.class,
			() -> port.apply(connection(), snapshotCaptured(), List.of(reward("AP", 100))));
		assertEquals(0, inventoryDao.transactions.size());
		assertEquals(0, abyssRankDao.calls.size());
		assertEquals(0, playerDao.calls.size());
	}

	@Test
	void applyDebitsKinahPersistsAndRollbackRestoresTheLiveBalance() throws Exception {
		Player player = playerWithCurrency(0, 0, 0, 100);
		RecordingInventoryDao inventoryDao = new RecordingInventoryDao();
		PlayerQuestCurrencyPort port = port(playerId -> player, inventoryDao,
			new RecordingAbyssRankDao(), new RecordingPlayerDao());
		Connection connection = connection();
		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0,
			Map.of(), Map.of(QuestRewardKind.GOLD, 100L));

		QuestTransactionParticipant participant = port.applyDebits(connection, snapshot,
			List.of(new QuestAction.DecreaseCurrency(QuestRewardKind.KINAH, 40)));

		assertEquals(60, player.getInventory().getKinah());
		assertEquals(1, inventoryDao.transactions.size());
		assertSame(connection, inventoryDao.transactions.get(0).connection);
		assertEquals(PersistentState.UPDATE_REQUIRED, player.getInventory().getPersistentState());

		participant.afterCommit();
		assertEquals(PersistentState.UPDATED, player.getInventory().getPersistentState());
		participant.afterRollback();
		assertEquals(100, player.getInventory().getKinah());
		assertEquals(PersistentState.UPDATED, player.getInventory().getPersistentState());
	}

	private static PlayerQuestCurrencyPort port(QuestPlayerPort players, InventoryDAO inventoryDao,
			AbyssRankDAO abyssRankDao, PlayerDAO playerDao) {
		return new PlayerQuestCurrencyPort(players, inventoryDao, abyssRankDao, playerDao, item -> { });
	}

	private static QuestSnapshot snapshotCaptured() {
		return new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of(), Map.of());
	}

	private static QuestAction.GrantReward reward(String kind, int amount) {
		return new QuestAction.GrantReward(kind, 0, amount);
	}

	private static Player playerWithCurrency(int ap, int gp, int dp, long kinah) throws Exception {
		Player player = emptyPlayer();
		PlayerCommonData pcd = new ObjenesisStd().newInstance(PlayerCommonData.class);
		setField(PlayerCommonData.class, pcd, "playerClass", PlayerClass.GLADIATOR);
		setField(PlayerCommonData.class, pcd, "dp", dp);
		setField(Player.class, player, "playerCommonData", pcd);
		AbyssRank rank = new ObjenesisStd().newInstance(AbyssRank.class);
		setField(AbyssRank.class, rank, "currentAp", ap);
		setField(AbyssRank.class, rank, "currentGp", gp);
		setField(AbyssRank.class, rank, "rank", AbyssRankEnum.GRADE9_SOLDIER);
		setField(AbyssRank.class, rank, "persistentState", PersistentState.UPDATED);
		setField(Player.class, player, "abyssRank", rank);
		PlayerStorage inventory = (PlayerStorage) player.getInventory();
		setField(Storage.class, inventory, "kinahItem", kinahItem(kinah));
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

	private static Item kinahItem(long count) throws Exception {
		ItemTemplate template = new ObjenesisStd().newInstance(ItemTemplate.class);
		setField(ItemTemplate.class, template, "itemId", 182400001);
		Item item = new ObjenesisStd().newInstance(Item.class);
		setField(Item.class, item, "itemTemplate", template);
		setField(Item.class, item, "itemCount", count);
		setField(Item.class, item, "persistentState", PersistentState.UPDATED);
		setField(AionObject.class, item, "objectId", 1);
		return item;
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

	private static final class RecordingInventoryDao extends InventoryDAO {
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

	private static final class RecordingAbyssRankDao extends AbyssRankDAO {
		private final List<Connection> calls = new ArrayList<>();
		private int lastPlayerId;
		private AbyssRank lastRank;

		@Override
		public void storeInTransaction(Connection connection, int playerId, AbyssRank rank) {
			calls.add(connection);
			lastPlayerId = playerId;
			lastRank = rank;
		}

		@Override
		public void loadAbyssRank(Player player) {
			throw new AssertionError("unexpected loadAbyssRank");
		}

		@Override
		public AbyssRank loadAbyssRank(int playerId) {
			throw new AssertionError("unexpected loadAbyssRank");
		}

		@Override
		public boolean storeAbyssRank(Player player) {
			throw new AssertionError("unexpected storeAbyssRank");
		}

		@Override
		public ArrayList<AbyssRankingResult> getAbyssRankingPlayers(Race race) {
			throw new AssertionError("unexpected getAbyssRankingPlayers");
		}

		@Override
		public ArrayList<AbyssRankingResult> getAbyssRankingLegions(Race race) {
			throw new AssertionError("unexpected getAbyssRankingLegions");
		}

		@Override
		public Map<Integer, Integer> loadPlayersAp(Race race, int lowerApLimit, int maxOfflineDays) {
			throw new AssertionError("unexpected loadPlayersAp");
		}

		@Override
		public Map<Integer, Integer> loadPlayersGp(Race race, int lowerGpLimit, int maxOfflineDays) {
			throw new AssertionError("unexpected loadPlayersGp");
		}

		@Override
		public void updateAbyssRank(int playerId, AbyssRankEnum rankEnum) {
			throw new AssertionError("unexpected updateAbyssRank");
		}

		@Override
		public void updateRankList() {
			throw new AssertionError("unexpected updateRankList");
		}

		@Override
		public void removePlayer(List<Player> listP) {
			throw new AssertionError("unexpected removePlayer");
		}

		@Override
		public boolean supports(String databaseName, int majorVersion, int minorVersion) {
			return false;
		}
	}

	private static final class RecordingPlayerDao extends PlayerDAO {
		private final List<Connection> calls = new ArrayList<>();
		private int lastPlayerId;
		private PlayerCommonData lastPcd;

		@Override
		public void storeInTransaction(Connection connection, int playerId, PlayerCommonData pcd) {
			calls.add(connection);
			lastPlayerId = playerId;
			lastPcd = pcd;
		}

		@Override
		public boolean isNameUsed(String name) {
			throw new AssertionError("unexpected isNameUsed");
		}

		@Override
		public Map<Integer, String> getPlayerNames(Collection<Integer> playerObjectIds) {
			throw new AssertionError("unexpected getPlayerNames");
		}

		@Override
		public void storePlayer(Player player) {
			throw new AssertionError("unexpected storePlayer");
		}

		@Override
		public boolean saveNewPlayer(PlayerCommonData pcd, int accountId, String accountName) {
			throw new AssertionError("unexpected saveNewPlayer");
		}

		@Override
		public PlayerCommonData loadPlayerCommonData(int playerObjId) {
			throw new AssertionError("unexpected loadPlayerCommonData");
		}

		@Override
		public void deletePlayer(int playerId) {
			throw new AssertionError("unexpected deletePlayer");
		}

		@Override
		public void updateDeletionTime(int objectId, Timestamp deletionDate) {
			throw new AssertionError("unexpected updateDeletionTime");
		}

		@Override
		public void storeCreationTime(int objectId, Timestamp creationDate) {
			throw new AssertionError("unexpected storeCreationTime");
		}

		@Override
		public void setCreationDeletionTime(com.aionemu.gameserver.model.account.PlayerAccountData acData) {
			throw new AssertionError("unexpected setCreationDeletionTime");
		}

		@Override
		public List<Integer> getPlayerOidsOnAccount(int accountId) {
			throw new AssertionError("unexpected getPlayerOidsOnAccount");
		}

		@Override
		public void storeLastOnlineTime(int objectId, Timestamp lastOnline) {
			throw new AssertionError("unexpected storeLastOnlineTime");
		}

		@Override
		public void onlinePlayer(Player player, boolean online) {
			throw new AssertionError("unexpected onlinePlayer");
		}

		@Override
		public void setPlayersOffline(boolean online) {
			throw new AssertionError("unexpected setPlayersOffline");
		}

		@Override
		public PlayerCommonData loadPlayerCommonDataByName(String name) {
			throw new AssertionError("unexpected loadPlayerCommonDataByName");
		}

		@Override
		public int getAccountIdByName(String name) {
			throw new AssertionError("unexpected getAccountIdByName");
		}

		@Override
		public String getPlayerNameByObjId(int playerObjId) {
			throw new AssertionError("unexpected getPlayerNameByObjId");
		}

		@Override
		public int getPlayerIdByName(String playerName) {
			throw new AssertionError("unexpected getPlayerIdByName");
		}

		@Override
		public void storePlayerName(PlayerCommonData recipientCommonData) {
			throw new AssertionError("unexpected storePlayerName");
		}

		@Override
		public int getCharacterCountOnAccount(int accountId) {
			throw new AssertionError("unexpected getCharacterCountOnAccount");
		}

		@Override
		public int getCharacterCountForRace(Race race) {
			throw new AssertionError("unexpected getCharacterCountForRace");
		}

		@Override
		public int getOnlinePlayerCount() {
			throw new AssertionError("unexpected getOnlinePlayerCount");
		}

		@Override
		public List<Integer> getPlayersToDelete(int paramInt1, int paramInt2) {
			throw new AssertionError("unexpected getPlayersToDelete");
		}

		@Override
		public void setPlayerLastTransferTime(int playerId, long time) {
			throw new AssertionError("unexpected setPlayerLastTransferTime");
		}

		@Override
		public Timestamp getCharacterCreationDateId(int obj) {
			throw new AssertionError("unexpected getCharacterCreationDateId");
		}

		@Override
		public void updateLegionJoinRequestState(int playerId,
				com.aionemu.gameserver.model.team.legion.LegionJoinRequestState state) {
			throw new AssertionError("unexpected updateLegionJoinRequestState");
		}

		@Override
		public void clearJoinRequest(int playerId) {
			throw new AssertionError("unexpected clearJoinRequest");
		}

		@Override
		public void getJoinRequestState(Player player) {
			throw new AssertionError("unexpected getJoinRequestState");
		}

		@Override
		public int getPlayerLunaConsumeByObjId(int playerObjId) {
			throw new AssertionError("unexpected getPlayerLunaConsumeByObjId");
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
