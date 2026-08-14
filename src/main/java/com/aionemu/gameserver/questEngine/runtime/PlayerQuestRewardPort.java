package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerTitleListDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.services.item.ItemService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

/**
 * 真实 {@link QuestRewardPort}：将耐久（非货币）任务奖励应用到在线玩家，
 * 并通过调用方连接上的事务 DAO 持久化。
 * Real {@link QuestRewardPort}: applies durable (non-currency) quest rewards to
 * the live player and persists them through the transactional DAO on the
 * caller-owned connection. The item grant path is injected as a function so the
 * production wiring uses {@code ItemService::addQuestItems} while tests stay
 * free of the static {@code DataManager}.
 *
 * <p>Preflight fails closed on unsupported kinds. Kinds with no transactional
 * store today ({@code EXTEND_INVENTORY}, {@code SELECTABLE_ITEM},
 * {@code EXTEND_STIGMA}) are rejected rather than guessed; their transactional
 * wiring is deferred to the retail-calibration batch.</p>
 *
 * <p>TITLE 奖励在调用方连接上经 {@link PlayerTitleListDAO#storeInTransaction} 持久化，
 * 与任务状态同事务；重复领取幂等（已拥有则不重复发放）。
 * 获得称号消息和 TitleList 刷新仅在事务提交后发送。</p>
 */
public final class PlayerQuestRewardPort implements QuestRewardPort {
	private final QuestPlayerPort players;
	private final InventoryDAO inventoryDao;
	private final PlayerDAO playerDao;
	private final PlayerTitleListDAO titleListDao;
	private final BiFunction<Player, List<QuestItems>, Boolean> itemAdder;
	private final Consumer<Item> itemReleaser;
	private final IntPredicate randomPoolExists;
	private final IntFunction<QuestItems> randomDraw;

	public PlayerQuestRewardPort(QuestPlayerPort players, InventoryDAO inventoryDao, PlayerDAO playerDao,
			BiFunction<Player, List<QuestItems>, Boolean> itemAdder, PlayerTitleListDAO titleListDao) {
		this(players, inventoryDao, playerDao, itemAdder, ItemService::releaseItemId, titleListDao,
			poolId -> DataManager.QUEST_RANDOM_REWARDS != null
				&& DataManager.QUEST_RANDOM_REWARDS.containsPool(poolId),
			poolId -> DataManager.QUEST_RANDOM_REWARDS.draw(poolId));
	}

	PlayerQuestRewardPort(QuestPlayerPort players, InventoryDAO inventoryDao, PlayerDAO playerDao,
			BiFunction<Player, List<QuestItems>, Boolean> itemAdder, Consumer<Item> itemReleaser,
			PlayerTitleListDAO titleListDao, IntPredicate randomPoolExists, IntFunction<QuestItems> randomDraw) {
		this.players = Objects.requireNonNull(players, "players");
		this.inventoryDao = Objects.requireNonNull(inventoryDao, "inventoryDao");
		this.playerDao = Objects.requireNonNull(playerDao, "playerDao");
		this.titleListDao = Objects.requireNonNull(titleListDao, "titleListDao");
		this.itemAdder = Objects.requireNonNull(itemAdder, "itemAdder");
		this.itemReleaser = Objects.requireNonNull(itemReleaser, "itemReleaser");
		this.randomPoolExists = Objects.requireNonNull(randomPoolExists, "randomPoolExists");
		this.randomDraw = Objects.requireNonNull(randomDraw, "randomDraw");
	}

	@Override
	public void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.GrantReward> rewards) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		for (QuestAction.GrantReward reward : rewards) {
			QuestRewardKind kind = reward.rewardKind();
			if (!supported(kind)) {
				throw new SQLException("no transactional durable reward store for kind " + kind);
			}
			if (reward.amount() < 0) {
				throw new SQLException("negative durable reward " + kind + " for player " + snapshot.playerId());
			}
			if (kind == QuestRewardKind.ITEM && reward.id() <= 0) {
				throw new SQLException("item reward without a positive item id for player " + snapshot.playerId());
			}
			if (kind == QuestRewardKind.RANDOM) {
				if (reward.id() <= 0) {
					throw new SQLException("random reward without a positive pool id for player " + snapshot.playerId());
				}
				if (reward.amount() != 1) {
					throw new SQLException("random reward must draw exactly one pool entry for player " + snapshot.playerId());
				}
				if (!randomPoolExists.test(reward.id())) {
					throw new SQLException("unknown quest random reward pool " + reward.id());
				}
			}
			if (kind == QuestRewardKind.TITLE && reward.id() <= 0) {
				throw new SQLException("title reward without a positive title id for player " + snapshot.playerId());
			}
			if (reward.amountMode() == com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode.QUEST_BASE
					&& kind != QuestRewardKind.EXP) {
				throw new SQLException("QUEST_BASE is unsupported for durable reward " + kind);
			}
		}
	}

	@Override
	public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.GrantReward> rewards) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			throw new SQLException("player is unavailable: " + snapshot.playerId());
		}
		boolean itemRewards = rewards.stream().anyMatch(reward -> {
			QuestRewardKind kind = reward.rewardKind();
			return kind == QuestRewardKind.ITEM || kind == QuestRewardKind.RANDOM;
		});
		boolean commonDataChanged = rewards.stream().anyMatch(reward -> {
			QuestRewardKind kind = reward.rewardKind();
			return kind != QuestRewardKind.ITEM && kind != QuestRewardKind.TITLE && kind != QuestRewardKind.RANDOM;
		});
		List<QuestItems> items = new ArrayList<>();
		List<Integer> grantedTitles = new ArrayList<>();
		var inventorySnapshot = itemRewards ? player.getInventory().transactionSnapshot() : null;
		var commonSnapshot = commonDataChanged ? player.getCommonData().transactionSnapshot() : null;
		QuestInventoryPersistenceStage inventoryStage = QuestInventoryPersistenceStage.none();
		try {
			for (QuestAction.GrantReward reward : rewards) {
				QuestRewardKind kind = reward.rewardKind();
				switch (kind) {
					case ITEM -> items.add(new QuestItems(reward.id(), (int) QuestRewardAmounts.resolve(player, reward)));
					case RANDOM -> items.add(drawRandomReward(snapshot, reward.id()));
					case EXP, EXP_BOOST, AURA_OF_GROWTH -> {
					}
					case TITLE -> grantTitle(connection, snapshot, player, reward, grantedTitles);
					default -> throw new SQLException("unsupported durable reward " + kind);
				}
			}
			if (!itemRewards && !commonDataChanged && grantedTitles.isEmpty()) {
				return QuestTransactionParticipant.none();
			}
			if (itemRewards && !itemAdder.apply(player, items)) {
				throw new SQLException("failed to add quest items for player " + snapshot.playerId());
			}
			for (QuestAction.GrantReward reward : rewards) {
				switch (reward.rewardKind()) {
					case ITEM, RANDOM -> {
					}
					case EXP -> player.getCommonData().addExp(QuestRewardAmounts.resolve(player, reward),
						expRewardType(reward));
					case EXP_BOOST -> player.getCommonData().addAuraOfGrowth(1060000L * QuestRewardAmounts.resolve(player, reward));
					case AURA_OF_GROWTH -> player.getCommonData().addAuraOfGrowth(QuestRewardAmounts.resolve(player, reward));
					case TITLE -> {
						// 已在首个循环中经 grantTitle 处理，仅内存+连接持久化，不回写 common data。
						// Handled by grantTitle in the first loop; in-memory + connection persistence only, no common-data write-back.
					}
					default -> throw new SQLException("unsupported durable reward " + reward.rewardKind());
				}
			}
			List<Item> dirty = itemRewards ? List.copyOf(player.getDirtyItemsToUpdate()) : List.of();
			inventoryStage = QuestInventoryPersistenceStage.persist(inventoryDao, connection, player, dirty);
			if (commonDataChanged) {
				playerDao.storeInTransaction(connection, snapshot.playerId(), player.getCommonData());
			}
			final QuestInventoryPersistenceStage committedInventoryStage = inventoryStage;
			List<Integer> committedTitles = List.copyOf(grantedTitles);
			return QuestTransactionParticipant.of(() -> {
				committedInventoryStage.afterCommit();
				for (int titleId : committedTitles) {
					player.getTitleList().notifyQuestReward(titleId);
				}
			}, () -> {
				committedInventoryStage.afterRollback();
				rollbackTitles(player, grantedTitles);
				if (commonSnapshot != null) {
					commonSnapshot.restore();
				}
				if (inventorySnapshot != null) {
					inventorySnapshot.restore(itemReleaser);
				}
			});
		} catch (SQLException | RuntimeException failure) {
			try {
				inventoryStage.afterRollback();
				rollbackTitles(player, grantedTitles);
				if (commonSnapshot != null) commonSnapshot.restore();
				if (inventorySnapshot != null) inventorySnapshot.restore(itemReleaser);
			} catch (RuntimeException restoreFailure) {
				failure.addSuppressed(restoreFailure);
			}
			throw failure;
		}
	}

	private QuestItems drawRandomReward(QuestSnapshot snapshot, int poolId) throws SQLException {
		try {
			QuestItems item = randomDraw.apply(poolId);
			if (item == null || item.getItemId() == null || item.getItemId() <= 0
					|| item.getCount() == null || item.getCount() <= 0) {
				throw new IllegalArgumentException("pool returned an invalid item");
			}
			return item;
		} catch (RuntimeException e) {
			throw new SQLException("failed to draw quest random reward pool " + poolId
				+ " for player " + snapshot.playerId(), e);
		}
	}

	/**
	 * 发放称号奖励：内存加入 TitleList 并在调用方连接上持久化，已拥有则幂等跳过。
	 * Grants a title reward: adds to the in-memory TitleList and persists on the caller connection; idempotent.
	 */
	private void grantTitle(Connection connection, QuestSnapshot snapshot, Player player,
			QuestAction.GrantReward reward, List<Integer> grantedTitles) throws SQLException {
		TitleList titleList = player.getTitleList();
		if (titleList == null) {
			throw new SQLException("player has no title list: " + snapshot.playerId());
		}
		int titleId = (int) reward.id();
		if (titleList.contains(titleId)) {
			return;
		}
		try {
			titleList.addEntry(titleId, 0);
		} catch (IllegalArgumentException e) {
			throw new SQLException("invalid title id " + titleId + " for player " + snapshot.playerId(), e);
		}
		grantedTitles.add(titleId);
		titleListDao.storeInTransaction(connection, snapshot.playerId(), titleId, 0);
	}

	private static void rollbackTitles(Player player, List<Integer> grantedTitles) {
		if (!grantedTitles.isEmpty() && player.getTitleList() != null) {
			for (int titleId : grantedTitles) {
				player.getTitleList().removeEntry(titleId);
			}
		}
	}

	private static boolean supported(QuestRewardKind kind) {
		return kind == QuestRewardKind.ITEM || kind == QuestRewardKind.RANDOM || kind == QuestRewardKind.EXP
			|| kind == QuestRewardKind.EXP_BOOST || kind == QuestRewardKind.AURA_OF_GROWTH
			|| kind == QuestRewardKind.TITLE;
	}

	static RewardType expRewardType(QuestAction.GrantReward reward) {
		return reward.amountMode() == QuestRewardAmountMode.QUEST_BASE ? RewardType.QUEST : RewardType.EXACT;
	}
}
