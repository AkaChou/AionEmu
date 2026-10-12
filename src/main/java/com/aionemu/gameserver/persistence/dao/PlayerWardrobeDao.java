package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerWardrobePo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家衣柜（Wardrobe）数据访问层。
 * Data access layer for player wardrobe (appearance skin) persistence.
 */
public interface PlayerWardrobeDao extends IService<PlayerWardrobePo> {

	/**
	 * 加载玩家衣柜列表。
	 * Loads the wardrobe list for the player.
	 * @param player 玩家 / player
	 * @return 衣柜列表 / wardrobe list
	 */
	PlayerWardrobeList load(Player player);

	/**
	 * 保存衣柜物品记录。
	 * Stores a wardrobe item record.
	 * @param objectId 玩家对象 ID / player object id
	 * @param itemId 物品 ID / item id
	 * @param slot 槽位 / slot
	 * @param reskin 重塑皮肤次数 / reskin count
	 * @return 是否保存成功 / true if stored
	 */
	boolean store(int objectId, int itemId, int slot, int reskin);

	/**
	 * 删除衣柜物品。
	 * Deletes a wardrobe item.
	 * @param objectId 玩家对象 ID / player object id
	 * @param itemId 物品 ID / item id
	 * @return 是否删除成功 / true if deleted
	 */
	boolean delete(int objectId, int itemId);

	/**
	 * 查询玩家衣柜物品数量。
	 * Returns wardrobe item count for the player.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 物品数量 / item count
	 */
	int getItemSize(int playerObjId);

	/**
	 * 按槽位查询衣柜物品 ID。
	 * Returns wardrobe item id by slot.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @return 物品 ID / item id
	 */
	int getWardrobeItemBySlot(int playerObjId, int slot);

	/**
	 * 按槽位查询重塑次数。
	 * Returns reskin count by slot.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @return 重塑次数 / reskin count
	 */
	int getReskinCountBySlot(int playerObjId, int slot);

	/**
	 * 按槽位设置重塑次数。
	 * Sets reskin count by slot.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @param reskinCount 重塑次数 / reskin count
	 */
	void setReskinCountBySlot(int playerObjId, int slot, int reskinCount);
}
