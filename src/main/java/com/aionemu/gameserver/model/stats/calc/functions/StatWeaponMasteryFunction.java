package com.aionemu.gameserver.model.stats.calc.functions;

import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.WeaponType;
import com.aionemu.gameserver.utils.stats.CalculationType;

/**
 * 属性 WeaponMastery 函数模型。
 * Stat Weapon Mastery Function model.
 *
 * @author ATracer (based on Mr.Poke WeaponMasteryModifier)
 */
public class StatWeaponMasteryFunction extends StatFunctionProxy {

	private final WeaponType weaponType;

	public StatWeaponMasteryFunction(StatOwner owner, WeaponType weaponType, IStatFunction function, StatEnum stat) {
		super(owner, function, stat);
		this.weaponType = weaponType;
	}

	@Override
	public void apply(Stat2 stat) {
		apply(stat, new CalculationType[0]);
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat, CalculationType... calculationTypes) {
		Player player = (Player) stat.getOwner();
		switch (getName()) {
		case MAIN_HAND_POWER:
			if (player.getEquipment().getMainHandWeaponType() == weaponType) {
				applyTo(stat, calculationTypes);
			}
			break;
		case OFF_HAND_POWER:
			if (player.getEquipment().getOffHandWeaponType() == weaponType) {
				applyTo(stat, calculationTypes);
			}
			break;
		default:
			if (player.getEquipment().getMainHandWeaponType() == weaponType) {
				applyTo(stat, calculationTypes);
			}
		}
	}

	private void applyTo(Stat2 stat, CalculationType... calculationTypes) {
		if (getProxiedFunction() instanceof StatRateFunction && isBonus()) {
			int bonusRate = getValue();
			if (ArrayUtils.contains(calculationTypes, CalculationType.SKILL)
					&& ArrayUtils.contains(calculationTypes, CalculationType.DUAL_WIELD)) {
				bonusRate = Rnd.get(0, getValue());
			}
			stat.setFixedBonusRate(bonusRate / 100f);
		} else {
			super.apply(stat, calculationTypes);
		}
	}
}
