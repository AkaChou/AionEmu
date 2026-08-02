package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
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
 * store today ({@code TITLE}, {@code EXTEND_INVENTORY}, {@code SELECTABLE_ITEM},
 * {@code EXTEND_STIGMA}) are rejected rather than guessed; their transactional
 * wiring is deferred to the retail-calibration batch.</p>
 */
public final class PlayerQuestRewardPort implements QuestRewardPort {
	private final QuestPlayerPort players;
	private final InventoryDAO inventoryDao;
	private final PlayerDAO playerDao;
	private final BiFunction<Player, List<QuestItems>, Boolean> itemAdder;
	private final Consumer<Item> itemReleaser;

	public PlayerQuestRewardPort(QuestPlayerPort players, InventoryDAO inventoryDao, PlayerDAO playerDao,
			BiFunction<Player, List<QuestItems>, Boolean> itemAdder) {
		this(players, inventoryDao, playerDao, itemAdder, ItemService::releaseItemId);
	}

	PlayerQuestRewardPort(QuestPlayerPort players, InventoryDAO inventoryDao, PlayerDAO playerDao,
			BiFunction<Player, List<QuestItems>, Boolean> itemAdder, Consumer<Item> itemReleaser) {
		this.players = Objects.requireNonNull(players, "players");
		this.inventoryDao = Objects.requireNonNull(inventoryDao, "inventoryDao");
		this.playerDao = Objects.requireNonNull(playerDao, "playerDao");
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
		for (QuestAction.GrantReward reward : rewards) {
			QuestRewardKind kind = reward.rewardKind();
			switch (kind) {
				case ITEM -> items.add(new QuestItems(reward.id(), (int) reward.amount()));
				case EXP, EXP_BOOST, AURA_OF_GROWTH -> {
				}
				default -> throw new SQLException("unsupported durable reward " + kind);
			}
		}
		boolean itemRewards = !items.isEmpty();
		boolean commonDataChanged = rewards.stream().anyMatch(reward -> reward.rewardKind() != QuestRewardKind.ITEM);
		if (!itemRewards && !commonDataChanged) {
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
					case EXP -> player.getCommonData().addExp(reward.amount(), RewardType.QUEST);
					case EXP_BOOST -> player.getCommonData().addAuraOfGrowth(1060000L * reward.amount());
					case AURA_OF_GROWTH -> player.getCommonData().addAuraOfGrowth(reward.amount());
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
				if (commonSnapshot != null) {
					commonSnapshot.restore();
				}
				if (inventorySnapshot != null) {
					inventorySnapshot.restore(itemReleaser);
				}
			});
		} catch (SQLException | RuntimeException failure) {
			try {
				if (commonSnapshot != null) commonSnapshot.restore();
				if (inventorySnapshot != null) inventorySnapshot.restore(itemReleaser);
			} catch (RuntimeException restoreFailure) {
				failure.addSuppressed(restoreFailure);
			}
			throw failure;
		}
	}

	private static boolean supported(QuestRewardKind kind) {
		return kind == QuestRewardKind.ITEM || kind == QuestRewardKind.EXP
			|| kind == QuestRewardKind.EXP_BOOST || kind == QuestRewardKind.AURA_OF_GROWTH;
	}
}
