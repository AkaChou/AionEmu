package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Tracks inventory rows already written inside one caller-owned quest transaction. */
final class QuestInventoryPersistenceStage {
	private static final QuestInventoryPersistenceStage NONE = new QuestInventoryPersistenceStage();

	private final InventoryDAO inventoryDao;
	private final Player player;
	private final List<Item> items;
	private final Map<Item, PersistentState> originalStates;

	private QuestInventoryPersistenceStage() {
		this.inventoryDao = null;
		this.player = null;
		this.items = List.of();
		this.originalStates = Map.of();
	}

	private QuestInventoryPersistenceStage(InventoryDAO inventoryDao, Player player, List<Item> items,
			Map<Item, PersistentState> originalStates) {
		this.inventoryDao = inventoryDao;
		this.player = player;
		this.items = items;
		this.originalStates = originalStates;
	}

	static QuestInventoryPersistenceStage none() {
		return NONE;
	}

	static QuestInventoryPersistenceStage persist(InventoryDAO inventoryDao, Connection connection,
			Player player, List<Item> dirty) throws SQLException {
		Objects.requireNonNull(inventoryDao, "inventoryDao");
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(player, "player");
		List<Item> items = List.copyOf(Objects.requireNonNull(dirty, "dirty"));
		if (items.isEmpty()) {
			return NONE;
		}
		Map<Item, PersistentState> originalStates = new IdentityHashMap<>();
		for (Item item : items) {
			if (item != null) {
				originalStates.put(item, item.getPersistentState());
			}
		}
		inventoryDao.storeInTransaction(connection, items, player.getObjectId(), null, null);
		for (Map.Entry<Item, PersistentState> entry : originalStates.entrySet()) {
			if (entry.getValue() == PersistentState.NEW
					&& entry.getKey().getPersistentState() == PersistentState.NEW) {
				// The row now exists in the open JDBC transaction. A later mutation will
				// move UPDATED to UPDATE_REQUIRED, while an unchanged later port skips it.
				entry.getKey().setPersistentState(PersistentState.UPDATED);
			}
		}
		return new QuestInventoryPersistenceStage(inventoryDao, player, items, originalStates);
	}

	boolean isEmpty() {
		return items.isEmpty();
	}

	void afterCommit() {
		if (isEmpty()) {
			return;
		}
		inventoryDao.markStored(items);
		player.markDirtyItemContainersStored();
	}

	void afterRollback() {
		for (Map.Entry<Item, PersistentState> entry : originalStates.entrySet()) {
			entry.getKey().setPersistentState(entry.getValue());
		}
	}
}
