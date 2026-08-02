package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Real {@link QuestInventoryPort}: removes required items from the live player
 * inventory and persists the dirty items through {@link InventoryDAO}'s
 * transaction-in-progress hook on the caller-owned connection, so the removal
 * commits atomically with the quest state and any other required mutations.
 *
 * <p>Preflight fails closed when the snapshot did not capture inventory facts:
 * unknown counts are never guessed as zero.</p>
 */
public final class PlayerQuestInventoryPort implements QuestInventoryPort {
	private final QuestPlayerPort players;
	private final InventoryDAO inventoryDao;

	public PlayerQuestInventoryPort(QuestPlayerPort players, InventoryDAO inventoryDao) {
		this.players = Objects.requireNonNull(players, "players");
		this.inventoryDao = Objects.requireNonNull(inventoryDao, "inventoryDao");
	}

	@Override
	public void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		for (QuestAction.RemoveItem removal : removals) {
			int available;
			try {
				available = snapshot.itemCount(removal.itemId());
			} catch (IllegalStateException unknownFacts) {
				throw new SQLException("inventory facts are not captured for player " + snapshot.playerId());
			}
			if (available < removal.count()) {
				throw new SQLException("insufficient item " + removal.itemId() + " for player " + snapshot.playerId());
			}
		}
	}

	@Override
	public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			throw new SQLException("player is unavailable: " + snapshot.playerId());
		}
		if (removals.isEmpty()) {
			return QuestTransactionParticipant.none();
		}
		var liveSnapshot = player.getInventory().transactionSnapshot();
		try {
			for (QuestAction.RemoveItem removal : removals) {
				if (!player.getInventory().decreaseByItemId(removal.itemId(), removal.count())) {
					throw new SQLException("failed to remove item " + removal.itemId() + " for player " + snapshot.playerId());
				}
			}
			List<Item> dirty = List.copyOf(player.getDirtyItemsToUpdate());
			if (!dirty.isEmpty()) {
				inventoryDao.storeInTransaction(connection, dirty, snapshot.playerId(), null, null);
			}
			return QuestTransactionParticipant.of(() -> {
				inventoryDao.markStored(dirty);
				player.markDirtyItemContainersStored();
			}, liveSnapshot::restore);
		} catch (SQLException | RuntimeException failure) {
			try {
				liveSnapshot.restore();
			} catch (RuntimeException restoreFailure) {
				failure.addSuppressed(restoreFailure);
			}
			throw failure;
		}
	}
}
