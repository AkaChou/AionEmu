package com.aionemu.gameserver.model.skill.linked_skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Set;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家 EquippedStigma 列表，用于技能相关逻辑。
 * Player Equipped Stigma List for skill logic.
 *
 * @author Ranastic
 */
public final class PlayerEquippedStigmaList implements StigmaList<Player> {

	private Map<Integer, EquippedStigmasEntry> itemList;
	private List<EquippedStigmasEntry> deletedItems;

	public PlayerEquippedStigmaList() {
		this.itemList = new HashMap<Integer, EquippedStigmasEntry>(0);
		this.deletedItems = new ArrayList<EquippedStigmasEntry>(0);
	}

	public PlayerEquippedStigmaList(List<EquippedStigmasEntry> items) {
		this();
		for (EquippedStigmasEntry entry : items) {
			itemList.put(entry.getItemId(), entry);
		}
	}

	/** 返回全部物品 / Returns the all items*/
	public EquippedStigmasEntry[] getAllItems() {
		List<EquippedStigmasEntry> allItems = new ArrayList<EquippedStigmasEntry>();
		allItems.addAll(itemList.values());
		return allItems.toArray(new EquippedStigmasEntry[allItems.size()]);
	}

	/** 返回 all items as integer / Returns the all items as integer */
	public List<Integer> getAllItemsAsInteger() {
		HashSet<Integer> equippedIds = new HashSet<Integer>();
		for (EquippedStigmasEntry i : itemList.values()) {
			equippedIds.add(i.getItemId());
		}
		return Arrays.asList(equippedIds.toArray(new Integer[0]));
	}

	/** 返回 deleted items / Returns the deleted items */
	public EquippedStigmasEntry[] getDeletedItems() {
		return deletedItems.toArray(new EquippedStigmasEntry[deletedItems.size()]);
	}

	/** 添加物品。 / Adds item. */
	@Override
	public boolean addItem(Player player, int itemId, String itemName) {
		return addItem(player, itemId, itemName, PersistentState.NEW);
	}

	private synchronized boolean addItem(Player player, int itemId, String itemName, PersistentState state) {
		itemList.put(itemId, new EquippedStigmasEntry(itemId, itemName, state));
		DAOManager.getDAO(PlayerStigmasEquippedDAO.class).storeItems(player);
		return true;
	}

	/** 移除。 / Remove. */
	@Override
	public boolean remove(Player player, int itemId) {
		boolean removed = removeInTransaction(itemId);
		if (player != null) {
			DAOManager.getDAO(PlayerStigmasEquippedDAO.class).storeItems(player);
		}
		return removed;
	}

	/** Removes one entry in memory so the caller can persist it on an existing transaction. */
	public synchronized boolean removeInTransaction(int itemId) {
		EquippedStigmasEntry entry = itemList.get(itemId);
		if (entry != null) {
			entry.setPersistentState(PersistentState.DELETED);
			deletedItems.add(entry);
			itemList.remove(itemId);
		}
		return entry != null;
	}

	/** Marks all changes stored after the owning JDBC transaction commits. */
	public synchronized void markStored() {
		for (EquippedStigmasEntry entry : itemList.values()) {
			entry.setPersistentState(PersistentState.UPDATED);
		}
		for (EquippedStigmasEntry entry : deletedItems) {
			entry.setPersistentState(PersistentState.UPDATED);
		}
	}

	/** Captures list ownership and entry states for caller-owned transaction rollback. */
	public synchronized TransactionSnapshot transactionSnapshot() {
		return new TransactionSnapshot();
	}

	public final class TransactionSnapshot {
		private final Map<Integer, EquippedStigmasEntry> items = new HashMap<>(itemList);
		private final List<EquippedStigmasEntry> deleted = new ArrayList<>(deletedItems);
		private final Map<EquippedStigmasEntry, PersistentState> states = new IdentityHashMap<>();
		private boolean restored;

		private TransactionSnapshot() {
			Set<EquippedStigmasEntry> entries = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
			entries.addAll(itemList.values());
			entries.addAll(deletedItems);
			for (EquippedStigmasEntry entry : entries) {
				states.put(entry, entry.getPersistentState());
			}
		}

		public void restore() {
			synchronized (PlayerEquippedStigmaList.this) {
				if (restored) {
					return;
				}
				restored = true;
				itemList.clear();
				itemList.putAll(items);
				deletedItems.clear();
				deletedItems.addAll(deleted);
				states.forEach(EquippedStigmasEntry::restorePersistentState);
			}
		}
	}

	/** 是否物品存在 / Whether item present*/
	@Override
	public boolean isItemPresent(int itemId) {
		return itemList.containsKey(itemId);
	}

	/** 大小 / size. */
	@Override
	public int size() {
		return itemList.size();
	}
}
