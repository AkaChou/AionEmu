package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家术古扫荡（Shugo Sweep）数据访问抽象层。
 * DAO for player Shugo Sweep event data persistence.
 *
 * Created by Wnkrz on 24/10/2017.
 */
public abstract class PlayerShugoSweepDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public String getClassName() {
		return PlayerShugoSweepDAO.class.getName();
	}

	/**
	 * 加载玩家术古扫荡数据。
	 * Loads Shugo Sweep data for the player.
	 *
	 * 玩家 / player
	 */
	public abstract void load(Player player);

	/**
	 * 新增玩家术古扫荡记录。
	 * Adds a Shugo Sweep record for the player.
	 *
	 * player object id
	 * @param freeDice 免费骰子数 / free dice count
	 * @param step 当前步数 / current step
	 * board id
	 * @return 是否添加成功 / true if added
	 */
	public abstract boolean add(final int playerId, int freeDice, int step, int boardId);

	/**
	 * 删除全部术古扫荡数据。
	 * Deletes all Shugo Sweep data.
	 *
	 * @return 是否删除成功 / true if deleted
	 */
	public abstract boolean delete();

	/**
	 * 保存玩家术古扫荡数据。
	 * Stores Shugo Sweep data for the player.
	 *
	 * 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean store(Player player);

	/**
	 * 按对象 ID 更新术古扫荡状态。
	 * Updates Shugo Sweep state by player object id.
	 *
	 * @param obj 玩家对象 ID / player object id
	 * @param freeDice 免费骰子数 / free dice count
	 * @param step 当前步数 / current step
	 * board id
	 * @return 是否更新成功 / true if updated
	 */
	public abstract boolean setShugoSweepByObjId(final int obj, int freeDice, int step, int boardId);
}
