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
package com.aionemu.gameserver.utils;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.utils.concurrent.PriorityThreadFactory;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;

public class WorkStealThreadFactory extends PriorityThreadFactory implements ForkJoinWorkerThreadFactory {
	public WorkStealThreadFactory(String namePrefix) {
		super(namePrefix, Thread.NORM_PRIORITY);
	}

	public void setDefaultPool(ForkJoinPool pool) {
		if (pool == null) {
			pool = ForkJoinPool.commonPool();
		}
		super.setDefaultPool(pool);
	}

	@Override
	public ForkJoinPool getDefaultPool() {
		return (ForkJoinPool) super.getDefaultPool();
	}

	@Override
	public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
		return new WorkStealThread(pool);
	}

	@Slf4j
	private static class WorkStealThread extends ForkJoinWorkerThread {

		public WorkStealThread(ForkJoinPool pool) {
			super(pool);
		}

		@Override
		protected void onStart() {
			super.onStart();
		}

		@Override
		protected void onTermination(Throwable exception) {
			if (exception != null) {
				log.error("Error - Thread: " + this.getName() + " terminated abnormaly: " + exception);
			}
			super.onTermination(exception);
		}
	}
}
