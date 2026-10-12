package com.aionemu.gameserver.persistence.dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.persistence.entity.ItemStonePo;
import com.mybatisflex.core.service.IService;

/**
 * 物品镶嵌石列表数据访问对象。
 * Item stone list data access object.
 */
public interface ItemStoneDao extends IService<ItemStonePo> {

	/**
	 * 加载物品上的镶嵌石。
	 * Loads stones for the given items.
	 * @param items 物品集合 / item collection
	 */
	void load(Collection<Item> items);

	/**
	 * 存储魔力石。
	 * Stores mana stones.
	 * @param manaStones 魔力石集合 / mana stone set
	 */
	void storeManaStones(Set<ManaStone> manaStones);

	/**
	 * 存储融合石。
	 * Stores fusion stones.
	 * @param fusionStones 融合石集合 / fusion stone set
	 */
	void storeFusionStones(Set<ManaStone> fusionStones);

	/**
	 * 存储伊迪安石。
	 * Stores an Idian stone.
	 * @param idianStone Idian stone / Idian stone
	 */
	void storeIdianStones(IdianStone idianStone);

	/**
	 * 保存玩家全部物品上的镶嵌石。
	 * Saves stones for all items of a player.
	 * @param player 玩家 / player
	 */
	default void save(Player player) {
		save(player.getAllItems());
	}

	/**
	 * 保存物品列表上的镶嵌石。
	 * Saves stones for the given items.
	 * @param items 物品列表 / item list
	 */
	void save(List<Item> items);
}
