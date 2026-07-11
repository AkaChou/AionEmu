package com.aionemu.gameserver.services.player;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import com.aionemu.gameserver.configs.main.EventsConfig;
import org.junit.jupiter.api.Test;

class PlayerEventServiceTest {

	@Test
	void disabledSubEventsDoNotCreateScheduledTasks() throws Exception {
		boolean oldEventEnabled = EventsConfig.EVENT_ENABLED;
		boolean oldAwakeEnabled = EventsConfig.ENABLE_AWAKE_EVENT;
		boolean oldVipEnabled = EventsConfig.ENABLE_VIP_TICKETS;
		try {
			EventsConfig.EVENT_ENABLED = true;
			EventsConfig.ENABLE_AWAKE_EVENT = false;
			EventsConfig.ENABLE_VIP_TICKETS = false;

			PlayerEventService service = new PlayerEventService();

			assertNull(task(service, "awakeTask"));
			assertNull(task(service, "vipTask"));
		} finally {
			EventsConfig.EVENT_ENABLED = oldEventEnabled;
			EventsConfig.ENABLE_AWAKE_EVENT = oldAwakeEnabled;
			EventsConfig.ENABLE_VIP_TICKETS = oldVipEnabled;
		}
	}

	private static Object task(PlayerEventService service, String name) throws ReflectiveOperationException {
		Field field = PlayerEventService.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(service);
	}
}
