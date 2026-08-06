package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.services.item.ItemService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Real {@link QuestInventoryPort}: removes required items from and grants quest
 * work items to the live player inventory, persisting dirty items through
 * {@link InventoryDAO}'s transaction-in-progress hook on the caller-owned
 * connection so the mutations commit atomically with the quest state.
 *
 * <p>Preflight fails closed when the snapshot did not capture inventory facts:
 * unknown counts are never guessed as zero.</p>
 */
public final class PlayerQuestInventoryPort implements QuestInventoryPort {
	private final QuestPlayerPort players;
	private final InventoryDAO inventoryDao;
	private final BiFunction<Player, List<QuestItems>, Boolean> itemAdder;

	public PlayerQuestInventoryPort(QuestPlayerPort players, InventoryDAO inventoryDao) {
		this(players, inventoryDao, ItemService::addQuestItems);
	}

	PlayerQuestInventoryPort(QuestPlayerPort players, InventoryDAO inventoryDao,
			BiFunction<Player, List<QuestItems>, Boolean> itemAdder) {
		this.players = Objects.requireNonNull(players, "players");
		this.inventoryDao = Objects.requireNonNull(inventoryDao, "inventoryDao");
		this.itemAdder = Objects.requireNonNull(itemAdder, "itemAdder");
	}

	@Override
	public void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) throws SQLException {
		preflight(connection, snapshot, removals, gives, List.of());
	}

	@Override
	public void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives,
			List<QuestAction.UnequipItem> unequips) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		Map<Integer, Integer> unequipped = new HashMap<>();
		Map<Integer, Integer> returnedRemovals = new HashMap<>();
		Map<Integer, Integer> plannedRemovals = new HashMap<>();
		if (!unequips.isEmpty() && snapshot.equipmentFacts() == null) {
			throw new SQLException("equipment facts are not captured for player " + snapshot.playerId());
		}
		for (QuestAction.UnequipItem unequip : unequips) {
			int count = snapshot.equipmentFacts().equippedItemCount(unequip.itemId());
			unequipped.put(unequip.itemId(), count);
			try {
				returnedRemovals.merge(unequip.itemId(), unequip.removeReturnedCount(), Math::addExact);
			} catch (ArithmeticException overflow) {
				throw new SQLException("returned item removal overflow for player " + snapshot.playerId(), overflow);
			}
		}
		for (QuestAction.RemoveItem removal : removals) {
			long available;
			try {
				available = Math.addExact(snapshot.itemCount(removal.itemId()),
					unequipped.getOrDefault(removal.itemId(), 0));
				long returned = Math.min(available, returnedRemovals.getOrDefault(removal.itemId(), 0));
				long remaining = available - returned - plannedRemovals.getOrDefault(removal.itemId(), 0);
				if (!removal.removeAll() && remaining < removal.count()) {
					throw new SQLException("insufficient item " + removal.itemId() + " for player " + snapshot.playerId());
				}
			} catch (ArithmeticException overflow) {
				throw new SQLException("inventory count overflow for player " + snapshot.playerId());
			} catch (IllegalStateException unknownFacts) {
				throw new SQLException("inventory facts are not captured for player " + snapshot.playerId());
			}
			try {
				if (removal.removeAll()) {
					plannedRemovals.put(removal.itemId(), Integer.MAX_VALUE);
				} else {
					plannedRemovals.merge(removal.itemId(), removal.count(), Math::addExact);
				}
			} catch (ArithmeticException overflow) {
				throw new SQLException("inventory removal overflow for player " + snapshot.playerId(), overflow);
			}
		}
		for (QuestAction.GiveItem give : gives) {
			if (give.count() <= 0) {
				throw new SQLException("invalid give item count for player " + snapshot.playerId());
			}
		}
	}

	@Override
	public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) throws SQLException {
		return apply(connection, snapshot, removals, gives, List.of());
	}

	@Override
	public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives,
			List<QuestAction.UnequipItem> unequips) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			throw new SQLException("player is unavailable: " + snapshot.playerId());
		}
		if (removals.isEmpty() && gives.isEmpty()) {
			return QuestTransactionParticipant.none();
		}
		var liveSnapshot = player.getInventory().transactionSnapshot();
		try {
			for (QuestAction.RemoveItem removal : removals) {
				int count = removal.removeAll()
					? (int) Math.min(Integer.MAX_VALUE, player.getInventory().getItemCountByItemId(removal.itemId()))
					: removal.count();
				if (count > 0 && !player.getInventory().decreaseByItemId(removal.itemId(), count)) {
					throw new SQLException("failed to remove item " + removal.itemId() + " for player " + snapshot.playerId());
				}
			}
			List<QuestItems> workItems = new ArrayList<>();
			for (QuestAction.GiveItem give : gives) {
				workItems.add(new QuestItems(give.itemId(), give.count()));
			}
			if (!workItems.isEmpty() && !itemAdder.apply(player, workItems)) {
				throw new SQLException("failed to give quest work items for player " + snapshot.playerId());
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
