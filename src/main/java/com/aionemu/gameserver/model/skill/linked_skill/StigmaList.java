package com.aionemu.gameserver.model.skill.linked_skill;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * Stigma 列表。
 * Stigma List interface.
 *
 * @author DrNism
 */
public interface StigmaList<T extends Creature> {

	/**
	 * 添加一个烙印之石条目。
	 * Adds a stigma entry.
	 *
	 * @param creature 所属生物 / Owning creature
	 * @param itemId 物品 ID / Item id
	 * @param itemName 物品名称 / Item name
	 * @return 添加成功为 {@code true} / {@code true} if added
	 */
	boolean addItem(T creature, int itemId, String itemName);

	/**
	 * 移除一个烙印之石条目并持久化。
	 * Removes a stigma entry and persists.
	 *
	 * @param player 所属玩家 / Owning player
	 * @param itemId 物品 ID / Item id
	 * @return 移除成功为 {@code true} / {@code true} if removed
	 */
	boolean remove(Player player, int itemId);

	/**
	 * 是否已装备指定条目。
	 * Whether the given entry is present.
	 *
	 * @param itemId 物品 ID / Item id
	 * @return 存在为 {@code true} / {@code true} if present
	 */
	boolean isItemPresent(int itemId);

	/**
	 * 当前条目数量。
	 * Number of current entries.
	 *
	 * @return 条目数量 / Entry count
	 */
	int size();

}
