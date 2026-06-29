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
package com.aionemu.gameserver.world;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.AionObject;

/**
 * @author xavier
 */
@Slf4j
public abstract class Executor<T extends AionObject> {


	public abstract boolean run(T object);

	private final void runImpl(Collection<T> objects) {
		try {
			for (T o : objects) {
				if (o != null) {
					if (!Executor.this.run(o)) {
						break;
					}
				}
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}

	public final void execute(final Collection<T> objects, boolean now) {
		if (now) {
			runImpl(objects);
		} else {
			GameThreadPoolServices.threadPoolManager().execute(new Runnable() {

				@Override
				public void run() {
					runImpl(objects);
				}
			});
		}
	}

	public final void execute(final Collection<T> objects) {
		execute(objects, false);
	}
}