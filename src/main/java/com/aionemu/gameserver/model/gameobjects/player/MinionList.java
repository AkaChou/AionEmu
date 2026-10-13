package com.aionemu.gameserver.model.gameobjects.player;


import java.util.Collection;
import java.util.List;

import com.aionemu.gameserver.services.player.PlayerService;

import java.util.LinkedHashMap;
import java.util.Map;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.utils.idfactory.IDFactory;

/**
 * 守护灵列表。
 * Minion List game object.
 */

public class MinionList {
	public static final String LAST_USED_VAR = "minion.last_used_object_id";
	private final Player player;
	private int lastUsedObjId;
	private final Map<Integer, MinionCommonData> minions = new LinkedHashMap<>();

	public MinionList(Player player) {
		this.player = player;
		loadMinions();
	}

	/**
	 * Load 守护灵 / Load minions
	 */
	public void loadMinions() {
		List<MinionCommonData> loadedMinions = PlayerService.daos().playerMinionsDAO().getPlayerMinions(player);
		if (loadedMinions == null) {
			return;
		}
		for (MinionCommonData minionCommonData : loadedMinions) {
			if (minionCommonData.getExpireTime() > 0) {
				ExpireTimerTask.getInstance().addTask(minionCommonData, player);
			}
			minions.put(minionCommonData.getObjectId(), minionCommonData);
		}
	}

	/**
	 * 返回 minions / Returns the minions
	 */
	public Collection<MinionCommonData> getMinions() {
		return minions.values();
	}

	/**
	 * 获取守护灵。 / Returns the minion.
	 */
	public MinionCommonData getMinion(int minionObjId) {
		return minions.get(minionObjId);
	}

	// 添加成长点 / Add growthPoint

	/**
	 * 添加 new minion / Adds new minion
	 */
	public MinionCommonData addNewMinion(Player player, int minionId, String name, String grade, int level, int growthPoint) {
		MinionCommonData minionCommonData = new MinionCommonData(minionId, player.getObjectId(), name, grade, level, growthPoint);
		if (!PlayerService.daos().playerMinionsDAO().insertPlayerMinion(minionCommonData)) {
			IDFactory.getInstance().releaseId(minionCommonData.getObjectId());
			return null;
		}
		PlayerService.daos().playerMinionsDAO().saveBirthday(minionCommonData);
		minions.put(minionCommonData.getObjectId(), minionCommonData);
		return minionCommonData;
	}

	public MinionCommonData replaceWithCombinedMinion(int minionId, String name, String grade, int level, int growthPoint,
													  List<Integer> materialObjectIds) {
		MinionCommonData replacement = new MinionCommonData(minionId, player.getObjectId(), name, grade, level, growthPoint);
		if (!PlayerService.daos().playerMinionsDAO().replacePlayerMinions(replacement, materialObjectIds)) {
			IDFactory.getInstance().releaseId(replacement.getObjectId());
			return null;
		}
		PlayerService.daos().playerMinionsDAO().saveBirthday(replacement);
		for (int materialObjectId : materialObjectIds) {
			removeFromMemory(materialObjectId);
		}
		minions.put(replacement.getObjectId(), replacement);
		return replacement;
	}

	/**
	 * 是否拥有守护灵。 / Whether minion.
	 */
	public boolean hasMinion(int n) {
		return minions.containsKey(n);
	}

	/**
	 * 删除守护灵。 / Deletes minion.
	 */
	public boolean deleteMinion(int minionObjId) {
		if (!hasMinion(minionObjId) || !PlayerService.daos().playerMinionsDAO().removePlayerMinion(player, minionObjId)) {
			return false;
		}
		removeFromMemory(minionObjId);
		return true;
	}

	public void removeFromMemory(int minionObjId) {
		if (minions.remove(minionObjId) != null) {
			IDFactory.getInstance().releaseId(minionObjId);
		}
	}

	/**
	 * 设置 last used / Sets the last used
	 */
	public void setLastUsed(int lastUsedObjId) {
		this.lastUsedObjId = lastUsedObjId;
	}
}
