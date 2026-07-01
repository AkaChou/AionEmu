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
package com.aionemu.gameserver.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;

/**
 * @author xavier
 */
@Slf4j
public class FlyRingService {

	private static volatile ObjectProvider<FlyRingService> instanceProvider;

	private static class SingletonHolder {

		protected static final FlyRingService instance = new FlyRingService();
	}

	public static final FlyRingService getInstance() {
		ObjectProvider<FlyRingService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	public static void setInstanceProvider(ObjectProvider<FlyRingService> instanceProvider) {
		FlyRingService.instanceProvider = instanceProvider;
	}

	public FlyRingService() {
		for (FlyRingTemplate t : DataManager.FLY_RING_DATA.getFlyRingTemplates()) {
			FlyRing f = new FlyRing(t, 0);
			f.spawn();
			log.debug("Added " + f.getName() + " at m=" + f.getWorldId() + ",x=" + f.getX() + ",y=" + f.getY() + ",z="
					+ f.getZ());
		}
	}
}
