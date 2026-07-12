package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.services.ServiceContext;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import org.junit.jupiter.api.Test;

class ScheduleHotReloadTest {

	@Test
	void reloadReplacesRiftSchedulesAndSupportsDisableEnable() {
		String oldConfigDir = System.getProperty("aion.config.dir");
		boolean oldEnabled = CustomConfig.RIFT_ENABLED;
		try (ServiceContext.Scope ignored = ServiceContext.use("schedule-reload-test-" + System.nanoTime())) {
			System.setProperty("aion.config.dir", "aion/config");
			CustomConfig.RIFT_ENABLED = true;
			GameCronServices.initialize();
			RiftService service = new RiftService();

			service.reloadSchedule();
			int taskCount = GameCronServices.cronService().getRunnables().size();
			assertTrue(taskCount > 0);
			service.reloadSchedule();
			assertEquals(taskCount, GameCronServices.cronService().getRunnables().size());

			CustomConfig.RIFT_ENABLED = false;
			service.reloadSchedule();
			assertEquals(0, GameCronServices.cronService().getRunnables().size());

			CustomConfig.RIFT_ENABLED = true;
			service.reloadSchedule();
			assertEquals(taskCount, GameCronServices.cronService().getRunnables().size());
		} finally {
			GameCronServices.shutdownIfInitialized();
			CustomConfig.RIFT_ENABLED = oldEnabled;
			if (oldConfigDir == null) {
				System.clearProperty("aion.config.dir");
			} else {
				System.setProperty("aion.config.dir", oldConfigDir);
			}
		}
	}
}
