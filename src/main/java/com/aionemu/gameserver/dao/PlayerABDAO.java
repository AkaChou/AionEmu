package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABList;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家阿特里亚图鉴（Atreian Bestiary）数据访问对象。
 * Player Atreian Bestiary data access object.
 *
 * @author Ranastic
 */
public abstract class PlayerABDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public String getClassName() {
		return PlayerABDAO.class.getName();
	}

	/**
	 * 加载玩家图鉴列表。
	 * Loads the player's Atreian Bestiary list.
	 *
	 * @param paramPlayer 玩家 / player
	 * @return 图鉴列表 / bestiary list
	 */
	public abstract PlayerABList load(Player paramPlayer);

	/**
	 * 存储一条图鉴进度记录。
	 * Stores a bestiary progress entry.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @param kill_count 击杀数 / kill count
	 * @param level 等级 / level
	 * @param levelUpable 是否可升级 / whether level-upable
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean store(int playerObjId, int id, int kill_count, int level, int levelUpable);

	/**
	 * 删除指定槽位的图鉴记录。
	 * Deletes a bestiary record for the given slot.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean delete(int playerObjId, int slot);

	/**
	 * 获取指定图鉴条目的击杀数。
	 * Returns the kill count for the given bestiary entry.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @return 击杀数 / kill count
	 */
	public abstract int getKillCountById(int playerObjId, int id);

	/**
	 * 获取指定图鉴条目的等级。
	 * Returns the level for the given bestiary entry.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @return 等级 / level
	 */
	public abstract int getLevelById(int playerObjId, int id);

	/**
	 * 获取指定图鉴条目的领奖状态。
	 * Returns the claim-reward status for the given bestiary entry.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @return 领奖状态值 / claim-reward value
	 */
	public abstract int getClaimRewardById(int playerObjId, int id);
}
