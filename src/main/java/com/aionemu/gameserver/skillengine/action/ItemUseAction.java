package com.aionemu.gameserver.skillengine.action;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 物品消耗动作：施法时从玩家背包扣除指定物品。
 * Item cost action: removes items from the player inventory on cast.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemUseAction")
public class ItemUseAction extends Action {

	/**
	 * 消耗物品 ID。
	 * Item id to consume.
	 */
	@XmlAttribute(required = true)
	protected int itemid;

	/**
	 * 消耗数量。
	 * Quantity to consume.
	 */
	@XmlAttribute(required = true)
	protected int count;

	/**
	 * 从玩家背包扣除物品；非玩家施法者忽略。
	 * Decreases items from player inventory; ignored for non-player casters.
	 *
	 * @param skill 当前技能上下文 / current skill context
	 */
	@Override
	public void act(Skill skill) {
		if (skill.getEffector() instanceof Player) {
			Player player = (Player) skill.getEffector();
			Storage inventory = player.getInventory();

			if (!inventory.decreaseByItemId(itemid, count)) {
				return;
			}
		}
	}
}
