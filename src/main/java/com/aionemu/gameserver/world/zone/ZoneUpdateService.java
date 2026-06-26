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
package com.aionemu.gameserver.world.zone;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.taskmanager.AbstractFIFOPeriodicTaskManager;

/**
 * @author ATracer
 */
public class ZoneUpdateService extends AbstractFIFOPeriodicTaskManager<Creature> {
	private static volatile ObjectProvider<ZoneUpdateService> instanceProvider;

	public ZoneUpdateService() {
		super(500);
	}

	@Override
	protected void callTask(Creature creature) {
		creature.getController().refreshZoneImpl();
		if (creature instanceof Player) {
			ZoneLevelService.checkZoneLevels((Player) creature);
		}
	}

	@Override
	protected String getCalledMethodName() {
		return "ZoneUpdateService()";
	}

	public static ZoneUpdateService getInstance() {
		ObjectProvider<ZoneUpdateService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	public static void setInstanceProvider(ObjectProvider<ZoneUpdateService> provider) {
		instanceProvider = provider;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final ZoneUpdateService instance = new ZoneUpdateService();
	}
}
