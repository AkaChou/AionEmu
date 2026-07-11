package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 从玩家背包扣除指定数量物品的操作。
 * Operation that removes a configured amount of an item from the player's inventory.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TakeItemOperation")
public class TakeItemOperation extends QuestOperation {

	/** 物品模板 ID / Item template id */
	@XmlAttribute(name = "item_id", required = true)
	protected int itemId;
	/** 扣除数量 / Amount to remove */
	@XmlAttribute(required = true)
	protected int count;

	/**
	 * 按物品 ID 减少背包数量。
	 * Decreases inventory count by item id.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	@Override
	public void doOperate(QuestEnv env) {
		env.getPlayer().getInventory().decreaseByItemId(itemId, count);
	}
}
