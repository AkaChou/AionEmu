package com.aionemu.gameserver.dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.model.items.ManaStone;

/**
 * 物品镶嵌石列表数据访问对象。
 * Item stone list data access object.
 */
public abstract class ItemStoneListDAO implements DAO {
	/**
	 * 加载物品上的镶嵌石。
	 * Loads stones for the given items.
	 *
	 * @param items 物品集合 / item collection
	 */
	public abstract void load(Collection<Item> items);

	/**
	 * 存储魔力石。
	 * Stores mana stones.
	 *
	 * @param manaStones 魔力石集合 / mana stone set
	 */
	public abstract void storeManaStones(Set<ManaStone> manaStones);

	/**
	 * 存储融合石。
	 * Stores fusion stones.
	 *
	 * @param fusionStones 融合石集合 / fusion stone set
	 */
	public abstract void storeFusionStones(Set<ManaStone> fusionStones);

	/**
	 * 存储伊迪安石。
	 * Stores an Idian stone.
	 *
	 * @param idianStone Idian stone / Idian stone
	 */
	public abstract void storeIdianStones(IdianStone idianStone);

	/**
	 * 保存玩家全部物品上的镶嵌石。
	 * Saves stones for all items of a player.
	 *
	 * @param player 玩家 / player
	 */
	public void save(Player player) {
		save(player.getAllItems());
	}

	/**
	 * 保存物品列表上的镶嵌石。
	 * Saves stones for the given items.
	 *
	 * @param items 物品列表 / item list
	 */
	public abstract void save(List<Item> items);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public String getClassName() {
		return ItemStoneListDAO.class.getName();
	}
}
