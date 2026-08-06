package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
		if (rewards.isEmpty()) {
			return;
		}
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
		if (rewards.isEmpty()) {
			return QuestTransactionParticipant.none();
		}
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
		validateGrantBalances(player, snapshot.playerId(), kinah, ap, gp, dp);
		boolean rankChanged = ap > 0 || gp > 0;
		var inventorySnapshot = kinah > 0 ? player.getInventory().transactionSnapshot() : null;
		var rankSnapshot = rankChanged ? player.getAbyssRank().transactionSnapshot() : null;
		var commonSnapshot = dp > 0 ? player.getCommonData().transactionSnapshot() : null;
		final AbyssRankEnum oldRank = rankChanged ? player.getAbyssRank().getRank() : null;
		Item rewardKinahItem = null;
		boolean rewardKinahItemCreated = false;
		try {
			if (kinah > 0) {
				rewardKinahItemCreated = player.getInventory().getKinahItem() == null;
				if (player.getInventory().increaseKinahSilently(kinah) != 0) {
					throw new SQLException("kinah reward exceeds the live stack limit for player "
						+ snapshot.playerId());
				}
				rewardKinahItem = player.getInventory().getKinahItem();
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
				player.getCommonData().setDpSilently(player.getCommonData().getDp() + dp);
				playerDao.storeInTransaction(connection, player.getObjectId(), player.getCommonData());
			}
			final Item committedKinahItem = rewardKinahItem;
			final boolean committedKinahItemCreated = rewardKinahItemCreated;
			final List<Item> committedDirty = dirty;
			final boolean committedRankChanged = rankChanged;
			final var committedCommonSnapshot = commonSnapshot;
			final int committedAp = ap;
			final int committedGp = gp;
			final AbyssRankEnum committedOldRank = oldRank;
			final AbyssRankEnum committedNewRank = rankChanged ? player.getAbyssRank().getRank() : null;
			return QuestTransactionParticipant.of(() -> {
				if (!committedDirty.isEmpty()) {
					inventoryDao.markStored(committedDirty);
					player.markDirtyItemContainersStored();
					if (committedKinahItem != null) {
						if (committedKinahItemCreated) {
							ItemPacketService.sendStorageUpdatePacket(player, player.getInventory().getStorageType(),
								committedKinahItem);
						}
						ItemPacketService.sendItemPacket(player, player.getInventory().getStorageType(), committedKinahItem,
							ItemUpdateType.INC_KINAH_QUEST);
					}
				}
				if (committedRankChanged) {
					player.getAbyssRank().setPersistentState(PersistentState.UPDATED);
					if (committedOldRank != committedNewRank) {
						AbyssPointsService.checkRankChanged(player, committedOldRank, committedNewRank);
					} else {
						PacketSendUtility.sendPacket(player, new SM_ABYSS_RANK(player.getAbyssRank()));
					}
					if (committedAp > 0) {
						PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_COMBAT_MY_ABYSS_POINT_GAIN(committedAp));
					}
					if (committedGp > 0) {
						PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_GLORY_POINT_GAIN(committedGp));
					}
				}
				if (committedCommonSnapshot != null) {
					player.getCommonData().publishDp();
				}
			}, () -> {
				if (committedCommonSnapshot != null) {
					committedCommonSnapshot.restore();
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

	@Override
	public void preflightDebits(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.DecreaseCurrency> debits) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		if (debits.isEmpty()) {
			return;
		}
		if (!snapshot.currenciesCaptured()) {
			throw new SQLException("currency facts are not captured for player " + snapshot.playerId());
		}
		Map<QuestRewardKind, Long> totals = new EnumMap<>(QuestRewardKind.class);
		for (QuestAction.DecreaseCurrency debit : debits) {
			if (!supported(debit.kind())) {
				throw new SQLException("no transactional currency store for kind " + debit.kind());
			}
			QuestRewardKind balanceKind = canonicalCurrencyKind(debit.kind());
			long total;
			try {
				total = totals.merge(balanceKind, debit.amount(), Math::addExact);
			} catch (ArithmeticException overflow) {
				throw new SQLException("currency debit amount overflow for player " + snapshot.playerId(), overflow);
			}
			if (snapshot.balance(balanceKind) < total) {
				throw new SQLException("insufficient " + balanceKind + " for player " + snapshot.playerId());
			}
		}
	}

	@Override
	public QuestTransactionParticipant applyDebits(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.DecreaseCurrency> debits) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		if (debits.isEmpty()) {
			return QuestTransactionParticipant.none();
		}
		preflightDebits(connection, snapshot, debits);
		long kinah;
		int ap;
		int gp;
		int dp;
		try {
			kinah = 0;
			ap = 0;
			gp = 0;
			dp = 0;
			for (QuestAction.DecreaseCurrency debit : debits) {
					if (!supported(debit.kind())) {
						throw new SQLException("no transactional currency store for kind " + debit.kind());
					}
					switch (canonicalCurrencyKind(debit.kind())) {
						case GOLD -> kinah = Math.addExact(kinah, debit.amount());
						case AP -> ap = Math.addExact(ap, Math.toIntExact(debit.amount()));
						case GP -> gp = Math.addExact(gp, Math.toIntExact(debit.amount()));
						case DP -> dp = Math.addExact(dp, Math.toIntExact(debit.amount()));
						default -> throw new SQLException("unsupported currency debit " + debit.kind());
					}
			}
		} catch (ArithmeticException overflow) {
			throw new SQLException("currency debit amount overflow for player " + snapshot.playerId(), overflow);
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			throw new SQLException("player is unavailable: " + snapshot.playerId());
		}
		if ((kinah > 0 && (player.getInventory() == null || player.getInventory().getKinahItem() == null))
				|| ((ap > 0 || gp > 0) && player.getAbyssRank() == null)
				|| (dp > 0 && player.getCommonData() == null)
				|| (player.getInventory() != null && kinah > player.getInventory().getKinah())
				|| (player.getAbyssRank() != null && (ap > player.getAbyssRank().getAp()
					|| gp > player.getAbyssRank().getGp()))
				|| (player.getCommonData() != null && dp > player.getCommonData().getDp())) {
			throw new SQLException("live currency balance is insufficient for player " + snapshot.playerId());
		}
		var inventorySnapshot = kinah > 0 ? player.getInventory().transactionSnapshot() : null;
		var rankSnapshot = ap > 0 || gp > 0 ? player.getAbyssRank().transactionSnapshot() : null;
		var commonSnapshot = dp > 0 ? player.getCommonData().transactionSnapshot() : null;
		Item kinahItem = kinah > 0 ? player.getInventory().getKinahItem() : null;
		final boolean rankDebitChanged = ap > 0 || gp > 0;
		final AbyssRankEnum oldRank = rankDebitChanged ? player.getAbyssRank().getRank() : null;
		try {
			if (kinah > 0 && kinahItem.decreaseItemCount(kinah) != 0) {
				throw new SQLException("failed to decrease kinah for player " + snapshot.playerId());
			}
			if (kinah > 0) {
				player.getInventory().setPersistentState(PersistentState.UPDATE_REQUIRED);
			}
			if (ap > 0) {
				// Mutate the rank projection directly; the service wrapper sends packets before
				// the caller-owned JDBC transaction commits.
				int expectedAp = player.getAbyssRank().getAp() - ap;
				player.getAbyssRank().addAp(-ap, player);
				if (player.getAbyssRank().getAp() != expectedAp) {
					throw new SQLException("live AP debit was not applied exactly for player " + snapshot.playerId());
				}
			}
			if (gp > 0) {
				int expectedGp = player.getAbyssRank().getGp() - gp;
				player.getAbyssRank().addGp(-gp);
				if (player.getAbyssRank().getGp() != expectedGp) {
					throw new SQLException("live GP debit was not applied exactly for player " + snapshot.playerId());
				}
			}
			if (dp > 0) {
				int expectedDp = player.getCommonData().getDp() - dp;
				player.getCommonData().setDpSilently(expectedDp);
				if (player.getCommonData().getDp() != expectedDp) {
					throw new SQLException("live DP debit was not applied exactly for player " + snapshot.playerId());
				}
			}
			List<Item> dirty = kinah > 0 ? List.copyOf(player.getDirtyItemsToUpdate()) : List.of();
			if (!dirty.isEmpty()) {
				inventoryDao.storeInTransaction(connection, dirty, snapshot.playerId(), null, null);
			}
			if (ap > 0 || gp > 0) {
				abyssRankDao.storeInTransaction(connection, player.getObjectId(), player.getAbyssRank());
			}
			if (dp > 0) {
				playerDao.storeInTransaction(connection, player.getObjectId(), player.getCommonData());
			}
			final AbyssRankEnum newRank = rankDebitChanged ? player.getAbyssRank().getRank() : null;
			final int debitedAp = ap;
			final int debitedGp = gp;
			return QuestTransactionParticipant.of(() -> {
				if (!dirty.isEmpty()) {
					inventoryDao.markStored(dirty);
					player.markDirtyItemContainersStored();
					if (kinahItem != null) {
						ItemPacketService.sendItemPacket(player, player.getInventory().getStorageType(), kinahItem,
							ItemUpdateType.DEC_KINAH_BUY);
					}
				}
				if (rankDebitChanged) {
					player.getAbyssRank().setPersistentState(PersistentState.UPDATED);
					if (debitedAp > 0) {
						PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300965, debitedAp));
					}
					if (debitedGp > 0) {
						PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402219, debitedGp));
					}
					if (oldRank != newRank) {
						AbyssPointsService.checkRankChanged(player, oldRank, newRank);
					} else {
						PacketSendUtility.sendPacket(player, new SM_ABYSS_RANK(player.getAbyssRank()));
					}
				}
				if (commonSnapshot != null) {
					player.getCommonData().publishDp();
				}
			}, () -> {
				if (commonSnapshot != null) commonSnapshot.restore();
				if (rankSnapshot != null) rankSnapshot.restore();
				if (inventorySnapshot != null) inventorySnapshot.restore(itemReleaser);
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

	@Override
	public void preflightSets(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.SetCurrency> sets) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		if (sets.isEmpty()) {
			return;
		}
		if (!snapshot.currenciesCaptured()) {
			throw new SQLException("currency facts are not captured for player " + snapshot.playerId());
		}
		Map<QuestRewardKind, Boolean> seen = new EnumMap<>(QuestRewardKind.class);
		for (QuestAction.SetCurrency set : sets) {
			validateSetTarget(snapshot, set);
			if (seen.put(set.kind(), Boolean.TRUE) != null) {
				throw new SQLException("multiple exact writes for " + set.kind() + " in one quest transition");
			}
			// Force the capture check without treating the balance as a fabricated zero.
			snapshot.balance(set.kind());
		}
	}

	@Override
	public QuestTransactionParticipant applySets(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.SetCurrency> sets) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		if (sets.isEmpty()) {
			return QuestTransactionParticipant.none();
		}
		preflightSets(connection, snapshot, sets);
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			throw new SQLException("player is unavailable: " + snapshot.playerId());
		}
		QuestAction.SetCurrency set = sets.get(0);
		if (player.getCommonData() == null) {
			throw new SQLException("player common data is unavailable: " + snapshot.playerId());
		}
		final int target;
		try {
			target = Math.toIntExact(set.amount());
		} catch (ArithmeticException overflow) {
			throw new SQLException("DP balance exceeds live integer range for player " + snapshot.playerId(), overflow);
		}
		if (player.getCommonData().getDp() == target) {
			return QuestTransactionParticipant.none();
		}
		var commonSnapshot = player.getCommonData().transactionSnapshot();
		try {
			player.getCommonData().setDpSilently(target);
			if (player.getCommonData().getDp() != target) {
				throw new SQLException("live DP exact set was clamped or ignored for player " + snapshot.playerId());
			}
			playerDao.storeInTransaction(connection, player.getObjectId(), player.getCommonData());
			return QuestTransactionParticipant.of(
				player.getCommonData()::publishDp,
				commonSnapshot::restore);
		} catch (SQLException | RuntimeException failure) {
			try {
				commonSnapshot.restore();
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

	private static void validateSetTarget(QuestSnapshot snapshot, QuestAction.SetCurrency set) throws SQLException {
		if (set.kind() != QuestRewardKind.DP) {
			throw new SQLException("exact currency set is currently supported only for DP");
		}
		if (set.amount() < 0 || set.amount() > Integer.MAX_VALUE) {
			throw new SQLException("DP balance exceeds live integer range for player " + snapshot.playerId());
		}
		Integer maxDp = snapshot.maxDp();
		if (maxDp != null && set.amount() > maxDp) {
			throw new SQLException("DP balance exceeds live maximum for player " + snapshot.playerId());
		}
	}

	private static void validateGrantBalances(Player player, int playerId, long kinah, int ap, int gp, int dp)
			throws SQLException {
		try {
			if (kinah > 0) {
				Math.addExact(player.getInventory().getKinah(), kinah);
			}
			if (ap > 0) {
				Math.addExact(player.getAbyssRank().getAp(), ap);
				Math.addExact(player.getAbyssRank().getDailyAP(), ap);
				Math.addExact(player.getAbyssRank().getWeeklyAP(), ap);
			}
			if (gp > 0) {
				Math.addExact(player.getAbyssRank().getGp(), gp);
				Math.addExact(player.getAbyssRank().getDailyGP(), gp);
				Math.addExact(player.getAbyssRank().getWeeklyGP(), gp);
			}
			if (dp > 0) {
				Math.addExact((long) player.getCommonData().getDp(), dp);
			}
		} catch (NullPointerException missingBalance) {
			throw new SQLException("live currency balance is unavailable for player " + playerId, missingBalance);
		} catch (ArithmeticException overflow) {
			throw new SQLException("live currency balance would overflow for player " + playerId, overflow);
		}
	}

	private static QuestRewardKind canonicalCurrencyKind(QuestRewardKind kind) {
		return kind == QuestRewardKind.KINAH ? QuestRewardKind.GOLD : kind;
	}
}
