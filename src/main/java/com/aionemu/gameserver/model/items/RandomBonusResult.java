package com.aionemu.gameserver.model.items;

import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;

/**
 * Random 加成结果，用于物品相关逻辑。
 * Random Bonus Result for items logic.
 *
 * @author Rolandas
 */
public record RandomBonusResult(ModifiersTemplate template, int templateNumber) {

}
