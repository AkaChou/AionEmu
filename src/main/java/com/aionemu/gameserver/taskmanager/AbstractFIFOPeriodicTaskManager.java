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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.utils.concurrent.RunnableStatsManager;

/**
 * @author lord_rex and MrPoke based on l2j-free engines.
 */
@Slf4j
public abstract class AbstractFIFOPeriodicTaskManager<T> extends AbstractPeriodicTaskManager {

	private final Set<T> queue = new LinkedHashSet<T>();

	private final Set<T> activeTasks = new LinkedHashSet<T>();

	public AbstractFIFOPeriodicTaskManager(int period) {
		super(period);
	}

	public final void add(T t) {
		writeLock();
		try {
			queue.add(t);
		} finally {
			writeUnlock();
		}
	}

	@Override
	public final void run() {
		writeLock();
		try {
			activeTasks.addAll(queue);

			queue.clear();
		} finally {
			writeUnlock();
		}

		for (T task; (task = removeFirst(activeTasks)) != null;) {
			final long begin = System.nanoTime();

			try {
				callTask(task);
			} catch (RuntimeException e) {
				log.warn("Periodic task {} failed in {}", task, getClass().getSimpleName(), e);
			} finally {
				RunnableStatsManager.handleStats(task.getClass(), getCalledMethodName(), System.nanoTime() - begin);
			}
		}
	}

	protected abstract void callTask(T task);

	protected abstract String getCalledMethodName();

	private T removeFirst(Set<T> tasks) {
		Iterator<T> iterator = tasks.iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		T task = iterator.next();
		iterator.remove();
		return task;
	}
}
