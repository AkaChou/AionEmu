package com.aionemu.gameserver.model.items;

import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import lombok.Getter;

/**
 * ManaStone，用于物品相关逻辑。
 * Mana Stone for items logic.
 */

public class ManaStone extends ItemStone {
	@Getter
	private List<StatFunction> modifiers;

	public ManaStone(int itemObjId, int itemId, int slot, PersistentState persistentState) {
		super(itemObjId, itemId, slot, persistentState);
		ItemTemplate stoneTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (stoneTemplate != null && stoneTemplate.getModifiers() != null) {
			this.modifiers = stoneTemplate.getModifiers();
		}
	}

	/** 返回 first modifier / Returns the first modifier */
	public StatFunction getFirstModifier() {
		return (modifiers != null && modifiers.size() > 0) ? modifiers.get(0) : null;
	}

	/** 是否基础 / Whether basic */
	public boolean isBasic() {
		return !isAncient();
	}

	/**
	 * @return Whether ancient
	 */
	public boolean isAncient() {
		return getItemId() >= 167020006 && // Ancient Manastone: HP +105
				getItemId() <= 167020112; // [Event] Ancient Manastone: Healing Boost +5
	}
}
