package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.PlayerScripts;

/**
 * 房屋脚本数据访问对象。
 * House scripts data access object.
 */
public abstract class HouseScriptsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	@Override
	public final String getClassName() {
		return HouseScriptsDAO.class.getName();
	}

	/**
	 * 获取玩家房屋脚本。
	 * Gets player house scripts.
	 *
	 * @param paramInt 房屋/玩家相关 ID / house or player related ID
	 * player scripts
	 */
	public abstract PlayerScripts getPlayerScripts(int paramInt);

	/**
	 * 添加房屋脚本。
	 * Adds a house script.
	 *
	 * @param paramInt1 房屋/玩家相关 ID / house or player related ID
	 * script index
	 * script content
	 */
	public abstract void addScript(int paramInt1, int paramInt2, String paramString);

	/**
	 * 更新房屋脚本。
	 * Updates a house script.
	 *
	 * @param paramInt1 房屋/玩家相关 ID / house or player related ID
	 * script index
	 * script content
	 */
	public abstract void updateScript(int paramInt1, int paramInt2, String paramString);

	/**
	 * 删除房屋脚本。
	 * Deletes a house script.
	 *
	 * @param paramInt1 房屋/玩家相关 ID / house or player related ID
	 * script index
	 */
	public abstract void deleteScript(int paramInt1, int paramInt2);
}
