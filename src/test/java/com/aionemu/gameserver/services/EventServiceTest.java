package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.model.templates.event.EventTemplate;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class EventServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void getActiveEventsReturnsSnapshot() throws ReflectiveOperationException {
		EventService service = objenesis.newInstance(EventService.class);
		EventTemplate event = new EventTemplate();
		List<EventTemplate> activeEvents = Collections.synchronizedList(new ArrayList<EventTemplate>());
		activeEvents.add(event);
		setField(service, "activeEvents", activeEvents);

		List<EventTemplate> snapshot = service.getActiveEvents();
		activeEvents.clear();

		assertEquals(1, snapshot.size());
		assertSame(event, snapshot.get(0));
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = EventService.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
