package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.Collection;
import java.util.List;

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
	public static final String LAST_USED_VAR = "minion.last_used_object_id";
	private final Player player;
	private int lastUsedObjId;
	private final Map<Integer, MinionCommonData> minions = new LinkedHashMap<Integer, MinionCommonData>();

	public MinionList(Player player) {
		this.player = player;
		loadMinions();
	}

	/** Load 守护灵 / Load minions */
	public void loadMinions() {
		List<MinionCommonData> loadedMinions = DAOManager.getDAO(PlayerMinionsDAO.class).getPlayerMinions(player);
		if (loadedMinions == null) {
			return;
		}
		for (MinionCommonData minionCommonData : loadedMinions) {
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
		List<MinionCommonData> loadedMinions = DAOManager.getDAO(PlayerMinionsDAO.class).getPlayerMinions(player);
		if (loadedMinions == null) {
			return;
		}
		minions.clear();
		for (MinionCommonData minionCommonData : loadedMinions) {
			minions.put(minionCommonData.getObjectId(), minionCommonData);
		}
		PacketSendUtility.sendPacket(player, new SM_MINIONS(0, getMinions()));
	}

	/** 获取守护灵。 / Returns the minion. */
	public MinionCommonData getMinion(int minionObjId) {
		return minions.get(minionObjId);
	}

	// 添加成长点 / Add growthPoint
	/** 添加 new minion / Adds new minion */
	public MinionCommonData addNewMinion(Player player, int minionId, String name, String grade, int level, int growthPoint) {
		MinionCommonData minionCommonData = new MinionCommonData(minionId, player.getObjectId(), name, grade, level, growthPoint);
		if (!DAOManager.getDAO(PlayerMinionsDAO.class).insertPlayerMinion(minionCommonData)) {
			GameWorldBootstrapServices.idFactory().releaseId(minionCommonData.getObjectId());
			return null;
		}
		DAOManager.getDAO(PlayerMinionsDAO.class).saveBirthday(minionCommonData);
		minions.put(minionCommonData.getObjectId(), minionCommonData);
		return minionCommonData;
	}

	public MinionCommonData replaceWithCombinedMinion(int minionId, String name, String grade, int level, int growthPoint,
			List<Integer> materialObjectIds) {
		MinionCommonData replacement = new MinionCommonData(minionId, player.getObjectId(), name, grade, level, growthPoint);
		if (!DAOManager.getDAO(PlayerMinionsDAO.class).replacePlayerMinions(replacement, materialObjectIds)) {
			GameWorldBootstrapServices.idFactory().releaseId(replacement.getObjectId());
			return null;
		}
		DAOManager.getDAO(PlayerMinionsDAO.class).saveBirthday(replacement);
		for (int materialObjectId : materialObjectIds) {
			removeFromMemory(materialObjectId);
		}
		minions.put(replacement.getObjectId(), replacement);
		return replacement;
	}

	/** 是否拥有守护灵。 / Whether minion. */
	public boolean hasMinion(int n) {
		return minions.containsKey(n);
	}

	/** 删除守护灵。 / Deletes minion. */
	public boolean deleteMinion(int minionObjId) {
		if (!hasMinion(minionObjId) || !DAOManager.getDAO(PlayerMinionsDAO.class).removePlayerMinion(player, minionObjId)) {
			return false;
		}
		removeFromMemory(minionObjId);
		return true;
	}

	public void removeFromMemory(int minionObjId) {
		if (minions.remove(minionObjId) != null) {
			GameWorldBootstrapServices.idFactory().releaseId(minionObjId);
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
