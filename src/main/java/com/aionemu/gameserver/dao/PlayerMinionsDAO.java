package com.aionemu.gameserver.dao;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.minion.MinionDopingBag;

/**
 * 玩家迷你宠物（Minion）数据访问对象。
 * Player minion data access object.
 *
 * @author Falke_34
 */
public abstract class PlayerMinionsDAO implements IDFactoryAwareDAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public final String getClassName() {
		return PlayerMinionsDAO.class.getName();
	}

	/**
	 * 插入一只迷你宠物。
	 * Inserts a player minion.
	 *
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 */
	public abstract boolean insertPlayerMinion(MinionCommonData minionCommonData);

	/**
	 * 移除玩家的指定迷你宠物。
	 * Removes the given minion from the player.
	 *
	 * @param player 玩家 / player
	 * @param minionObjId 迷你宠物对象 ID / minion object id
	 */
	public abstract boolean removePlayerMinion(Player player, int minionObjId);

	/**
	 * 更新迷你宠物名称。
	 * Updates the minion name.
	 *
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 */
	public abstract boolean updateMinionName(MinionCommonData minionCommonData);

	/**
	 * 获取玩家全部迷你宠物。
	 * Returns all minions owned by the player.
	 *
	 * @param player 玩家 / player
	 * @return 迷你宠物列表 / list of minions
	 */
	public abstract List<MinionCommonData> getPlayerMinions(Player player);

	/**
	 * 更新迷你宠物成长点数。
	 * Updates the minion growth points.
	 *
	 * @param player 玩家 / player
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 */
	public abstract boolean updatePlayerMinionGrowthPoint(Player player, MinionCommonData minionCommonData);

	public abstract boolean updateGrowthAndRemoveMaterials(Player player, MinionCommonData minionCommonData,
			List<Integer> materialObjectIds);

	public abstract boolean replacePlayerMinions(MinionCommonData replacement, List<Integer> materialObjectIds);

	/**
	 * 进化迷你宠物。
	 * Evolves the minion.
	 *
	 * @param player 玩家 / player
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 */
	public abstract boolean evolutionMinion(Player player, MinionCommonData minionCommonData);

	/**
	 * 锁定/解锁迷你宠物。
	 * Locks or unlocks a minion.
	 *
	 * @param player 玩家 / player
	 * @param minionObjId 迷你宠物对象 ID / minion object id
	 * @param isLocked 锁定标记 / lock flag
	 */
	public abstract boolean lockMinions(Player player, int minionObjId, int isLocked);

	/**
	 * 保存迷你宠物增益包。
	 * Saves the minion doping bag.
	 *
	 * @param player 玩家 / player
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 * @param bag 补给包 / doping bag
	 */
	public abstract boolean saveDopingBag(Player player, MinionCommonData minionCommonData, MinionDopingBag bag);

	/**
	 * 保存迷你宠物生日。
	 * Saves the minion birthday.
	 *
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 */
	public abstract void saveBirthday(MinionCommonData minionCommonData);

}
