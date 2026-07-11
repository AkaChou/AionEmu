package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.conditions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 按玩家背包中指定物品数量与配置值比较的条件。
 * Condition that compares the player's inventory count of an item against a configured value.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PcInventoryCondition")
public class PcInventoryCondition extends QuestCondition {

	/** 物品模板 ID / Item template id */
	@XmlAttribute(name = "item_id", required = true)
	protected int itemId;
	/** 比较用数量阈值 / Count threshold for comparison */
	@XmlAttribute(required = true)
	protected long count;

	/**
	 * 返回物品模板 ID。
	 * Returns the item template id.
	 *
	 * Item id
	 */
	public int getItemId() {
		return itemId;
	}

	/**
	 * 返回比较用数量阈值。
	 * Returns the count threshold used for comparison.
	 *
	 * Count
	 */
	public long getCount() {
		return count;
	}

	/**
	 * 比较玩家背包中该物品数量与配置阈值。
	 * Compares the player's item count with the configured threshold.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 比较是否成立 / Whether the comparison holds
	 */
	@Override
	public boolean doCheck(QuestEnv env) {
		Player player = env.getPlayer();
		long itemCount = player.getInventory().getItemCountByItemId(itemId);
		switch (getOp()) {
		case EQUAL:
			return itemCount == count;
		case GREATER:
			return itemCount > count;
		case GREATER_EQUAL:
			return itemCount >= count;
		case LESSER:
			return itemCount < count;
		case LESSER_EQUAL:
			return itemCount <= count;
		case NOT_EQUAL:
			return itemCount != count;
		default:
			return false;
		}
	}
}
