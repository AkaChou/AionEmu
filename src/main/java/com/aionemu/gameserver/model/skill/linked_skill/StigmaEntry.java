package com.aionemu.gameserver.model.skill.linked_skill;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * Stigma 条目，用于技能相关逻辑。
 * Stigma Entry for skill logic.
 *
 * @author DrNism
 */
public abstract class StigmaEntry {

	protected final int itemId;
	protected final String itemName;

	StigmaEntry(int itemId, String itemName) {
		this.itemId = itemId;
		this.itemName = itemName;
	}

	/** 返回物品 ID / Returns the item id */
	public final int getItemId() {
		return itemId;
	}

	/** 获取物品名称。 / Returns the item name. */
	public final String getItemName() {
		return DataManager.ITEM_DATA.getItemTemplate(itemId).getName();
	}

	/** 获取技能模板。 / Returns the skill template. */
	public final ItemTemplate getSkillTemplate() {
		return DataManager.ITEM_DATA.getItemTemplate(getItemId());
	}
}
