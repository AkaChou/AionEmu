package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 向玩家发放指定数量物品的操作。
 * Operation that gives a configured amount of an item to the player.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GiveItemOperation")
public class GiveItemOperation extends QuestOperation {

	/** 物品模板 ID / Item template id */
	@XmlAttribute(name = "item_id", required = true)
	protected int itemId;
	/** 发放数量 / Amount to give */
	@XmlAttribute(required = true)
	protected int count;

	/**
	 * 通过玩家控制器添加物品。
	 * Adds the items via the player controller.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	@Override
	public void doOperate(QuestEnv env) {
		Player player = env.getPlayer();
		player.getController().addItems(itemId, count);
	}
}
