package com.aionemu.gameserver.dao;

import java.util.List;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.minion.MinionDopingBag;

/**
 * 玩家迷你宠物（Minion）数据访问对象。
 * Player minion data access object.
 *
 * @author Falke_34
 */
public abstract class PlayerMinionsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * DAO class name
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
	public abstract void insertPlayerMinion(MinionCommonData minionCommonData);

	/**
	 * 移除玩家的指定迷你宠物。
	 * Removes the given minion from the player.
	 *
	 * 玩家 / player
	 * @param minionObjId 迷你宠物对象 ID / minion object id
	 */
	public abstract void removePlayerMinion(Player player, int minionObjId);

	/**
	 * 更新迷你宠物名称。
	 * Updates the minion name.
	 *
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 */
	public abstract void updateMinionName(MinionCommonData minionCommonData);

	/**
	 * 获取玩家全部迷你宠物。
	 * Returns all minions owned by the player.
	 *
	 * 玩家 / player
	 * @return 迷你宠物列表 / list of minions
	 */
	public abstract List<MinionCommonData> getPlayerMinions(Player player);

	/**
	 * 更新迷你宠物成长点数。
	 * Updates the minion growth points.
	 *
	 * 玩家 / player
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 */
	public abstract void updatePlayerMinionGrowthPoint(Player player, MinionCommonData minionCommonData);

	/**
	 * 判断玩家是否拥有指定迷你宠物。
	 * Checks whether the player owns the given minion.
	 *
	 * player id
	 * minion id
	 * 若 owned 则为 true / true if owned
	 */
	public abstract boolean PlayerMinions(int playerid, int miniona);

	/**
	 * 进化迷你宠物。
	 * Evolves the minion.
	 *
	 * 玩家 / player
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 */
	public abstract void evolutionMinion(Player player, MinionCommonData minionCommonData);

	/**
	 * 锁定/解锁迷你宠物。
	 * Locks or unlocks a minion.
	 *
	 * 玩家 / player
	 * @param minionObjId 迷你宠物对象 ID / minion object id
	 * lock flag
	 */
	public abstract void lockMinions(Player player, int minionObjId, int isLocked);

	/**
	 * 保存迷你宠物增益包。
	 * Saves the minion doping bag.
	 *
	 * 玩家 / player
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 * doping bag
	 */
	public abstract void saveDopingBag(Player player, MinionCommonData minionCommonData, MinionDopingBag bag);

	/**
	 * 保存迷你宠物生日。
	 * Saves the minion birthday.
	 *
	 * @param minionCommonData 迷你宠物公共数据 / minion common data
	 */
	public abstract void saveBirthday(MinionCommonData minionCommonData);

}
