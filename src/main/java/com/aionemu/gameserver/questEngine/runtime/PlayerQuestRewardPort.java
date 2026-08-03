package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerTitleListDAO;
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

/**
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
 * TITLE 奖励只更新内存 TitleList，不发 SM_TITLE_INFO 表现包（与其他 durable 奖励一致，
 * 表现层刷新留待统一处理）。</p>
 */
public final class PlayerQuestRewardPort implements QuestRewardPort {
	private final QuestPlayerPort players;
	private final InventoryDAO inventoryDao;
	private final PlayerDAO playerDao;
	private final PlayerTitleListDAO titleListDao;
	private final BiFunction<Player, List<QuestItems>, Boolean> itemAdder;
	private final Consumer<Item> itemReleaser;

	public PlayerQuestRewardPort(QuestPlayerPort players, InventoryDAO inventoryDao, PlayerDAO playerDao,
			BiFunction<Player, List<QuestItems>, Boolean> itemAdder, PlayerTitleListDAO titleListDao) {
		this(players, inventoryDao, playerDao, itemAdder, ItemService::releaseItemId, titleListDao);
	}

	PlayerQuestRewardPort(QuestPlayerPort players, InventoryDAO inventoryDao, PlayerDAO playerDao,
			BiFunction<Player, List<QuestItems>, Boolean> itemAdder, Consumer<Item> itemReleaser,
			PlayerTitleListDAO titleListDao) {
		this.players = Objects.requireNonNull(players, "players");
		this.inventoryDao = Objects.requireNonNull(inventoryDao, "inventoryDao");
		this.playerDao = Objects.requireNonNull(playerDao, "playerDao");
		this.titleListDao = Objects.requireNonNull(titleListDao, "titleListDao");
		this.itemAdder = Objects.requireNonNull(itemAdder, "itemAdder");
		this.itemReleaser = Objects.requireNonNull(itemReleaser, "itemReleaser");
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
		List<QuestItems> items = new ArrayList<>();
		List<Integer> grantedTitles = new ArrayList<>();
		for (QuestAction.GrantReward reward : rewards) {
			QuestRewardKind kind = reward.rewardKind();
			switch (kind) {
				case ITEM -> items.add(new QuestItems(reward.id(), (int) QuestRewardAmounts.resolve(player, reward)));
				case EXP, EXP_BOOST, AURA_OF_GROWTH -> {
				}
				case TITLE -> grantTitle(connection, snapshot, player, reward, grantedTitles);
				default -> throw new SQLException("unsupported durable reward " + kind);
			}
		}
		boolean itemRewards = !items.isEmpty();
		boolean commonDataChanged = rewards.stream().anyMatch(reward -> {
			QuestRewardKind kind = reward.rewardKind();
			return kind != QuestRewardKind.ITEM && kind != QuestRewardKind.TITLE;
		});
		if (!itemRewards && !commonDataChanged && grantedTitles.isEmpty()) {
			return QuestTransactionParticipant.none();
		}
		var inventorySnapshot = itemRewards ? player.getInventory().transactionSnapshot() : null;
		var commonSnapshot = commonDataChanged ? player.getCommonData().transactionSnapshot() : null;
		try {
			if (itemRewards && !itemAdder.apply(player, items)) {
				throw new SQLException("failed to add quest items for player " + snapshot.playerId());
			}
			for (QuestAction.GrantReward reward : rewards) {
				switch (reward.rewardKind()) {
					case ITEM -> {
					}
					case EXP -> player.getCommonData().addExp(QuestRewardAmounts.resolve(player, reward),
						expRewardType(reward));
					case EXP_BOOST -> player.getCommonData().addAuraOfGrowth(1060000L * QuestRewardAmounts.resolve(player, reward));
					case AURA_OF_GROWTH -> player.getCommonData().addAuraOfGrowth(QuestRewardAmounts.resolve(player, reward));
					case TITLE -> {
					// 已在首个循环中经 grantTitle 处理，仅内存+连接持久化，不回写 common data
				}
				default -> throw new SQLException("unsupported durable reward " + reward.rewardKind());
				}
			}
			List<Item> dirty = itemRewards ? List.copyOf(player.getDirtyItemsToUpdate()) : List.of();
			if (!dirty.isEmpty()) {
				inventoryDao.storeInTransaction(connection, dirty, snapshot.playerId(), null, null);
			}
			if (commonDataChanged) {
				playerDao.storeInTransaction(connection, snapshot.playerId(), player.getCommonData());
			}
			return QuestTransactionParticipant.of(() -> {
				if (!dirty.isEmpty()) {
					inventoryDao.markStored(dirty);
					player.markDirtyItemContainersStored();
				}
			}, () -> {
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
				rollbackTitles(player, grantedTitles);
				if (commonSnapshot != null) commonSnapshot.restore();
				if (inventorySnapshot != null) inventorySnapshot.restore(itemReleaser);
			} catch (RuntimeException restoreFailure) {
				failure.addSuppressed(restoreFailure);
			}
			throw failure;
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
		titleListDao.storeInTransaction(connection, snapshot.playerId(), titleId, 0);
		grantedTitles.add(titleId);
	}

	private static void rollbackTitles(Player player, List<Integer> grantedTitles) {
		if (!grantedTitles.isEmpty() && player.getTitleList() != null) {
			for (int titleId : grantedTitles) {
				player.getTitleList().removeEntry(titleId);
			}
		}
	}

	private static boolean supported(QuestRewardKind kind) {
		return kind == QuestRewardKind.ITEM || kind == QuestRewardKind.EXP
			|| kind == QuestRewardKind.EXP_BOOST || kind == QuestRewardKind.AURA_OF_GROWTH
			|| kind == QuestRewardKind.TITLE;
	}

	static RewardType expRewardType(QuestAction.GrantReward reward) {
		return reward.amountMode() == QuestRewardAmountMode.QUEST_BASE ? RewardType.QUEST : RewardType.EXACT;
	}
}
