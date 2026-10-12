package com.aionemu.gameserver.persistence.dao;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.minion.MinionDopingBag;
import com.aionemu.gameserver.persistence.entity.PlayerMinionPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家迷你宠物（Minion）数据访问对象。
 * Player minion data access object.
 */
public interface PlayerMinionDao extends IService<PlayerMinionPo> {

	/**
	 * 插入一只迷你宠物。
	 * Inserts a player minion.
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 * @return 是否成功 / whether successful
	 */
	boolean insertPlayerMinion(MinionCommonData minionCommonData);

	/**
	 * 移除玩家的指定迷你宠物。
	 * Removes the given minion from the player.
	 * @param player 玩家 / player
	 * @param minionObjId 迷你宠物对象 ID / minion object id
	 * @return 是否成功 / whether successful
	 */
	boolean removePlayerMinion(Player player, int minionObjId);

	/**
	 * 更新迷你宠物名称。
	 * Updates the minion name.
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 * @return 是否成功 / whether successful
	 */
	boolean updateMinionName(MinionCommonData minionCommonData);

	/**
	 * 获取玩家全部迷你宠物。
	 * Returns all minions owned by the player.
	 * @param player 玩家 / player
	 * @return 迷你宠物列表 / list of minions
	 */
	List<MinionCommonData> getPlayerMinions(Player player);

	/**
	 * 更新迷你宠物成长点数。
	 * Updates the minion growth points.
	 * @param player 玩家 / player
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 * @param materialObjectIds 消耗材料的对象 ID 列表 / object ids of consumed materials
	 * @return 是否成功 / whether successful
	 */
	boolean updateGrowthAndRemoveMaterials(Player player, MinionCommonData minionCommonData,
			List<Integer> materialObjectIds);

	/**
	 * 以新随从替换旧随从，并移除材料。
	 * Replaces old minions with the replacement and removes the materials.
	 * @param replacement 替换用随从数据 / replacement minion data
	 * @param materialObjectIds 消耗材料的对象 ID 列表 / object ids of consumed materials
	 * @return 是否成功 / whether successful
	 */
	boolean replacePlayerMinions(MinionCommonData replacement, List<Integer> materialObjectIds);

	/**
	 * 进化迷你宠物。
	 * Evolves the minion.
	 * @param player 玩家 / player
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 * @return 是否成功 / whether successful
	 */
	boolean evolutionMinion(Player player, MinionCommonData minionCommonData);

	/**
	 * 锁定/解锁迷你宠物。
	 * Locks or unlocks a minion.
	 * @param player 玩家 / player
	 * @param minionObjId 迷你宠物对象 ID / minion object id
	 * @param isLocked 锁定标记 / lock flag
	 * @return 是否成功 / whether successful
	 */
	boolean lockMinions(Player player, int minionObjId, int isLocked);

	/**
	 * 保存迷你宠物增益包。
	 * Saves the minion doping bag.
	 * @param player 玩家 / player
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 * @param bag 补给包 / doping bag
	 * @return 是否成功 / whether successful
	 */
	boolean saveDopingBag(Player player, MinionCommonData minionCommonData, MinionDopingBag bag);

	/**
	 * 保存迷你宠物生日。
	 * Saves the minion birthday.
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 */
	void saveBirthday(MinionCommonData minionCommonData);

	/**
	 * 返回本 DAO 已使用的全部 ID。
	 * Returns an array of all IDs that are used by this DAO.
	 * @return 已使用 ID 数组 / array of used IDs
	 */
	int[] getUsedIDs();
}
