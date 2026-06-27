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

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;

import static com.aionemu.gameserver.taskmanager.parallel.ForEach.forEach;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.ForkJoinTask;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.taskmanager.AbstractPeriodicTaskManager;
import com.google.common.base.Predicate;

public class MoveTaskManager extends AbstractPeriodicTaskManager {
	private static volatile ObjectProvider<MoveTaskManager> instanceProvider;
	private final Map<Integer, Creature> movingCreatures = new ConcurrentHashMap<Integer, Creature>();

	public static final int UPDATE_PERIOD = 100;

	private final Predicate<Creature> CREATURE_MOVE_PREDICATE = new Predicate<Creature>() {
		@Override
		public boolean apply(Creature creature) {
			creature.getMoveController().moveToDestination();
			if (creature.getAi2().poll(AIQuestion.DESTINATION_REACHED)) {
				movingCreatures.remove(creature.getObjectId());
				creature.getAi2().onGeneralEvent(AIEventType.MOVE_ARRIVED);
				GameMovementLoopServices.zoneUpdateService().add(creature);
			} else {
				creature.getAi2().onGeneralEvent(AIEventType.MOVE_VALIDATE);
			}
			return true;
		}
	};

	public MoveTaskManager() {
		super(UPDATE_PERIOD);
	}

	public void addCreature(Creature creature) {
		movingCreatures.put(creature.getObjectId(), creature);
	}

	public void removeCreature(Creature creature) {
		movingCreatures.remove(creature.getObjectId());
	}

	@Override
	public void run() {
		final ArrayList<Creature> copy = new ArrayList<Creature>(movingCreatures.values());
		ForkJoinTask<Creature> task = forEach(copy, CREATURE_MOVE_PREDICATE);
		if (task != null) {
			GameThreadPoolServices.threadPoolManager().getForkingPool().invoke(task);
		}
	}

	public static MoveTaskManager getInstance() {
		ObjectProvider<MoveTaskManager> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
		}
		return SingletonHolder.INSTANCE;
	}

	public static void setInstanceProvider(ObjectProvider<MoveTaskManager> provider) {
		instanceProvider = provider;
	}

	private static final class SingletonHolder {
		private static final MoveTaskManager INSTANCE = new MoveTaskManager();
	}
}
