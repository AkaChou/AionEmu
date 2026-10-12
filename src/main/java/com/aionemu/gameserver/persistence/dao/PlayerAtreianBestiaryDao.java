package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.atreian_bestiary.PlayerABList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerAtreianBestiaryPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家阿特里亚图鉴（Atreian Bestiary）数据访问层。
 * Player Atreian Bestiary data access layer.
 * @author Ranastic
 */
public interface PlayerAtreianBestiaryDao extends IService<PlayerAtreianBestiaryPo> {

	/**
	 * 加载玩家图鉴列表。
	 * Loads the player's Atreian Bestiary list.
	 * @param player 玩家 / player
	 * @return 图鉴列表 / bestiary list
	 */
	PlayerABList load(Player player);

	/**
	 * 存储一条图鉴进度记录。
	 * Stores a bestiary progress entry.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @param killCount 击杀数 / kill count
	 * @param level 等级 / level
	 * @param levelUpable 是否可升级 / whether level-upable
	 * @return 若成功则为 true / true if successful
	 */
	boolean store(int playerObjId, int id, int killCount, int level, int levelUpable);

	/**
	 * 删除指定 ID 的图鉴记录。
	 * Deletes a bestiary record for the given id.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @return 若成功则为 true / true if successful
	 */
	boolean delete(int playerObjId, int id);

	/**
	 * 获取指定图鉴条目的击杀数。
	 * Returns the kill count for the given bestiary entry.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @return 击杀数 / kill count
	 */
	int getKillCountById(int playerObjId, int id);

	/**
	 * 获取指定图鉴条目的等级。
	 * Returns the level for the given bestiary entry.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @return 等级 / level
	 */
	int getLevelById(int playerObjId, int id);

	/**
	 * 获取指定图鉴条目的领奖状态。
	 * Returns the claim-reward status for the given bestiary entry.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @return 领奖状态值 / claim-reward value
	 */
	int getClaimRewardById(int playerObjId, int id);
}
