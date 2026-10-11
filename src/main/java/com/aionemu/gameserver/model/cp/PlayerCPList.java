package com.aionemu.gameserver.model.cp;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.player.PlayerService;

/**
 * 玩家创造点列表，用于创造点相关逻辑。
 * Player CP List for cp logic.
 */
public final class PlayerCPList {

	private final Map<Integer, PlayerCPEntry> entry;

	public PlayerCPList() {
		this.entry = new HashMap<>(0);
	}

	public PlayerCPList(List<PlayerCPEntry> entries) {
		this();
		for (PlayerCPEntry e : entries) {
			entry.put(e.getSlot(), e);
		}
	}

	/**
	 * 返回全部创意点 / Returns the all cp
	 */
	public PlayerCPEntry[] getAllCP() {
		return entry.values().toArray(new PlayerCPEntry[0]);
	}

	/**
	 * 返回基础创意点 / Returns the basic cp
	 */
	public PlayerCPEntry[] getBasicCP() {
		return entry.values().toArray(new PlayerCPEntry[0]);
	}

	/**
	 * Captures the live creativity-point entries for caller-owned transaction rollback.
	 */
	public synchronized TransactionSnapshot transactionSnapshot() {
		return new TransactionSnapshot();
	}

	public final class TransactionSnapshot {
		private final Map<Integer, PlayerCPEntry> entries = new HashMap<>(entry);
		private final Map<PlayerCPEntry, PersistentState> states = new IdentityHashMap<>();
		private boolean restored;

		private TransactionSnapshot() {
			for (PlayerCPEntry value : entries.values()) {
				states.put(value, value.getPersistentState());
			}
		}

		/**
		 * Restores CP membership and persistence states exactly once.
		 */
		public void restore() {
			synchronized (PlayerCPList.this) {
				if (restored) {
					return;
				}
				restored = true;
				entry.clear();
				entry.putAll(entries);
				states.forEach(PlayerCPEntry::restorePersistentState);
			}
		}
	}

	/**
	 * 添加点。 / Adds point.
	 */
	public synchronized void addPoint(Player player, int slot, int point) {
		entry.put(slot, new PlayerCPEntry(slot, point, PersistentState.NEW));
		PlayerService.daos().playerCreativityPointsDAO().storeCP(player.getObjectId(), slot, point);
return;
	}

	/**
	 * 移除点。 / Removes point.
	 */
	public synchronized void removePoint(Player player, int slot) {
		PlayerCPEntry entries = entry.get(slot);
		if (entries != null) {
			entries.setPersistentState(PersistentState.DELETED);
			entry.remove(slot);
			PlayerService.daos().playerCreativityPointsDAO().deleteCP(player.getObjectId(), slot);
		}
return;
	}

	/**
	 * 大小 / size.
	 */
	public int size() {
		return entry.size();
	}
}
