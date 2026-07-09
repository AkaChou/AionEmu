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
import com.aionemu.gameserver.utils.stats.CalculationType;

class PhysicalAttackFunction extends StatFunction {

	PhysicalAttackFunction() {
		stat = StatEnum.PHYSICAL_ATTACK;
	}

	@Override
	public void apply(Stat2 stat, CalculationType... calculationTypes) {
		float power = stat.getOwner().getGameStats().getPower().getCurrent();
		if (stat.getOwner() instanceof Player
				&& ArrayUtils.contains(calculationTypes, CalculationType.SKILL)
				&& ArrayUtils.contains(calculationTypes, CalculationType.DUAL_WIELD)) {
			power = power > 100 ? Rnd.get(100, (int) power) : Rnd.get((int) power, 100);
		}
		stat.setBaseRate(power / 100f);
	}

	@Override
	public int getPriority() {
		return 30;
	}
}
