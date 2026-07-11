package com.aionemu.gameserver.model.items;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.bonuses.StatBonusType;

/**
 * Random 属性，用于物品相关逻辑。
 * Random Stats for items logic.
 *
 * @author Ranastic
 */

public class RandomStats {
	private final RandomBonusEffect rndBonusEffect;

	public RandomStats(int setId, int setNumber) {
		rndBonusEffect = new RandomBonusEffect(StatBonusType.INVENTORY, setId, setNumber);
	}

	/** 装备时 / on Equip. */
	public void onEquip(final Player player) {
		rndBonusEffect.applyEffect(player);
	}

	/** 卸下时 / on Un Equip. */
	public void onUnEquip(Player player) {
		rndBonusEffect.endEffect(player);
	}
}
