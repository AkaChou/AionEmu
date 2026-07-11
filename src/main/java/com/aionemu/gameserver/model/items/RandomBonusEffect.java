package com.aionemu.gameserver.model.items;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.templates.item.bonuses.StatBonusType;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;

/**
 * Random 加成效果，用于物品相关逻辑。
 * Random Bonus Effect for items logic.
 *
 * @author Ranastic
 */
public class RandomBonusEffect implements StatOwner {

	private final ModifiersTemplate template;

	public RandomBonusEffect(StatBonusType type, int polishSetId, int polishNumber) {
		template = DataManager.ITEM_RANDOM_BONUSES.getTemplate(type, polishSetId, polishNumber);
	}

	/** 应用效果。 / Apply effect. */
	public void applyEffect(Player player) {
		player.getGameStats().addEffect(this, template.getModifiers());
	}

	/** 结束效果 / End Effect */
	public void endEffect(Player player) {
		player.getGameStats().endEffect(this);
	}
}
