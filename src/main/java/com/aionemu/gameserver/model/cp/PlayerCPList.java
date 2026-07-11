package com.aionemu.gameserver.model.cp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerCreativityPointsDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家创造点列表，用于创造点相关逻辑。
 * Player CP List for cp logic.
 */

public final class PlayerCPList implements CPList<Player> {

	private final Map<Integer, PlayerCPEntry> entry;

	public PlayerCPList() {
		this.entry = new HashMap<Integer, PlayerCPEntry>(0);
	}

	public PlayerCPList(List<PlayerCPEntry> entries) {
		this();
		for (PlayerCPEntry e : entries) {
			entry.put(e.getSlot(), e);
		}
	}

	/** 返回全部创意点 / Returns the all cp*/
	public PlayerCPEntry[] getAllCP() {
		List<PlayerCPEntry> allCp = new ArrayList<PlayerCPEntry>();
		allCp.addAll(entry.values());
		return allCp.toArray(new PlayerCPEntry[allCp.size()]);
	}

	/** 返回基础创意点 / Returns the basic cp */
	public PlayerCPEntry[] getBasicCP() {
		return entry.values().toArray(new PlayerCPEntry[entry.size()]);
	}

	/** 添加点。 / Adds point. */
	@Override
	public boolean addPoint(Player player, int slot, int point) {
		return addPoint(player, slot, point, PersistentState.NEW);
	}

	private synchronized boolean addPoint(Player player, int slot, int point, PersistentState state) {
		entry.put(slot, new PlayerCPEntry(slot, point, state));
		DAOManager.getDAO(PlayerCreativityPointsDAO.class).storeCP(player.getObjectId(), slot, point);
		return true;
	}

	/** 移除点。 / Removes point. */
	@Override
	public synchronized boolean removePoint(Player player, int slot) {
		PlayerCPEntry entries = entry.get(slot);
		if (entries != null) {
			entries.setPersistentState(PersistentState.DELETED);
			entry.remove(slot);
			DAOManager.getDAO(PlayerCreativityPointsDAO.class).deleteCP(player.getObjectId(), slot);
		}
		return entry != null;
	}

	/** 大小 / size. */
	@Override
	public int size() {
		return entry.size();
	}
}
