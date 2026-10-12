package com.aionemu.gameserver.persistence.dao;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;
import com.aionemu.gameserver.persistence.entity.PlayerMotionPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家动作/表情动作数据访问层。
 * Data access layer for player motions.
 * @author MrPoke
 */
public interface PlayerMotionDao extends IService<PlayerMotionPo> {

	/**
	 * 按玩家 ID 加载动作列表。
	 * Loads the motion list by player ID.
	 * @param playerId 玩家 ID / player id
	 * @return 动作列表 / list of motions
	 */
	List<Motion> loadMotions(Integer playerId);

	/**
	 * 加载并填充玩家的动作列表。
	 * Loads and fills the player's motion list.
	 * @param player 玩家 / player
	 */
	void loadMotionList(Player player);

	/**
	 * 存储一条新动作。
	 * Stores a new motion entry.
	 * @param objectId 玩家对象 ID / player object id
	 * @param motion 动作 / motion
	 */
	void storeMotion(int objectId, Motion motion);

	/**
	 * 更新已有动作记录。
	 * Updates an existing motion record.
	 * @param objectId 玩家对象 ID / player object id
	 * @param motion 动作 / motion
	 */
	void updateMotion(int objectId, Motion motion);

	/**
	 * 删除指定动作。
	 * Deletes the given motion.
	 * @param objectId 玩家对象 ID / player object id
	 * @param motionId 动作 ID / motion id
	 */
	void deleteMotion(int objectId, int motionId);
}
