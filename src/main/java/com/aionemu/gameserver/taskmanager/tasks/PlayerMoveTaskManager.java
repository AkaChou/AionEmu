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
package com.aionemu.gameserver.taskmanager.tasks;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.taskmanager.AbstractPeriodicTaskManager;

import javolution.util.FastMap;

/**
 * @author ATracer
 */
public class PlayerMoveTaskManager extends AbstractPeriodicTaskManager {
	private static volatile ObjectProvider<PlayerMoveTaskManager> instanceProvider;

	private final FastMap<Integer, Creature> movingPlayers = new FastMap<Integer, Creature>().shared();

	public PlayerMoveTaskManager() {
		super(200);
	}

	public void addPlayer(Creature player) {
		movingPlayers.put(player.getObjectId(), player);
	}

	public void removePlayer(Creature player) {
		movingPlayers.remove(player.getObjectId());
	}

	@Override
	public void run() {
		for (FastMap.Entry<Integer, Creature> e = movingPlayers.head(),
				mapEnd = movingPlayers.tail(); (e = e.getNext()) != mapEnd;) {
			Creature player = e.getValue();
			player.getMoveController().moveToDestination();
		}
	}

	public static final PlayerMoveTaskManager getInstance() {
		ObjectProvider<PlayerMoveTaskManager> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
		}
		return SingletonHolder.INSTANCE;
	}

	public static void setInstanceProvider(ObjectProvider<PlayerMoveTaskManager> provider) {
		instanceProvider = provider;
	}

	private static final class SingletonHolder {

		private static final PlayerMoveTaskManager INSTANCE = new PlayerMoveTaskManager();
	}
}
