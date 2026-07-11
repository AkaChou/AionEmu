package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import java.util.Collection;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerMinionsDAO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MINIONS;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 守护灵列表。
 * Minion List game object.
 */

public class MinionList {
	private final Player player;
	private int lastUsedObjId;
	private Map<Integer, MinionCommonData> minions = new LinkedHashMap<Integer, MinionCommonData>();

	public MinionList(Player player) {
		this.player = player;
		loadMinions();
	}

	/** Load 守护灵 / Load minions */
	public void loadMinions() {
		for (MinionCommonData minionCommonData : DAOManager.getDAO(PlayerMinionsDAO.class).getPlayerMinions(player)) {
			if (minionCommonData.getExpireTime() > 0) {
				GameTaskManagerServices.expireTimerTask().addTask(minionCommonData, player);
			}
			minions.put(minionCommonData.getObjectId(), minionCommonData);
		}
	}

	/** 返回 minions / Returns the minions */
	public Collection<MinionCommonData> getMinions() {
		return (Collection<MinionCommonData>) minions.values();
	}

	/** 更新守护灵列表 / Update minions list */
	public void updateMinionsList() {
		minions.clear();
		for (MinionCommonData minionCommonData : DAOManager.getDAO(PlayerMinionsDAO.class).getPlayerMinions(player)) {
			minions.put(minionCommonData.getObjectId(), minionCommonData);
		}
		if (minions != null) {
			PacketSendUtility.sendPacket(player, new SM_MINIONS(0, player.getMinionList().getMinions()));
		}
		return;
	}

	/** 获取守护灵。 / Returns the minion. */
	public MinionCommonData getMinion(int minionObjId) {
		return minions.get(minionObjId);
	}

	// 添加成长点 / Add growthPoint
	/** 添加 new minion / Adds new minion */
	public MinionCommonData addNewMinion(Player player, int minionId, String name, String grade, int level, int growthPoint) {
		MinionCommonData minionCommonData = new MinionCommonData(minionId, player.getObjectId(), name, grade, level, growthPoint);
		DAOManager.getDAO(PlayerMinionsDAO.class).insertPlayerMinion(minionCommonData);
		DAOManager.getDAO(PlayerMinionsDAO.class).saveBirthday(minionCommonData);
		minions.put(minionId, minionCommonData);
		return minionCommonData;
	}

	/** 是否拥有守护灵。 / Whether minion. */
	public boolean hasMinion(int n) {
		return minions.containsKey(n);
	}

	/** 删除守护灵。 / Deletes minion. */
	public void deleteMinion(int minionObjId) {
		if (hasMinion(minionObjId)) {
			DAOManager.getDAO(PlayerMinionsDAO.class).removePlayerMinion(player, minionObjId);
			minions.remove(minionObjId);
		}
	}

	/** 设置 last used / Sets the last used */
	public void setLastUsed(int lastUsedObjId) {
		this.lastUsedObjId = lastUsedObjId;
	}

	/** 返回上次已用 / Returns the last used*/
	public int getLastUsed() {
		return lastUsedObjId;
	}
}
