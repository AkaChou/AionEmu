package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.cp.PlayerCPList;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家创造点数（Creativity Points）数据访问对象。
 * Player creativity points data access object.
 */
public abstract class PlayerCreativityPointsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * DAO class name
	 */
	@Override
	public String getClassName() {
		return PlayerCreativityPointsDAO.class.getName();
	}

	/**
	 * 加载玩家创造点数列表。
	 * Loads the player's creativity-points list.
	 *
	 * 玩家 / player
	 * @return 创造点数列表 / creativity-points list
	 */
	public abstract PlayerCPList loadCP(Player paramPlayer);

	/**
	 * 存储一条创造点数记录。
	 * Stores a creativity-points entry.
	 *
	 * player object id
	 * @param paramInt2 槽位或条目 ID / slot or entry id
	 * points value
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean storeCP(int paramInt1, int paramInt2, int paramInt3);

	/**
	 * 删除指定槽位的创造点数记录。
	 * Deletes a creativity-points entry for the given slot.
	 *
	 * player object id
	 * slot
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean deleteCP(int playerObjId, int slot);

	/**
	 * 获取玩家已用创造点数槽位数。
	 * Returns the number of creativity-points slots used by the player.
	 *
	 * player id
	 * slot count
	 */
	public abstract int getSlotSize(int playerId);

	/**
	 * 按对象 ID 获取创造点数槽位对象 ID。
	 * Returns the creativity-points slot object id for the given object id.
	 *
	 * object id
	 * slot object id
	 */
	public abstract int getCPSlotObjId(int obj);
}
