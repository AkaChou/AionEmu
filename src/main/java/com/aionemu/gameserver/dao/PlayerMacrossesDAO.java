package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.MacroList;

/**
 * 玩家宏（Macrosses）数据访问对象。
 * Player macros (Macrosses) data access object.
 * <p/>
 * Created on: 13.07.2009 17:05:56
 *
 * @author Aquanox
 */
public abstract class PlayerMacrossesDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * DAO class name
	 */
	@Override
	public final String getClassName() {
		return PlayerMacrossesDAO.class.getName();
	}

	/**
	 * 加载玩家宏列表。
	 * Restores the list of macros for the player.
	 *
	 * player object id
	 * macro list
	 */
	public abstract MacroList restoreMacrosses(int playerId);

	/**
	 * 向数据库添加一条宏。
	 * Adds macro information into the database.
	 *
	 * player object id
	 * @param macroPosition 宏槽位序号 / macro order number
	 * macro contents
	 */
	public abstract void addMacro(int playerId, int macroPosition, String macro);

	/**
	 * 更新数据库中的宏。
	 * Updates macro information in the database.
	 *
	 * player object id
	 * @param macroPosition 宏槽位序号 / macro order number
	 * macro contents
	 */
	public abstract void updateMacro(int playerId, int macroPosition, String macro);

	/**
	 * 删除数据库中的宏。
	 * Removes a macro from the database.
	 *
	 * player object id
	 * @param macroPosition 宏槽位序号 / order of macro in macro list
	 */
	public abstract void deleteMacro(int playerId, int macroPosition);
}
