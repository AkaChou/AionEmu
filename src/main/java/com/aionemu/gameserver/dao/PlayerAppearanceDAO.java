package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;

/**
 * 玩家外观数据访问对象，负责加载/存储玩家外观。
 * Player appearance data access object responsible for loading/storing player appearance.
 *
 * @author SoulKeeper
 */
public abstract class PlayerAppearanceDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public final String getClassName() {
		return PlayerAppearanceDAO.class.getName();
	}

	/**
	 * 按玩家 ID 加载外观；数据库中不存在时返回 null。
	 * Loads player appearance by player ID; returns null if not found in database.
	 *
	 * @param playerId 玩家 ID / player id
	 * @return 玩家外观，或 null / player appearance or null
	 */
	public abstract PlayerAppearance load(int playerId);

	/**
	 * 保存玩家外观到数据库；实际调用 {@link #store(int, PlayerAppearance)}。
	 * Saves player appearance to the database; actually calls {@link #store(int, PlayerAppearance)}.
	 *
	 * @param player 需要存储外观的玩家 / player whose appearance to store
	 * @return 若 the SQL query was successful 则为 true / true if the SQL query was successful
	 */
	public final boolean store(Player player) {
		return store(player.getObjectId(), player.getPlayerAppearance());
	}

	/**
	 * 将外观写入数据库。
	 * Stores appearance in the database.
	 *
	 * @param id 玩家 ID / player id
	 * @param playerAppearance 玩家外观 / player appearance
	 * @return 若 SQL 查询成功则为 true / true if the SQL query was successful
	 */
	public abstract boolean store(int id, PlayerAppearance playerAppearance);
}
