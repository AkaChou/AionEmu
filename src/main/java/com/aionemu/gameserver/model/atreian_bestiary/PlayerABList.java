package com.aionemu.gameserver.model.atreian_bestiary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.player.PlayerService;

/**
 * 玩家 AB 列表，用于艾特里亚图鉴相关逻辑。
 * Player AB List for atreian bestiary logic.
 *
 * @author Ranastic
 */

public final class PlayerABList {
	private final Map<Integer, PlayerABEntry> entry;

	public PlayerABList() {
		this.entry = new HashMap<>(0);
	}

	public PlayerABList(List<PlayerABEntry> entries) {
		this();
		for (PlayerABEntry e : entries) {
			entry.put(e.getId(), e);
		}
	}

	/**
	 * 返回全部 AB 条目。 / Returns all AB entries.
	 */
	public PlayerABEntry[] getAllAB() {
		return entry.values().toArray(new PlayerABEntry[0]);
	}

	/**
	 * 添加。 / Add.
	 */
	public synchronized boolean add(Player player, int id, int killCount, int level, int claimReward) {
		entry.put(id, new PlayerABEntry(id, killCount, level, claimReward, PersistentState.NEW));
		PlayerService.daos().playerABDAO().store(player.getObjectId(), id, killCount, level, claimReward);
		return true;
	}

	/**
	 * 移除。 / Remove.
	 */
	public synchronized boolean remove(Player player, int id) {
		PlayerABEntry entries = entry.get(id);
		if (entries != null) {
			entries.setPersistentState(PersistentState.DELETED);
			entry.remove(id);
			PlayerService.daos().playerABDAO().delete(player.getObjectId(), id);
		}
		return entries != null;
	}

	/**
	 * 返回条目数。 / Returns the number of entries.
	 */
	public int size() {
		return entry.size();
	}
}
