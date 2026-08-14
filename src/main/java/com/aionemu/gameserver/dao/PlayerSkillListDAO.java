package com.aionemu.gameserver.dao;

import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.SQLException;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillList;

/**
 * 玩家技能列表数据访问抽象层。
 * DAO for player skill list persistence.
 *
 * Created on: 15.07.2009 19:33:07 Edited On: 13.09.2009 19:48:00
 *
 * @author IceReaper, orfeo087, Avol, AEJTester
 */
public abstract class PlayerSkillListDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * @return 完整类名 / fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return PlayerSkillListDAO.class.getName();
	}

	/**
	 * 加载玩家技能列表。
	 * Loads the skill list for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @return 玩家技能列表 / player skill list
	 */
	public abstract PlayerSkillList loadSkillList(int playerId);

	/**
	 * 保存玩家技能信息。
	 * Stores skill information for the player.
	 *
	 * @param player 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean storeSkills(Player player);

	/** Converges one skill to at least the target level on the caller-owned transaction. */
	public abstract void storeSkillInTransaction(Connection connection, int playerId, int skillId, int targetLevel)
		throws SQLException;

	/**
	 * 查询技能皮肤激活时间。
	 * Returns the active date of a skill skin by id.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param skillId 技能 ID / skill id
	 * @return 激活时间戳 / activation timestamp
	 */
	public abstract Timestamp getSkinSkillActiveDateById(final int playerObjId, final int skillId);

	/**
	 * 查询技能皮肤过期时间（秒）。
	 * Returns the expire time (seconds) of a skill skin.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param skillId 技能 ID / skill id
	 * @return 过期时间（秒） / expire time in seconds
	 */
	public abstract int getSkinExpireTime(final int playerObjId, final int skillId);
}
