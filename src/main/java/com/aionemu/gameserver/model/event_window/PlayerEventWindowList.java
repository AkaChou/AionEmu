package com.aionemu.gameserver.model.event_window;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerEventsWindowDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家活动窗口列表，用于活动窗口相关逻辑。
 * Player Event Window List for event window logic.
 *
 * @author Ranastic
 */
public class PlayerEventWindowList implements EventWindowList<Player> {

	private final Map<Integer, PlayerEventWindowEntry> entry = new HashMap<>(0);

	public PlayerEventWindowList(List<PlayerEventWindowEntry> list) {
		for (PlayerEventWindowEntry playerEventWindowEntry : list) {
			entry.put(playerEventWindowEntry.getId(), playerEventWindowEntry);
		}
	}

	/** 返回全部 / Returns the all*/
	public PlayerEventWindowEntry[] getAll() {
		ArrayList<PlayerEventWindowEntry> arrayList = new ArrayList<PlayerEventWindowEntry>(entry.values());
		return arrayList.toArray(new PlayerEventWindowEntry[arrayList.size()]);
	}

	/** 返回基础 / Returns the basic*/
	public PlayerEventWindowEntry[] getBasic() {
		return entry.values().toArray(new PlayerEventWindowEntry[entry.size()]);
	}

	/**
	 * 添加玩家 eventwindow 列表。 / add player event window list
	 */
	private synchronized boolean add(Player player, int remaining, Timestamp timestamp, int Time,
			PersistentState persistentState) {
		entry.put(remaining, new PlayerEventWindowEntry(remaining, timestamp, Time, persistentState));
		DAOManager.getDAO(PlayerEventsWindowDAO.class).store(player.getPlayerAccount().getId(), remaining, timestamp,
				Time);
		return true;
	}

	/** 添加。 / Add. */
	@Override
	public boolean add(Player player, int remaining, Timestamp timestamp, int Time) {
		return add(player, remaining, timestamp, Time, PersistentState.NEW);
	}

	/**
	 * 移除玩家 eventwindow 列表。 / remove player event window list
	 */
	@Override
	public synchronized boolean remove(Player player, int remaining) {
		PlayerEventWindowEntry playerEventWindowEntry = entry.get(remaining);
		if (playerEventWindowEntry != null) {
			playerEventWindowEntry.setPersistentState(PersistentState.DELETED);
			entry.remove(remaining);
			DAOManager.getDAO(PlayerEventsWindowDAO.class).delete(player.getPlayerAccount().getId(), remaining);
		}
		return entry != null;
	}

	/**
	 * size player event window list
	 */
	@Override
	public int size() {
		return entry.size();
	}
}
