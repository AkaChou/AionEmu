package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Real {@link QuestCurrencyPort}: applies currency rewards (kinah, AP, GP, DP)
 * to the live player and persists them through each currency's transactional
 * DAO on the caller-owned connection, so they commit atomically with the quest
 * state. Preflight fails closed on uncaptured facts or unsupported kinds.
 */
public final class PlayerQuestCurrencyPort implements QuestCurrencyPort {
	private final QuestPlayerPort players;
	private final InventoryDAO inventoryDao;
	private final AbyssRankDAO abyssRankDao;
	private final PlayerDAO playerDao;
	private final Consumer<Item> itemReleaser;

	public PlayerQuestCurrencyPort(QuestPlayerPort players, InventoryDAO inventoryDao,
			AbyssRankDAO abyssRankDao, PlayerDAO playerDao) {
		this(players, inventoryDao, abyssRankDao, playerDao, ItemService::releaseItemId);
	}

	PlayerQuestCurrencyPort(QuestPlayerPort players, InventoryDAO inventoryDao,
			AbyssRankDAO abyssRankDao, PlayerDAO playerDao, Consumer<Item> itemReleaser) {
		this.players = Objects.requireNonNull(players, "players");
		this.inventoryDao = Objects.requireNonNull(inventoryDao, "inventoryDao");
		this.abyssRankDao = Objects.requireNonNull(abyssRankDao, "abyssRankDao");
		this.playerDao = Objects.requireNonNull(playerDao, "playerDao");
		this.itemReleaser = Objects.requireNonNull(itemReleaser, "itemReleaser");
	}

	@Override
	public void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.GrantReward> rewards) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		if (!snapshot.currenciesCaptured()) {
			throw new SQLException("currency facts are not captured for player " + snapshot.playerId());
		}
		for (QuestAction.GrantReward reward : rewards) {
			QuestRewardKind kind = reward.rewardKind();
			if (!supported(kind)) {
				throw new SQLException("no transactional currency store for kind " + kind);
			}
			if (reward.amount() < 0) {
				throw new SQLException("negative currency reward " + kind + " for player " + snapshot.playerId());
			}
			if (reward.amountMode() == com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode.QUEST_BASE
					&& kind != QuestRewardKind.GOLD && kind != QuestRewardKind.KINAH
					&& kind != QuestRewardKind.AP && kind != QuestRewardKind.GP) {
				throw new SQLException("QUEST_BASE is unsupported for currency reward " + kind);
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
		long kinah = 0;
		long apTotal = 0;
		long gpTotal = 0;
		long dpTotal = 0;
		for (QuestAction.GrantReward reward : rewards) {
			QuestRewardKind kind = reward.rewardKind();
			long amount = QuestRewardAmounts.resolve(player, reward);
			if (amount < 0) {
				throw new SQLException("negative resolved currency reward " + kind + " for player "
					+ snapshot.playerId());
			}
			try {
				switch (kind) {
					case GOLD, KINAH -> kinah = Math.addExact(kinah, amount);
					case AP -> apTotal = Math.addExact(apTotal, amount);
					case GP -> gpTotal = Math.addExact(gpTotal, amount);
					case DP -> dpTotal = Math.addExact(dpTotal, amount);
					default -> throw new SQLException("unsupported currency reward " + kind);
				}
			} catch (ArithmeticException overflow) {
				throw new SQLException("currency reward amount overflow for player " + snapshot.playerId(), overflow);
			}
		}
		final int ap;
		final int gp;
		final int dp;
		try {
			ap = Math.toIntExact(apTotal);
			gp = Math.toIntExact(gpTotal);
			dp = Math.toIntExact(dpTotal);
		} catch (ArithmeticException overflow) {
			throw new SQLException("currency reward amount exceeds live integer balance for player "
				+ snapshot.playerId(), overflow);
		}
		if (kinah == 0 && ap == 0 && gp == 0 && dp == 0) {
			return QuestTransactionParticipant.none();
		}
		boolean rankChanged = ap > 0 || gp > 0;
		var inventorySnapshot = kinah > 0 ? player.getInventory().transactionSnapshot() : null;
		var rankSnapshot = rankChanged ? player.getAbyssRank().transactionSnapshot() : null;
		var commonSnapshot = dp > 0 ? player.getCommonData().transactionSnapshot() : null;
		try {
			if (kinah > 0) {
				player.getInventory().increaseKinah(kinah, ItemUpdateType.INC_KINAH_QUEST);
			}
			List<Item> dirty = kinah > 0 ? List.copyOf(player.getDirtyItemsToUpdate()) : List.of();
			if (!dirty.isEmpty()) {
				inventoryDao.storeInTransaction(connection, dirty, snapshot.playerId(), null, null);
			}
			if (ap > 0) {
				player.getAbyssRank().addAp(ap, player);
			}
			if (gp > 0) {
				player.getAbyssRank().addGp(gp);
			}
			if (rankChanged) {
				abyssRankDao.storeInTransaction(connection, player.getObjectId(), player.getAbyssRank());
			}
			if (dp > 0) {
				player.getCommonData().setDp(player.getCommonData().getDp() + dp);
				playerDao.storeInTransaction(connection, player.getObjectId(), player.getCommonData());
			}
			return QuestTransactionParticipant.of(() -> {
				if (!dirty.isEmpty()) {
					inventoryDao.markStored(dirty);
					player.markDirtyItemContainersStored();
				}
				if (rankChanged) {
					player.getAbyssRank().setPersistentState(PersistentState.UPDATED);
				}
			}, () -> {
				if (commonSnapshot != null) {
					commonSnapshot.restore();
				}
				if (rankSnapshot != null) {
					rankSnapshot.restore();
				}
				if (inventorySnapshot != null) {
					inventorySnapshot.restore(itemReleaser);
				}
			});
		} catch (SQLException | RuntimeException failure) {
			try {
				if (commonSnapshot != null) commonSnapshot.restore();
				if (rankSnapshot != null) rankSnapshot.restore();
				if (inventorySnapshot != null) inventorySnapshot.restore(itemReleaser);
			} catch (RuntimeException restoreFailure) {
				failure.addSuppressed(restoreFailure);
			}
			throw failure;
		}
	}

	private static boolean supported(QuestRewardKind kind) {
		return kind == QuestRewardKind.GOLD || kind == QuestRewardKind.KINAH
			|| kind == QuestRewardKind.AP || kind == QuestRewardKind.GP
			|| kind == QuestRewardKind.DP;
	}
}
