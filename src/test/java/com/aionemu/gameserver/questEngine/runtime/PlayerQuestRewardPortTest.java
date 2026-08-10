package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerTitleListDAO;
import com.aionemu.gameserver.dataholders.TitleData;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.gameobjects.player.title.Title;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import com.aionemu.gameserver.model.items.storage.ItemStorage;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.TitleTemplate;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TITLE_INFO;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real {@link QuestRewardPort}: durable (non-currency) rewards are applied to the
 * live player and persisted through the transactional DAO on the caller-owned
 * connection, so rewards commit atomically with the quest state.
 *
 * <p>EXP 数值发放依赖 {@code DataManager.PLAYER_EXPERIENCE_TABLE}（单测不可用），
 * 因此 EXP apply 用例用 {@code noExp} 短路验证端口路径（调用 addExp + 持久化）；
 * 实际数值由 PlayerCommonData 承担。AURA_OF_GROWTH/EXP_BOOST 依赖 level>=66 且
 * {@code GSConfig.PLAYER_MAX_LEVEL} 高于 level,用例通过反射注入 level 与静态配置。</p>
 */
class PlayerQuestRewardPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;
	private static final int ITEM_A = 169001001;
	private static final int TITLE_ID = 7;

	private static TitleData originalTitleData;

	@BeforeAll
	static void configurePacketProcessor() {
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = 1;
	}

	@BeforeEach
	void setUpTitleData() throws Exception {
		originalTitleData = com.aionemu.gameserver.dataholders.DataManager.TITLE_DATA;
		com.aionemu.gameserver.dataholders.DataManager.TITLE_DATA = emptyTitleData();
	}

	@AfterEach
	void restoreTitleData() {
		com.aionemu.gameserver.dataholders.DataManager.TITLE_DATA = originalTitleData;
	}

	@Test
	void preflightFailsOnUnsupportedDurableKind() throws Exception {
		PlayerQuestRewardPort port = port(playerId -> null, new RecordingInventoryDao(),
			new RecordingPlayerDao(), (p, items) -> true);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflight(connection(), snapshot(), List.of(reward("EXTEND_INVENTORY", 0, 1))));
		assertTrue(thrown.getMessage().contains("no transactional durable reward store"));
	}

	@Test
	void preflightFailsOnItemRewardWithoutPositiveId() throws Exception {
		PlayerQuestRewardPort port = port(playerId -> null, new RecordingInventoryDao(),
			new RecordingPlayerDao(), (p, items) -> true);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflight(connection(), snapshot(), List.of(reward("ITEM", 0, 1))));
		assertTrue(thrown.getMessage().contains("without a positive item id"));
	}

	@Test
	void preflightPassesForSupportedDurableKinds() throws Exception {
		PlayerQuestRewardPort port = port(playerId -> null, new RecordingInventoryDao(),
			new RecordingPlayerDao(), (p, items) -> true);
		port.preflight(connection(), snapshot(),
			List.of(reward("ITEM", ITEM_A, 2), reward("EXP", 0, 1000),
				reward("EXP_BOOST", 0, 3), reward("AURA_OF_GROWTH", 0, 5000),
				reward("TITLE", TITLE_ID, 1), reward("RANDOM", 18505, 1)));
	}

	@Test
	void preflightFailsOnUnknownRandomRewardPool() throws Exception {
		PlayerQuestRewardPort port = new PlayerQuestRewardPort(playerId -> null,
			new RecordingInventoryDao(), new RecordingPlayerDao(), (p, items) -> true, item -> { },
			new RecordingTitleListDao(), poolId -> false, poolId -> new QuestItems(ITEM_A, 1));

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflight(connection(), snapshot(), List.of(reward("RANDOM", 18505, 1))));

		assertTrue(thrown.getMessage().contains("unknown quest random reward pool 18505"));
	}

	@Test
	void preflightRejectsMultipleDrawsFromOneRandomRewardEntry() throws Exception {
		PlayerQuestRewardPort port = port(playerId -> null, new RecordingInventoryDao(),
			new RecordingPlayerDao(), (p, items) -> true);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflight(connection(), snapshot(), List.of(reward("RANDOM", 18505, 2))));

		assertTrue(thrown.getMessage().contains("must draw exactly one pool entry"));
	}

	@Test
	void preflightFailsOnTitleWithoutPositiveId() throws Exception {
		PlayerQuestRewardPort port = port(playerId -> null, new RecordingInventoryDao(),
			new RecordingPlayerDao(), (p, items) -> true);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflight(connection(), snapshot(), List.of(reward("TITLE", 0, 1))));
		assertTrue(thrown.getMessage().contains("without a positive title id"));
	}

	@Test
	void applyGrantsTitleAndPersistsOnCallerConnection() throws Exception {
		com.aionemu.gameserver.dataholders.DataManager.TITLE_DATA = titleDataWith(TITLE_ID);
		Player player = emptyPlayer();
		RecordingTitleListDao titleListDao = new RecordingTitleListDao();
		PlayerQuestRewardPort port = port(playerId -> player, new RecordingInventoryDao(),
			new RecordingPlayerDao(), (p, items) -> true, titleListDao);
		Connection connection = connection();

		port.apply(connection, snapshot(), List.of(reward("TITLE", TITLE_ID, 1)));

		assertTrue(player.getTitleList().contains(TITLE_ID));
		assertEquals(1, titleListDao.calls.size());
		assertSame(connection, titleListDao.calls.get(0));
		assertEquals(TITLE_ID, titleListDao.lastTitleId);
	}

	@Test
	void titleRewardNotificationIsSentOnlyAfterCommit() throws Exception {
		com.aionemu.gameserver.dataholders.DataManager.TITLE_DATA = titleDataWith(TITLE_ID);
		Player player = emptyPlayer();
		player.getTitleList().setOwner(player);
		setField(Player.class, player, "clientConnection", packetConnection());
		PlayerQuestRewardPort port = port(playerId -> player, new RecordingInventoryDao(),
			new RecordingPlayerDao(), (p, items) -> true);

		QuestTransactionParticipant participant = port.apply(connection(), snapshot(),
			List.of(reward("TITLE", TITLE_ID, 1)));

		assertTrue(packetQueue(player.getClientConnection()).isEmpty());
		participant.afterCommit();
		assertEquals(List.of(SM_SYSTEM_MESSAGE.class, SM_TITLE_INFO.class),
			packetQueue(player.getClientConnection()).stream().map(Object::getClass).toList());
	}

	@Test
	void applySkipsAlreadyOwnedTitleIdempotently() throws Exception {
		com.aionemu.gameserver.dataholders.DataManager.TITLE_DATA = titleDataWith(TITLE_ID);
		Player player = emptyPlayer();
		player.getTitleList().addEntry(TITLE_ID, 0);
		RecordingTitleListDao titleListDao = new RecordingTitleListDao();
		PlayerQuestRewardPort port = port(playerId -> player, new RecordingInventoryDao(),
			new RecordingPlayerDao(), (p, items) -> true, titleListDao);

		QuestTransactionParticipant participant = port.apply(connection(), snapshot(),
			List.of(reward("TITLE", TITLE_ID, 1)));

		assertEquals(0, titleListDao.calls.size());
		participant.afterRollback();
		assertTrue(player.getTitleList().contains(TITLE_ID));
	}

	@Test
	void rollbackRemovesGrantedTitleFromMemory() throws Exception {
		com.aionemu.gameserver.dataholders.DataManager.TITLE_DATA = titleDataWith(TITLE_ID);
		Player player = emptyPlayer();
		RecordingTitleListDao titleListDao = new RecordingTitleListDao();
		PlayerQuestRewardPort port = port(playerId -> player, new RecordingInventoryDao(),
			new RecordingPlayerDao(), (p, items) -> true, titleListDao);

		QuestTransactionParticipant participant = port.apply(connection(), snapshot(),
			List.of(reward("TITLE", TITLE_ID, 1)));

		assertTrue(player.getTitleList().contains(TITLE_ID));
		participant.afterRollback();
		assertFalse(player.getTitleList().contains(TITLE_ID));
		assertEquals(0, player.getInventory().getItemCountByItemId(ITEM_A));
	}

	@Test
	void applyFailsOnInvalidTitleIdAndSkipsDao() throws Exception {
		// TITLE_DATA 为空：addEntry 校验失败，整笔事务回滚且不持久化
		Player player = emptyPlayer();
		RecordingTitleListDao titleListDao = new RecordingTitleListDao();
		PlayerQuestRewardPort port = port(playerId -> player, new RecordingInventoryDao(),
			new RecordingPlayerDao(), (p, items) -> true, titleListDao);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.apply(connection(), snapshot(), List.of(reward("TITLE", TITLE_ID, 1))));
		assertTrue(thrown.getMessage().contains("invalid title id"));
		assertEquals(0, titleListDao.calls.size());
		assertFalse(player.getTitleList().contains(TITLE_ID));
	}

	@Test
	void applyFailsWhenPlayerIsUnavailableBeforeAnyDaoWrite() throws Exception {
		RecordingInventoryDao inventoryDao = new RecordingInventoryDao();
		RecordingPlayerDao playerDao = new RecordingPlayerDao();
		RecordingItemAdder adder = new RecordingItemAdder(true);
		PlayerQuestRewardPort port = port(playerId -> null, inventoryDao, playerDao, adder);

		assertThrows(SQLException.class,
			() -> port.apply(connection(), snapshot(), List.of(reward("ITEM", ITEM_A, 1))));
		assertEquals(0, adder.calls.size());
		assertEquals(0, inventoryDao.transactions.size());
		assertEquals(0, playerDao.calls.size());
	}

	@Test
	void applyInvokesItemAdderAndPersistsDirtyOnCallerConnection() throws Exception {
		Player player = emptyPlayer();
		RecordingInventoryDao inventoryDao = new RecordingInventoryDao();
		AddingItemAdder adder = new AddingItemAdder();
		PlayerQuestRewardPort port = port(playerId -> player, inventoryDao,
			new RecordingPlayerDao(), adder);
		Connection connection = connection();

		QuestTransactionParticipant participant = port.apply(
			connection, snapshot(), List.of(reward("ITEM", ITEM_A, 2)));

		// itemAdder 收到正确奖励清单
		assertEquals(1, adder.calls.size());
		assertEquals(ITEM_A, adder.calls.get(0).get(0).getItemId());
		assertEquals(2, adder.calls.get(0).get(0).getCount());
		// 通过调用方 Connection 持久化 dirty items,与任务状态同事务
		assertEquals(1, inventoryDao.transactions.size());
		assertSame(connection, inventoryDao.transactions.get(0).connection);
		assertTrue(inventoryDao.transactions.get(0).items.stream()
			.anyMatch(item -> item.getItemTemplate().getTemplateId() == ITEM_A));
		assertEquals(PersistentState.UPDATE_REQUIRED, player.getInventory().getPersistentState());
		participant.afterCommit();
		// 仅 commit 后清 dirty,下一次提交不重复写
		assertEquals(PersistentState.UPDATED, player.getInventory().getPersistentState());
	}

	@Test
	void applyDrawsRandomRewardOnceAndGrantsTheResolvedItem() throws Exception {
		Player player = emptyPlayer();
		RecordingItemAdder adder = new RecordingItemAdder(true);
		AtomicInteger draws = new AtomicInteger();
		PlayerQuestRewardPort port = new PlayerQuestRewardPort(playerId -> player,
			new RecordingInventoryDao(), new RecordingPlayerDao(), adder, item -> { },
			new RecordingTitleListDao(), poolId -> poolId == 18505, poolId -> {
				draws.incrementAndGet();
				return new QuestItems(182005205, 1);
			});

		List<QuestAction.GrantReward> rewards = List.of(reward("RANDOM", 18505, 1));
		port.preflight(connection(), snapshot(), rewards);
		port.apply(connection(), snapshot(), rewards);

		assertEquals(1, draws.get());
		assertEquals(1, adder.calls.size());
		assertEquals(182005205, adder.calls.get(0).get(0).getItemId());
		assertEquals(1, adder.calls.get(0).get(0).getCount());
	}

	@Test
	void randomDrawFailureRollsBackEarlierTitleMutation() throws Exception {
		com.aionemu.gameserver.dataholders.DataManager.TITLE_DATA = titleDataWith(TITLE_ID);
		Player player = emptyPlayer();
		RecordingTitleListDao titleListDao = new RecordingTitleListDao();
		PlayerQuestRewardPort port = new PlayerQuestRewardPort(playerId -> player,
			new RecordingInventoryDao(), new RecordingPlayerDao(), (p, items) -> true, item -> { },
			titleListDao, poolId -> true, poolId -> {
				throw new IllegalStateException("invalid random pool result");
			});

		SQLException thrown = assertThrows(SQLException.class, () -> port.apply(connection(), snapshot(),
			List.of(reward("TITLE", TITLE_ID, 1), reward("RANDOM", 18505, 1))));

		assertTrue(thrown.getMessage().contains("failed to draw quest random reward pool 18505"));
		assertFalse(player.getTitleList().contains(TITLE_ID));
		assertEquals(1, titleListDao.calls.size());
	}

	@Test
	void rollbackRestoresGrantedItemsAndAura() throws Exception {
		Player player = playerWithCommonData(false, 66, 1_000_000_000L);
		PlayerQuestRewardPort port = port(playerId -> player, new RecordingInventoryDao(),
			new RecordingPlayerDao(), new AddingItemAdder());

		QuestTransactionParticipant participant = port.apply(connection(), snapshot(),
			List.of(reward("ITEM", ITEM_A, 2), reward("AURA_OF_GROWTH", 0, 5000)));
		assertEquals(2, player.getInventory().getItemCountByItemId(ITEM_A));
		assertEquals(5000, player.getCommonData().getAuraOfGrowth());

		participant.afterRollback();

		assertEquals(0, player.getInventory().getItemCountByItemId(ITEM_A));
		assertEquals(0, player.getCommonData().getAuraOfGrowth());
		assertEquals(PersistentState.UPDATED, player.getInventory().getPersistentState());
	}

	@Test
	void applyFailsWhenItemAdderRejectsAndSkipsDao() throws Exception {
		Player player = emptyPlayer();
		RecordingInventoryDao inventoryDao = new RecordingInventoryDao();
		RecordingItemAdder adder = new RecordingItemAdder(false);
		PlayerQuestRewardPort port = port(playerId -> player, inventoryDao,
			new RecordingPlayerDao(), adder);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.apply(connection(), snapshot(), List.of(reward("ITEM", ITEM_A, 1))));
		assertTrue(thrown.getMessage().contains("failed to add quest items"));
		assertEquals(0, inventoryDao.transactions.size());
	}

	@Test
	void applyRandomRewardAddsDrawnPoolItem() throws Exception {
		Player player = emptyPlayer();
		AddingItemAdder adder = new AddingItemAdder();
		RecordingInventoryDao inventoryDao = new RecordingInventoryDao();
		PlayerQuestRewardPort port = new PlayerQuestRewardPort(playerId -> player, inventoryDao,
			new RecordingPlayerDao(), adder, item -> { }, new RecordingTitleListDao(),
			poolId -> true, poolId -> new QuestItems(182005205, 1));

		QuestTransactionParticipant participant = port.apply(
			connection(), snapshot(), List.of(reward("RANDOM", 18505, 1)));

		assertEquals(1, adder.calls.size());
		assertEquals(182005205, adder.calls.get(0).get(0).getItemId());
		assertEquals(1, adder.calls.get(0).get(0).getCount());
		participant.afterCommit();
	}

	@Test
	void preflightRejectsRandomRewardWithoutPoolId() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestRewardPort port = port(playerId -> player, new RecordingInventoryDao(),
			new RecordingPlayerDao(), new AddingItemAdder());

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.preflight(connection(), snapshot(), List.of(reward("RANDOM", 0, 1))));
		assertTrue(thrown.getMessage().contains("positive pool id"));
	}

	@Test
	void applyGrantsExpAndPersistsCommonDataOnCallerConnection() throws Exception {
		// noExp 短路:addExp 直接返回,不触碰 DataManager 经验表;验证端口调用 addExp 并持久化
		Player player = playerWithCommonData(true, 66, 0);
		RecordingPlayerDao playerDao = new RecordingPlayerDao();
		PlayerQuestRewardPort port = port(playerId -> player, new RecordingInventoryDao(),
			playerDao, (p, items) -> true);
		Connection connection = connection();

		port.apply(connection, snapshot(), List.of(reward("EXP", 0, 1000)));

		assertEquals(0, player.getCommonData().getExp());
		assertEquals(1, playerDao.calls.size());
		assertSame(connection, playerDao.calls.get(0));
		assertEquals(PLAYER_ID, playerDao.lastPlayerId);
		assertSame(player.getCommonData(), playerDao.lastPcd);
	}

	@Test
	void exactExpModeUsesAnIdentityRewardTypeInsteadOfTheQuestRate() {
		assertEquals(1000, RewardType.EXACT.calcReward(null, 1000));
		assertEquals(RewardType.EXACT, PlayerQuestRewardPort.expRewardType(reward("EXP", 0, 1000)));
		assertEquals(RewardType.QUEST, PlayerQuestRewardPort.expRewardType(
			new QuestAction.GrantReward("EXP", 0, 1000, QuestRewardAmountMode.QUEST_BASE)));
	}

	@Test
	void applyGrantsAuraOfGrowthAndPersistsCommonDataOnCallerConnection() throws Exception {
		Player player = playerWithCommonData(false, 66, 1_000_000_000L);
		RecordingPlayerDao playerDao = new RecordingPlayerDao();
		PlayerQuestRewardPort port = port(playerId -> player, new RecordingInventoryDao(),
			playerDao, (p, items) -> true);
		Connection connection = connection();

		port.apply(connection, snapshot(), List.of(reward("AURA_OF_GROWTH", 0, 5000)));

		assertEquals(5000, player.getCommonData().getAuraOfGrowth());
		assertEquals(1, playerDao.calls.size());
		assertSame(connection, playerDao.calls.get(0));
		assertEquals(PLAYER_ID, playerDao.lastPlayerId);
	}

	@Test
	void applyGrantsExpBoostAsAuraOfGrowthAndPersistsCommonData() throws Exception {
		Player player = playerWithCommonData(false, 66, 1_000_000_000L);
		RecordingPlayerDao playerDao = new RecordingPlayerDao();
		PlayerQuestRewardPort port = port(playerId -> player, new RecordingInventoryDao(),
			playerDao, (p, items) -> true);

		port.apply(connection(), snapshot(), List.of(reward("EXP_BOOST", 0, 3)));

		// 镜像旧 QuestService.giveReward 语义:boast 值 × 1060000 折算为 aura of growth
		assertEquals(1060000L * 3, player.getCommonData().getAuraOfGrowth());
		assertEquals(1, playerDao.calls.size());
	}

	@AfterEach
	void restorePlayerMaxLevel() {
		// 静态配置修改仅在需要时注入,测试后恢复,避免泄漏到同 JVM 其他用例
		restoreStaticField(GSConfig.class, "PLAYER_MAX_LEVEL", savedPlayerMaxLevel);
	}

	private static int savedPlayerMaxLevel;

	private static PlayerQuestRewardPort port(QuestPlayerPort players, InventoryDAO inventoryDao,
			PlayerDAO playerDao, BiFunction<Player, List<QuestItems>, Boolean> itemAdder) {
		return port(players, inventoryDao, playerDao, itemAdder, new RecordingTitleListDao());
	}

	private static PlayerQuestRewardPort port(QuestPlayerPort players, InventoryDAO inventoryDao,
			PlayerDAO playerDao, BiFunction<Player, List<QuestItems>, Boolean> itemAdder,
			RecordingTitleListDao titleListDao) {
		return new PlayerQuestRewardPort(players, inventoryDao, playerDao, itemAdder, item -> { }, titleListDao,
			poolId -> true, poolId -> new QuestItems(poolId, 1));
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of(), Map.of());
	}

	private static QuestAction.GrantReward reward(String kind, int id, int amount) {
		return new QuestAction.GrantReward(kind, id, amount);
	}

	private static Player playerWithCommonData(boolean noExp, int level, long auraOfGrowthMax) throws Exception {
		Player player = emptyPlayer();
		PlayerCommonData pcd = new ObjenesisStd().newInstance(PlayerCommonData.class);
		setField(PlayerCommonData.class, pcd, "playerClass", PlayerClass.GLADIATOR);
		setField(PlayerCommonData.class, pcd, "noExp", noExp);
		setField(PlayerCommonData.class, pcd, "level", level);
		setField(PlayerCommonData.class, pcd, "auraOfGrowthMax", auraOfGrowthMax);
		setField(Player.class, player, "playerCommonData", pcd);
		// isReadyForAuraOfGrowth 需要 level 低于 PLAYER_MAX_LEVEL + 1
		savedPlayerMaxLevel = (int) getField(GSConfig.class, null, "PLAYER_MAX_LEVEL");
		setField(GSConfig.class, null, "PLAYER_MAX_LEVEL", Math.max(level + 1, 100));
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
		setField(Player.class, player, "titleList", new TitleList());
		PlayerStorage inventory = new PlayerStorage(StorageType.CUBE);
		inventory.setOwner(player);
		setField(Player.class, player, "inventory", inventory);
		return player;
	}

	private static TitleData emptyTitleData() throws Exception {
		TitleData data = new TitleData();
		setField(TitleData.class, data, "titles", new IntObjectHashMap<>());
		return data;
	}

	private static TitleData titleDataWith(int titleId) throws Exception {
		TitleData data = new TitleData();
		IntObjectHashMap<TitleTemplate> titles = new IntObjectHashMap<>();
		TitleTemplate template = new ObjenesisStd().newInstance(TitleTemplate.class);
		setField(TitleTemplate.class, template, "race", Race.ELYOS);
		titles.put(titleId, template);
		setField(TitleData.class, data, "titles", titles);
		return data;
	}

	private static Item item(int itemId, int count) throws Exception {
		ItemTemplate template = new ObjenesisStd().newInstance(ItemTemplate.class);
		setField(ItemTemplate.class, template, "itemId", itemId);
		Item item = new ObjenesisStd().newInstance(Item.class);
		setField(Item.class, item, "itemTemplate", template);
		setField(Item.class, item, "itemCount", count);
		setField(Item.class, item, "persistentState", PersistentState.NEW);
		setField(AionObject.class, item, "objectId", itemId);
		return item;
	}

	/** 记录 itemAdder 收到的清单;result 控制 apply 返回值。 */
	private static final class RecordingItemAdder implements BiFunction<Player, List<QuestItems>, Boolean> {
		private final List<List<QuestItems>> calls = new ArrayList<>();
		private final boolean result;

		RecordingItemAdder(boolean result) {
			this.result = result;
		}

		@Override
		public Boolean apply(Player player, List<QuestItems> items) {
			calls.add(items);
			return result;
		}
	}

	/** itemAdder 把奖励物品直接写入 CUBE itemStorage 并标记 dirty,模拟真实发奖副作用。 */
	private static final class AddingItemAdder implements BiFunction<Player, List<QuestItems>, Boolean> {
		private final List<List<QuestItems>> calls = new ArrayList<>();

		@Override
		public Boolean apply(Player player, List<QuestItems> items) {
			calls.add(items);
			QuestItems qi = items.get(0);
			try {
				PlayerStorage storage = (PlayerStorage) player.getInventory();
				Item item = item(qi.getItemId(), qi.getCount());
				ItemStorage itemStorage = (ItemStorage) getField(Storage.class, storage, "itemStorage");
				@SuppressWarnings("unchecked")
				Map<Integer, Item> slots = (Map<Integer, Item>) getField(ItemStorage.class, itemStorage, "items");
				slots.put(item.getObjectId(), item);
				setField(Storage.class, storage, "persistentState", PersistentState.UPDATE_REQUIRED);
			} catch (Exception e) {
				throw new IllegalStateException("cannot add test item", e);
			}
			return true;
		}
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Object getField(Class<?> declaringClass, Object target, String name) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void restoreStaticField(Class<?> declaringClass, String name, Object value) {
		if (value == null) {
			return;
		}
		try {
			setField(declaringClass, null, name, value);
		} catch (Exception ignored) {
			// 恢复失败不掩盖用例结果
		}
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
	private static List<AionServerPacket> packetQueue(AionConnection connection) {
		try {
			return (List<AionServerPacket>) getField(AionConnection.class, connection, "sendMsgQueue");
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private static final class RecordingTransport implements ConnectionTransport {
		private AionConnection connection;

		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		@Override
		public void enableWriteInterest() {
			packetQueue(connection).getLast();
		}

		@Override
		public void close(boolean forced) {
		}

		@Override
		public boolean onlyClose() {
			return true;
		}
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

	private static final class RecordingTitleListDao extends PlayerTitleListDAO {
		private final List<Connection> calls = new ArrayList<>();
		private int lastTitleId;

		@Override
		public void storeInTransaction(Connection connection, int playerId, int titleId, int remaining) {
			calls.add(connection);
			lastTitleId = titleId;
		}

		@Override
		public TitleList loadTitleList(int playerId) {
			throw new AssertionError("unexpected loadTitleList");
		}

		@Override
		public boolean storeTitles(Player player, Title entry) {
			throw new AssertionError("unexpected storeTitles");
		}

		@Override
		public boolean removeTitle(int playerId, int titleId) {
			throw new AssertionError("unexpected removeTitle");
		}

		@Override
		public boolean supports(String databaseName, int majorVersion, int minorVersion) {
			return false;
		}
	}
}
