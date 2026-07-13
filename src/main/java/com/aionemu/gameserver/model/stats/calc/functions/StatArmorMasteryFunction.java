package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.templates.item.ArmorType;
import com.aionemu.gameserver.utils.stats.CalculationType;

/**
 * 属性 ArmorMastery 函数模型。
 * Stat Armor Mastery Function model.
 *
 * @author ATracer (based on Mr.Poke ArmorMasteryModifier)
 */
public class StatArmorMasteryFunction extends StatFunctionProxy {

	private final ArmorType armorType;

	public StatArmorMasteryFunction(StatOwner owner, ArmorType armorType, IStatFunction function) {
		super(owner, function);
		this.armorType = armorType;
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		apply(stat, new CalculationType[0]);
	}

	@Override
	public void apply(Stat2 stat, CalculationType... calculationTypes) {
		Player player = (Player) stat.getOwner();
		if (player.getEquipment().isArmorEquipped(armorType)) {
			super.apply(stat, calculationTypes);
		}
	}
}
