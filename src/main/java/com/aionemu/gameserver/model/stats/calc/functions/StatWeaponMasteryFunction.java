/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.model.stats.calc.functions;

import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.WeaponType;
import com.aionemu.gameserver.utils.stats.CalculationType;

/**
 * @author ATracer (based on Mr.Poke WeaponMasteryModifier)
 */
public class StatWeaponMasteryFunction extends StatRateFunction {

	private final WeaponType weaponType;

	public StatWeaponMasteryFunction(WeaponType weaponType, StatEnum name, int value, boolean bonus) {
		super(name, value, bonus);
		this.weaponType = weaponType;
	}

	@Override
	public void apply(Stat2 stat, CalculationType... calculationTypes) {
		Player player = (Player) stat.getOwner();
		switch (this.stat) {
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
		if (isBonus()) {
			int bonusRate = getValue();
			if (ArrayUtils.contains(calculationTypes, CalculationType.SKILL)
					&& ArrayUtils.contains(calculationTypes, CalculationType.DUAL_WIELD)) {
				bonusRate = Rnd.get(0, getValue());
			}
			stat.setFixedBonusRate(bonusRate / 100f);
		} else {
			stat.setBase(stat.getExactBaseWithoutBaseRate() * stat.calculatePercent(getValue()));
		}
	}
}
