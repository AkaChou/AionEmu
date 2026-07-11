package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeList;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家衣柜（Wardrobe）数据访问抽象层。
 * DAO for player wardrobe (appearance skin) persistence.
 */
public abstract class PlayerWardrobeDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public String getClassName() {
		return PlayerWardrobeDAO.class.getName();
	}

	/**
	 * 加载玩家衣柜列表。
	 * Loads the wardrobe list for the player.
	 *
	 * 玩家 / player
	 * wardrobe list
	 */
	public abstract PlayerWardrobeList load(Player paramPlayer);

	/**
	 * 保存衣柜物品记录。
	 * Stores a wardrobe item record.
	 *
	 * player object id
	 * item id
	 * slot
	 * @param reskin 重塑皮肤次数 / reskin count
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean store(int paramInt1, int paramInt2, int paramInt3, int reskin);

	/**
	 * 删除衣柜物品。
	 * Deletes a wardrobe item.
	 *
	 * player object id
	 * @param paramInt2 物品/槽位标识 / item or slot id
	 * @return 是否删除成功 / true if deleted
	 */
	public abstract boolean delete(int paramInt, int paramInt2);

	/**
	 * 查询玩家衣柜物品数量。
	 * Returns wardrobe item count for the player.
	 *
	 * player object id
	 * item count
	 */
	public abstract int getItemSize(int playerId);

	/**
	 * 按槽位查询衣柜物品 ID。
	 * Returns wardrobe item id by slot.
	 *
	 * player object id
	 * slot
	 * item id
	 */
	public abstract int getWardrobeItemBySlot(int playerObjId, int slot);

	/**
	 * 按槽位查询重塑次数。
	 * Returns reskin count by slot.
	 *
	 * player object id
	 * slot
	 * reskin count
	 */
	public abstract int getReskinCountBySlot(int playerObjId, int slot);

	/**
	 * 按槽位设置重塑次数。
	 * Sets reskin count by slot.
	 *
	 * player object id
	 * slot
	 * reskin count
	 * @return 是否更新成功 / true if updated
	 */
	public abstract boolean setReskinCountBySlot(int playerObjId, int slot, int reskin_count);
}
