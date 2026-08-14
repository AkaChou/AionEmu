package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;

import java.util.List;

/**
 * 玩家动作/表情动作数据访问对象。
 * Player motion data access object.
 *
 * @author MrPoke
 * @rework: MATTY
 */
public abstract class MotionDAO implements DAO {

	/**
	 * 按玩家 ID 加载动作列表。
	 * Loads the motion list by player ID.
	 *
	 * @param playerId 玩家 ID / player id
	 * @return 动作列表 / list of motions
	 */
	public abstract List<Motion> loadMotions(Integer playerId);

	/**
	 * 加载并填充玩家的动作列表。
	 * Loads and fills the player's motion list.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void loadMotionList(Player player);

	/**
	 * 存储一条新动作。
	 * Stores a new motion entry.
	 *
	 * @param objectId 玩家对象 ID / player object id
	 * @param motion 动作 / motion
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean storeMotion(int objectId, Motion motion);

	/**
	 * 更新已有动作记录。
	 * Updates an existing motion record.
	 *
	 * @param objectId 玩家对象 ID / player object id
	 * @param motion 动作 / motion
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean updateMotion(int objectId, Motion motion);

	/**
	 * 删除指定动作。
	 * Deletes the given motion.
	 *
	 * @param objectId 玩家对象 ID / player object id
	 * @param motionId 动作 ID / motion id
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean deleteMotion(int objectId, int motionId);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public String getClassName() {
		return MotionDAO.class.getName();
	}
}
