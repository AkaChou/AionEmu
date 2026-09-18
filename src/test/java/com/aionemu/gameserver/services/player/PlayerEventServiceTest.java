package com.aionemu.gameserver.services.player;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.testutil.ConfigSnapshot;
import org.junit.jupiter.api.Test;

class PlayerEventServiceTest {

	@Test
	void disabledSubEventsDoNotCreateScheduledTasks() throws Exception {
		ConfigSnapshot snapshot = ConfigSnapshot.of(EventsConfig.class,
			"EVENT_ENABLED", "ENABLE_AWAKE_EVENT", "ENABLE_VIP_TICKETS");
		try {
			EventsConfig.EVENT_ENABLED = true;
			EventsConfig.ENABLE_AWAKE_EVENT = false;
			EventsConfig.ENABLE_VIP_TICKETS = false;

			PlayerEventService service = new PlayerEventService();

			assertNull(task(service, "awakeTask"));
			assertNull(task(service, "vipTask"));
		} finally {
			snapshot.restore();
		}
	}

	private static Object task(PlayerEventService service, String name) throws ReflectiveOperationException {
		Field field = PlayerEventService.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(service);
	}
}
