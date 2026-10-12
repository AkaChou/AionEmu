package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.MacroList;
import com.aionemu.gameserver.persistence.entity.PlayerMacrossePo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家宏（Macrosses）数据访问层。
 * Data access layer for player macros (Macrosses).
 * @author Aquanox
 */
public interface PlayerMacrosseDao extends IService<PlayerMacrossePo> {

	/**
	 * 加载玩家宏列表。
	 * Restores the list of macros for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @return 宏列表 / macro list
	 */
	MacroList restoreMacrosses(int playerId);

	/**
	 * 向数据库添加一条宏。
	 * Adds macro information into the database.
	 * @param playerId 玩家对象 ID / player object id
	 * @param macroPosition 宏槽位序号 / macro order number
	 * @param macro 宏内容 / macro contents
	 */
	void addMacro(int playerId, int macroPosition, String macro);

	/**
	 * 更新数据库中的宏。
	 * Updates macro information in the database.
	 * @param playerId 玩家对象 ID / player object id
	 * @param macroPosition 宏槽位序号 / macro order number
	 * @param macro 宏内容 / macro contents
	 */
	void updateMacro(int playerId, int macroPosition, String macro);

	/**
	 * 删除数据库中的宏。
	 * Removes a macro from the database.
	 * @param playerId 玩家对象 ID / player object id
	 * @param macroPosition 宏槽位序号 / order of macro in macro list
	 */
	void deleteMacro(int playerId, int macroPosition);
}
