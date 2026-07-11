package com.aionemu.gameserver.model.items;

import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;
import lombok.Getter;

/**
 * Random 加成结果，用于物品相关逻辑。
 * Random Bonus Result for items logic.
 *
 * @author Rolandas
 */
@Getter
public class RandomBonusResult {

	private final ModifiersTemplate template;
	private final int templateNumber;

	public RandomBonusResult(ModifiersTemplate template, int number) {
		this.template = template;
		this.templateNumber = number;
	}

}
