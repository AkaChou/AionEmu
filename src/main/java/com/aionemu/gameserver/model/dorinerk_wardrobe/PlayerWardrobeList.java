package com.aionemu.gameserver.model.dorinerk_wardrobe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerWardrobeDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家衣橱列表，用于多里纳克衣橱相关逻辑。
 * Player Wardrobe List for dorinerk wardrobe logic.
 *
 * @author Ranastic
 */
public final class PlayerWardrobeList {

	private final Map<Integer, PlayerWardrobeEntry> entry;

	public PlayerWardrobeList() {
		this.entry = new HashMap<>(0);
	}

	public PlayerWardrobeList(List<PlayerWardrobeEntry> entries) {
		this();
		for (PlayerWardrobeEntry e : entries) {
			entry.put(e.getItemId(), e);
		}
	}

	/** 返回全部衣橱。 / Returns the all wardrobe. */
	public PlayerWardrobeEntry[] getAllWardrobe() {
		List<PlayerWardrobeEntry> allWardrobe = new ArrayList<>();
		allWardrobe.addAll(entry.values());
		return allWardrobe.toArray(new PlayerWardrobeEntry[allWardrobe.size()]);
	}

	/** 返回基础衣橱。 / Returns the basic wardrobe. */
	public PlayerWardrobeEntry[] getBasicWardrobe() {
		return entry.values().toArray(new PlayerWardrobeEntry[entry.size()]);
	}

	/** 添加物品。 / Adds item. */
	public boolean addItem(Player player, int itemId, int slot, int reskin_count) {
		return addItem(player, itemId, slot, reskin_count, PersistentState.NEW);
	}

	private synchronized boolean addItem(Player player, int itemId, int slot, int reskin_count, PersistentState state) {
		entry.put(itemId, new PlayerWardrobeEntry(itemId, slot, reskin_count, state));
		DAOManager.getDAO(PlayerWardrobeDAO.class).store(player.getObjectId(), itemId, slot, reskin_count);
		return true;
	}

	/** 移除物品。 / Removes item. */
	public synchronized boolean removeItem(Player player, int itemId) {
		PlayerWardrobeEntry entries = entry.get(itemId);
		if (entries != null) {
			entries.setPersistentState(PersistentState.DELETED);
			entry.remove(itemId);
			DAOManager.getDAO(PlayerWardrobeDAO.class).delete(player.getObjectId(), itemId);
		}
		return entry != null;
	}

	/** 大小。 / Size. */
	public int size() {
		return entry.size();
	}
}
