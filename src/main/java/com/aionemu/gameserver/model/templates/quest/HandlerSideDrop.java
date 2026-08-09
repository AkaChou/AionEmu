package com.aionemu.gameserver.model.templates.quest;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

/**
 * 处理器 Side 掉落模板（静态数据/XML）。
 * XML template.
 */

public class HandlerSideDrop extends QuestDrop {
	private int neededAmount;

	public HandlerSideDrop(int questId, int npcId, int itemId, int amount, int chance) {
		this.questId = questId;
		this.npcId = npcId;
		this.itemId = itemId;
		this.chance = chance;

		GameEngineServices.questEngine().questCatalog().findMetadata(questId).stream()
			.flatMap(metadata -> metadata.drops().stream())
			.filter(drop -> drop.npcId() == npcId && drop.itemId() == itemId)
			.findFirst()
			.ifPresent(drop -> this.dropEachMember = switch (drop.scope()) {
				case GROUP -> 1;
				case ALLIANCE -> 2;
				case NONE -> 0;
			});
		this.neededAmount = amount;
	}

	public HandlerSideDrop(int questId, int npcId, int itemId, int amount, int chance, int step) {
		this(questId, npcId, itemId, amount, chance);
		this.collecting_step = step;
	}

	/** 返回 needed amount / Returns the needed amount */
	public int getNeededAmount() {
		return neededAmount;
	}
}
