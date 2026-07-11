package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.stats.CalculationType;

/**
 * 属性 DualWeaponMastery 函数模型。
 * Stat Dual Weapon Mastery Function model.
 *
 * @author ATracer
 */
public class StatDualWeaponMasteryFunction extends StatFunctionProxy {

	public StatDualWeaponMasteryFunction(Effect effect, IStatFunction statFunction) {
		super(effect, statFunction);
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat, CalculationType... calculationTypes) {
		Player player = (Player) stat.getOwner();
		if (player.getEquipment().hasDualWeaponEquipped(ItemSlot.SUB_HAND)) {
			super.apply(stat, calculationTypes);
		}
	}
}
