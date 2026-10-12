package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerShugoSweepPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家术古扫荡（Shugo Sweep）数据访问抽象层。
 * DAO for player Shugo Sweep event data persistence.
 * Created by Wnkrz on 24/10/2017.
 */
public interface PlayerShugoSweepDao extends IService<PlayerShugoSweepPo> {

	/**
	 * 加载玩家术古扫荡数据。
	 * Loads Shugo Sweep data for the player.
	 * @param player 玩家 / player
	 */
	void load(Player player);

	/**
	 * 新增玩家术古扫荡记录。
	 * Adds a Shugo Sweep record for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param freeDice 免费骰子数 / free dice count
	 * @param step 当前步数 / current step
	 * @param boardId 排行榜 ID / board id
	 * @return 是否添加成功 / true if added
	 */
	boolean add(int playerId, int freeDice, int step, int boardId);

	/**
	 * 删除全部术古扫荡数据。
	 * Deletes all Shugo Sweep data.
	 * @return 是否删除成功 / true if deleted
	 */
	boolean delete();

	/**
	 * 保存玩家术古扫荡数据。
	 * Stores Shugo Sweep data for the player.
	 * @param player 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	boolean store(Player player);

	/**
	 * 按对象 ID 更新术古扫荡状态。
	 * Updates Shugo Sweep state by player object id.
	 * @param obj 玩家对象 ID / player object id
	 * @param freeDice 免费骰子数 / free dice count
	 * @param step 当前步数 / current step
	 * @param boardId 排行榜 ID / board id
	 */
	void setShugoSweepByObjId(int obj, int freeDice, int step, int boardId);
}
