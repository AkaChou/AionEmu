package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 抽象物品动作模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbstractItemAction")
public abstract class AbstractItemAction {

	/**
	 * 检查物品是否可以使用。
	 * Check whether the item can be used.
	 *
	 * @param player 使用物品的玩家 / the player using the item
	 * @param parentItem 父物品 / the parent item
	 * @param targetItem 目标物品 / the target item
	 * @return 允许使用则为 true / true if the item can be used
	 */
	public abstract boolean canAct(Player player, Item parentItem, Item targetItem);

	/**
	 * 执行物品动作。
	 * Perform the item action.
	 *
	 * @param player 使用物品的玩家 / the player using the item
	 * @param parentItem 父物品 / the parent item
	 * @param targetItem 目标物品 / the target item
	 */
	public abstract void act(Player player, Item parentItem, Item targetItem);

}
