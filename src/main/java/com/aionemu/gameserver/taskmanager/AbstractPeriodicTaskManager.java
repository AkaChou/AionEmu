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
package com.aionemu.gameserver.taskmanager;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.taskmanager.AbstractLockManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.GameServer.StartupHook;

/**
 * @author lord_rex and MrPoke based on l2j-free engines. This can be used for
 *         periodic calls.
 */
@Slf4j(access = AccessLevel.PROTECTED)
public abstract class AbstractPeriodicTaskManager extends AbstractLockManager implements Runnable, StartupHook {

	private final int period;

	public AbstractPeriodicTaskManager(int period) {
		this.period = period;

		GameServer.addStartupHook(this);

		log.info("{} initialized", getClass().getSimpleName());
	}

	@Override
	public final void onStartup() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(this, 1000 + Rnd.get(period),
				Rnd.get(period - 5, period + 5));
	}

	@Override
	public abstract void run();
}
