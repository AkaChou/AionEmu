package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;

class InstanceDeadlineSchedulerTest {

	@Test
	void executesExpiredDeadlineOnceAndPersistsCompletion() throws InterruptedException {
		ThreadPoolManager manager = new ThreadPoolManager();
		DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
		beans.registerSingleton("threadPoolManager", manager);
		GameThreadPoolServices services = new GameThreadPoolServices(beans.getBeanProvider(ThreadPoolManager.class));
		WorldMapInstance instance = new ObjenesisStd().newInstance(TestInstance.class);
		AtomicInteger executions = new AtomicInteger();
		CountDownLatch completed = new CountDownLatch(1);
		try {
			long deadline = System.currentTimeMillis() - 1;
			InstanceDeadlineScheduler.schedule(instance, "test", deadline, () -> {
				executions.incrementAndGet();
				completed.countDown();
			});
			assertTrue(completed.await(2, TimeUnit.SECONDS));
			InstanceDeadlineScheduler.schedule(instance, "test", deadline, executions::incrementAndGet);

			assertEquals(1, executions.get());
			assertTrue(InstanceDeadlineScheduler.isCompleted(instance, "test"));
			assertEquals(deadline, InstanceDeadlineScheduler.deadline(instance, "test"));
			InstanceDeadlineScheduler.cancel(instance, "test");
			assertFalse(InstanceDeadlineScheduler.isCompleted(instance, "test"));
		} finally {
			InstanceDeadlineScheduler.clearTransient(instance);
			services.destroy();
			manager.shutdown();
		}
	}

	private static final class TestInstance extends WorldMapInstance {
		private TestInstance() { super(null, 0); }
		@Override public MapRegion getRegion(float x, float y, float z) { return null; }
		@Override protected MapRegion createMapRegion(int regionId) { return null; }
		@Override protected void initMapRegions() { }
		@Override public boolean isPersonal() { return false; }
		@Override public int getOwnerId() { return 0; }
	}
}
