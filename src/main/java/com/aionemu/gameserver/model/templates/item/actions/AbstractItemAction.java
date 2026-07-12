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
	 * 检查是否物品可 used。 / Check if an item can be used
	 *
	 * @param player
	 * @param parentItem
	 * @param targetItem
	 * @return
	 */
	public abstract boolean canAct(Player player, Item parentItem, Item targetItem);

	/**
	 * @param player
	 * @param parentItem
	 * @param targetItem
	 */
	public abstract void act(Player player, Item parentItem, Item targetItem);

}
