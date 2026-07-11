package com.aionemu.gameserver.model.atreian_bestiary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerABDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家 AB 列表，用于艾特里亚图鉴相关逻辑。
 * Player AB List for atreian bestiary logic.
 *
 * @author Ranastic
 */

public final class PlayerABList implements ABList<Player> {
	private final Map<Integer, PlayerABEntry> entry;

	public PlayerABList() {
		this.entry = new HashMap<Integer, PlayerABEntry>(0);
	}

	public PlayerABList(List<PlayerABEntry> entries) {
		this();
		for (PlayerABEntry e : entries) {
			entry.put(e.getId(), e);
		}
	}

	/** 返回 all ab / Returns the all ab */
	public PlayerABEntry[] getAllAB() {
		List<PlayerABEntry> allCp = new ArrayList<PlayerABEntry>();
		allCp.addAll(entry.values());
		return allCp.toArray(new PlayerABEntry[allCp.size()]);
	}

	/** 返回 basic ab / Returns the basic ab */
	public PlayerABEntry[] getBasicAB() {
		return entry.values().toArray(new PlayerABEntry[entry.size()]);
	}

	/** 添加。 / Add. */
	@Override
	public boolean add(Player player, int id, int killCount, int level, int claimReward) {
		return add(player, id, killCount, level, claimReward, PersistentState.NEW);
	}

	private synchronized boolean add(Player player, int id, int killCount, int level, int claimReward,
			PersistentState state) {
		entry.put(id, new PlayerABEntry(id, killCount, level, claimReward, state));
		DAOManager.getDAO(PlayerABDAO.class).store(player.getObjectId(), id, killCount, level, claimReward);
		return true;
	}

	/** 移除。 / Remove. */
	@Override
	public synchronized boolean remove(Player player, int id) {
		PlayerABEntry entries = entry.get(id);
		if (entries != null) {
			entries.setPersistentState(PersistentState.DELETED);
			entry.remove(id);
			DAOManager.getDAO(PlayerABDAO.class).delete(player.getObjectId(), id);
		}
		return entry != null;
	}

	/** 大小 / size. */
	@Override
	public int size() {
		return entry.size();
	}
}
