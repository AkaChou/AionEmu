package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.EventData;
import com.aionemu.gameserver.model.EventType;
import com.aionemu.gameserver.model.templates.event.EventTemplate;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class EventServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void getActiveEventsReturnsSnapshot() throws ReflectiveOperationException {
		EventService service = objenesis.newInstance(EventService.class);
		EventTemplate event = new EventTemplate();
		List<EventTemplate> activeEvents = Collections.synchronizedList(new ArrayList<>());
		activeEvents.add(event);
		setField(service, "activeEvents", activeEvents);

		List<EventTemplate> snapshot = service.getActiveEvents();
		activeEvents.clear();

		assertEquals(1, snapshot.size());
		assertSame(event, snapshot.getFirst());
	}

	@Test
	void eventTypeIncludesAionServerTestBasicThemes() {
		assertEquals(16, EventType.getEventType("test_basic_1").getId());
		assertEquals(32, EventType.getEventType("test_basic_2").getId());
		assertEquals(64, EventType.getEventType("test_basic_3").getId());
		assertEquals(128, EventType.getEventType("test_basic_4").getId());
	}

	@Test
	void checkEventsDoesNotStartDateActiveEventThatIsNotConfiguredActive() throws Exception {
		EventData previous = DataManager.EVENT_DATA;
		try {
			EventData data = (EventData) JAXBContext.newInstance(EventData.class).createUnmarshaller()
				.unmarshal(new StringReader("""
					<events_config>
						<active>Configured;Future</active>
						<events>
							<event name="Configured" start="2000-01-01T00:00:00+00:00" end="2999-12-31T00:00:00+00:00"/>
							<event name="DateOnly" start="2000-01-01T00:00:00+00:00" end="2999-12-31T00:00:00+00:00"/>
							<event name="Future" start="3000-01-01T00:00:00+00:00" end="3099-12-31T00:00:00+00:00"/>
						</events>
					</events_config>
					"""));
			DataManager.EVENT_DATA = data;

			assertTrue(data.isConfiguredActive("Future"));
			assertFalse(data.isConfiguredActive("DateOnly"));

			EventService service = objenesis.newInstance(EventService.class);
			setField(service, "activeEvents",
				Collections.synchronizedList(new ArrayList<>(data.getActiveEvents())));
			setField(service, "eventsForStartQuest", new HashMap<>());
			setField(service, "eventsForMaintainQuest", new HashMap<>());

			Method checkEvents = EventService.class.getDeclaredMethod("checkEvents");
			checkEvents.setAccessible(true);
			checkEvents.invoke(service);

			assertTrue(event(data, "Configured").isStarted());
			assertFalse(event(data, "DateOnly").isStarted());
			assertFalse(event(data, "Future").isStarted());
		} finally {
			DataManager.EVENT_DATA = previous;
		}
	}

	private static EventTemplate event(EventData data, String name) {
		return data.getAllEvents().stream()
			.filter(template -> name.equals(template.getName()))
			.findFirst()
			.orElseThrow();
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = EventService.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
