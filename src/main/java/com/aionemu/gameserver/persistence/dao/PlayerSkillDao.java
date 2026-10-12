package com.aionemu.gameserver.persistence.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.persistence.entity.PlayerSkillPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家技能列表数据访问层。
 * Data access layer for player skill list persistence.
 *
 * @author IceReaper, orfeo087, Avol, AEJTester
 */
public interface PlayerSkillDao extends IService<PlayerSkillPo> {

	/**
	 * 加载玩家技能列表。
	 * Loads the skill list for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @return 玩家技能列表 / player skill list
	 */
	PlayerSkillList loadSkillList(int playerId);

	/**
	 * 保存玩家技能信息。
	 * Stores skill information for the player.
	 * @param player 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	boolean storeSkills(Player player);

	/**
	 * 在调用方事务上将单个技能收敛到至少目标等级。
	 * Converges one skill to at least the target level on the caller-owned transaction.
	 * @param connection 调用方事务连接 / caller-owned transaction connection
	 * @param playerId 玩家对象 ID / player object id
	 * @param skillId 技能 ID / skill id
	 * @param targetLevel 目标等级 / target level
	 * @throws SQLException 写入失败时 / on write failure
	 */
	void storeSkillInTransaction(Connection connection, int playerId, int skillId, int targetLevel) throws SQLException;
}
