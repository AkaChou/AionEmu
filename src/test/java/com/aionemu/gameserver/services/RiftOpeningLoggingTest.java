package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.rift.RiftLocation;
import com.aionemu.gameserver.services.rift.RiftManager;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.testutil.ConfigSnapshot;
import com.aionemu.testutil.LogCapture;
import com.aionemu.testutil.LocalizedMessages;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;

/**
 * 验证地图摘要只计实际新增入口和 NPC，单入口操作不重复汇总。
 * Verifies map summaries count only actual additions and single-entrance operations do not duplicate summaries.
 */
class RiftOpeningLoggingTest {

	@Test
	void mapSummaryCountsNewEntrancesAndNpcsOnly() throws Exception {
		try (Fixture fixture = new Fixture(); LocalizedMessages messages = new LocalizedMessages()) {
			fixture.add(1, 10000, 2);
			fixture.add(2, 10000, 4);
			fixture.add(3, 10000, 0);
			RiftLocation alreadyOpened = fixture.add(4, 10000, 8);
			alreadyOpened.getSpawned().add(fixture.npc);
			fixture.add(5, 20000, 2);

			List<ILoggingEvent> first = fixture.openMapSummary(10000);
			assertEquals(1, first.size());
			assertEquals(I18n.get("rift.open.map_summary", 10000, 2, 6), first.getFirst().getFormattedMessage());
			assertEquals(Level.INFO, first.getFirst().getLevel());

			List<ILoggingEvent> second = fixture.openMapSummary(10000);
			assertEquals(1, second.size());
			assertEquals(I18n.get("rift.open.map_summary", 10000, 0, 0), second.getFirst().getFormattedMessage());

			List<ILoggingEvent> other = fixture.openMapSummary(20000);
			assertEquals(1, other.size());
			assertEquals(I18n.get("rift.open.map_summary", 20000, 1, 2), other.getFirst().getFormattedMessage());
		}
	}

	@Test
	void singleEntranceReportsZeroWhenNothingSpawnedOrAlreadyOpen() throws Exception {
		try (Fixture fixture = new Fixture(); LocalizedMessages messages = new LocalizedMessages();
			LogCapture logs = new LogCapture(RiftService.class)) {
			RiftLocation entrance = fixture.add(1, 10000, 2);
			fixture.service.openRifts(entrance);
			fixture.service.openRifts(entrance);
			fixture.service.openRifts(fixture.add(7, 10000, 0));
			assertFalse(fixture.service.openRifts(1));

			assertEquals(2, fixture.scheduler.oneShotTasks.size());
			assertEquals(List.of(
				I18n.get("rift.open.result", 1, 1, 2),
				I18n.get("rift.open.result", 1, 0, 0),
				I18n.get("rift.open.result", 7, 0, 0),
				I18n.get("rift.open.result", 1, 0, 0)),
				logs.events().stream().map(ILoggingEvent::getFormattedMessage).toList());
		}
	}

	@Test
	void staleAutoCloseDoesNotRemoveReopenedRift() throws Exception {
		try (Fixture fixture = new Fixture(); LocalizedMessages messages = new LocalizedMessages()) {
			RiftLocation entrance = fixture.add(1, 10000, 1);
			fixture.service.openRifts(entrance);
			Runnable staleClose = fixture.scheduler.oneShot();
			fixture.service.closeRift(entrance);
			fixture.service.openRifts(entrance);
			assertTrue(entrance.isOpened());
			Runnable currentClose = fixture.scheduler.oneShot();
			staleClose.run();

			assertTrue(entrance.isOpened());
			currentClose.run();
			assertFalse(fixture.service.getRiftLocations().get(1).isOpened());
			assertTrue(fixture.scheduler.oneShotTasks.isEmpty());
		}
	}

	/** 离线入口生成环境，不启动游戏世界。 / Offline spawn fixture without starting the game world. */
	@SuppressWarnings("unchecked")
	private static final class Fixture implements AutoCloseable {
		private final RiftService service = new RiftService();
		private final Map<Integer, RiftLocation> locations = new LinkedHashMap<>();
		private final RecordingScheduler scheduler = new RecordingScheduler();
		private final StaticApplicationContext applicationContext = new StaticApplicationContext();
		private final ConfigSnapshot config = ConfigSnapshot.of(CustomConfig.class, "RIFT_DURATION");
		private final Npc npc = new ObjenesisStd().newInstance(PlaceholderNpc.class);

		private Fixture() throws Exception {
			CustomConfig.RIFT_DURATION = 1;
			Field field = RiftService.class.getDeclaredField("locations");
			field.setAccessible(true);
			field.set(service, locations);
			ThreadPoolManager pools = new ObjenesisStd().newInstance(ThreadPoolManager.class);
			field = ThreadPoolManager.class.getDeclaredField("scheduledPool");
			field.setAccessible(true);
			field.set(pools, scheduler);
			DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
			beans.registerSingleton("pools", pools);
			ThreadPoolManager.setInstanceProvider(beans.getBeanProvider(ThreadPoolManager.class));
			applicationContext.getBeanFactory().registerSingleton("riftManager", new Spawner(npc));
			RiftManager.setInstanceProvider(applicationContext.getBeanProvider(RiftManager.class));
		}

		private RiftLocation add(int id, int world, int count) {
			RiftLocation location = new Entrance(id, world, count);
			location.getSpawned().clear();
			locations.put(id, location);
			return location;
		}

		private boolean openMap(int world) throws Exception {
			var method = RiftService.class.getDeclaredMethod("openRiftsOnMap", int.class);
			method.setAccessible(true);
			return (boolean) method.invoke(service, world);
		}

		private List<ILoggingEvent> openMapSummary(int world) throws Exception {
			LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
			Logger logger = context.getLogger(RiftService.class.getName());
			ListAppender<ILoggingEvent> appender = new ListAppender<>();
			appender.setContext(context);
			appender.start();
			Level previous = logger.getLevel();
			logger.addAppender(appender);
			logger.setLevel(Level.DEBUG);
			try {
				openMap(world);
				return List.copyOf(appender.list);
			} finally {
				logger.detachAppender(appender);
				appender.stop();
				logger.setLevel(previous);
			}
		}

		@Override
		public void close() {
			scheduler.shutdownNow();
			ThreadPoolManager.setInstanceProvider(null);
			RiftManager.setInstanceProvider(null);
			applicationContext.close();
			config.restore();
		}
	}

	/** 记录延迟任务而不启动工作线程。 / Records delayed tasks without starting workers. */
	private static final class RecordingScheduler extends ScheduledThreadPoolExecutor {
		private final List<Runnable> oneShotTasks = new ArrayList<>();

		private RecordingScheduler() {
			super(1);
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
			oneShotTasks.add(task);
			return null;
		}

		private Runnable oneShot() {
			return oneShotTasks.removeFirst();
		}
	}

	/** 可控的生成结果；每次生成后清除预置占位并记录实际新增数。 / Controlled spawn results that replace placeholders and count additions. */
	private static final class Spawner extends RiftManager {
		private final Npc npc;
		private int spawned;

		private Spawner(Npc npc) {
			this.npc = npc;
		}

		@Override
		public void spawnRift(RiftLocation location) {
			location.getSpawned().clear();
			int count = ((Entrance) location).count;
			for (int i = 0; i < count; i++) {
				location.getSpawned().add(npc);
			}
			spawned += count;
		}
	}

	/** 可清除占位裂隙的 NPC。 / NPC that can delete placeholder rifts. */
	private static final class PlaceholderNpc extends Npc {
		private PlaceholderNpc() {
			super(0, new NpcController(), null, null);
		}

		@Override
		public NpcController getController() {
			return new NpcController() {
				@Override
				public void onDelete() {
				}
			};
		}
	}

	/** 无需 XML 模板的入口。 / Entrance independent of XML templates. */
	private static final class Entrance extends RiftLocation {
		private final int id;
		private final int world;
		private final int count;

		private Entrance(int id, int world, int count) {
			this.id = id;
			this.world = world;
			this.count = count;
		}

		@Override
		public int getId() {
			return id;
		}

		@Override
		public int getWorldId() {
			return world;
		}
	}
}
