package com.aionemu.gameserver.model.autogroup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aionemu.commons.taskmanager.AbstractLockManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * LookingForParty，用于 autogroup 相关逻辑。
 * Looking For Party for autogroup logic.
 */

public class LookingForParty extends AbstractLockManager {
	private List<SearchInstance> searchInstances = new ArrayList<SearchInstance>();
	private Player player;
	private long startEnterTime;
	private long penaltyTime;

	public LookingForParty(Player player, int instanceMaskId, EntryRequestType ert) {
		this.player = player;
		searchInstances.add(new SearchInstance(instanceMaskId, ert,
				ert.isGroupEntry() ? player.getPlayerGroup2().getOnlineMembers() : null));
	}

	/** 注销副本。 / Unregister instance. */
	public int unregisterInstance(int instanceMaskId) {
		super.writeLock();
		try {
			for (Iterator<SearchInstance> iterator = searchInstances.iterator(); iterator.hasNext();) {
				SearchInstance si = iterator.next();
				if (si.getInstanceMaskId() == instanceMaskId) {
					iterator.remove();
					return searchInstances.size();
				}
			}
			return searchInstances.size();
		} finally {
			super.writeUnlock();
		}
	}

	/** 返回搜索副本列表 / Returns the search instances */
	public List<SearchInstance> getSearchInstances() {
		super.readLock();
		try {
			return new ArrayList<SearchInstance>(searchInstances);
		} finally {
			super.readUnlock();
		}
	}

	/** 添加 instance mask id / Adds instance mask id */
	public void addInstanceMaskId(int instanceMaskId, EntryRequestType ert) {
		super.writeLock();
		try {
			searchInstances.add(new SearchInstance(instanceMaskId, ert,
					ert.isGroupEntry() ? player.getPlayerGroup2().getOnlineMembers() : null));
		} finally {
			super.writeUnlock();
		}
	}

	/** 返回搜索副本 / Returns the search instance */
	public SearchInstance getSearchInstance(int instanceMaskId) {
		super.readLock();
		try {
			for (SearchInstance si : searchInstances) {
				if (si.getInstanceMaskId() == instanceMaskId) {
					return si;
				}
			}
			return null;
		} finally {
			super.readUnlock();
		}
	}

	/**
	 * @param instanceMaskId Whether registred instance / Whether registred instance
	 */
	public boolean isRegistredInstance(int instanceMaskId) {
		super.readLock();
		try {
			for (SearchInstance si : searchInstances) {
				if (si.getInstanceMaskId() == instanceMaskId) {
					return true;
				}
			}
			return false;
		} finally {
			super.readUnlock();
		}
	}

	/** 获取玩家。 / Returns the player. */
	public Player getPlayer() {
		return player;
	}

	/** 设置玩家。 / Sets the player. */
	public void setPlayer(Player player) {
		this.player = player;
	}

	/** 设置 penalty time / Sets the penalty time */
	public void setPenaltyTime() {
		penaltyTime = System.currentTimeMillis();
	}

	/**
	 * @return Whether penalty / Whether penalty
	 */
	public boolean hasPenalty() {
		return System.currentTimeMillis() - penaltyTime <= 10000;
	}

	/** 设置 start enter time / Sets the start enter time */
	public void setStartEnterTime() {
		startEnterTime = System.currentTimeMillis();
	}

	/** 是否开始进入任务时 / Whether on start enter task */
	public boolean isOnStartEnterTask() {
		return System.currentTimeMillis() - startEnterTime <= 120000;
	}
}
