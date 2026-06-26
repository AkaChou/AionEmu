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
package com.aionemu.gameserver.services.player.CreativityPanel.stats;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;

public class Power implements StatOwner {

	private static volatile ObjectProvider<Power> instanceProvider;

	private List<IStatFunction> power = new ArrayList<IStatFunction>();

	public void onChange(Player player, int point) {
		if (point >= 1) {
			power.clear();
			player.getGameStats().endEffect(this);
			power.add(new StatAddFunction(StatEnum.HSTR, point, true));
			player.getGameStats().addEffect(this, power);
		} else if (point == 0) {
			power.clear();
			power.add(new StatAddFunction(StatEnum.HSTR, point, false));
			player.getGameStats().endEffect(this);
		}
	}

	public static Power getInstance() {
		ObjectProvider<Power> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	public static void setInstanceProvider(ObjectProvider<Power> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final Power INSTANCE = new Power();
	}
}
