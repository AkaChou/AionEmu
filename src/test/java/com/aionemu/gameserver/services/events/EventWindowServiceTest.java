package com.aionemu.gameserver.services.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.templates.event.EventsWindow;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.datatype.DatatypeFactory;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class EventWindowServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void getActiveEventsDoesNotReusePreviousPlayersResults() throws Exception {
		EventWindowService service = objenesis.newInstance(EventWindowService.class);
		Map<Integer, EventsWindow> allEvents = new LinkedHashMap<Integer, EventsWindow>();
		allEvents.put(1, eventWindow(1, 1, 10));
		allEvents.put(2, eventWindow(2, 50, 60));
		setField(service, "allEvents", allEvents);
		setField(service, "activeEvents", new ConcurrentHashMap<Integer, EventsWindow>());

		Map<Integer, EventsWindow> lowLevelEvents = service.getActiveEvents(playerWithLevel(5));
		Map<Integer, EventsWindow> highLevelEvents = service.getActiveEvents(playerWithLevel(55));

		assertEquals(1, lowLevelEvents.size());
		assertTrue(lowLevelEvents.containsKey(1));
		assertEquals(1, highLevelEvents.size());
		assertTrue(highLevelEvents.containsKey(2));
	}

	@Test
	void logoutElapsedCalculationDoesNotUseSharedTimingFields() {
		assertThrows(NoSuchFieldException.class, () -> EventWindowService.class.getDeclaredField("tStart"));
		assertThrows(NoSuchFieldException.class, () -> EventWindowService.class.getDeclaredField("tEnd"));
	}

	@Test
	void activeEventExclusionMapIsSafeForScheduledAndLoginThreads() throws Exception {
		assertTrue(ConcurrentMap.class.isAssignableFrom(EventWindowService.class.getDeclaredField("activeEvents").getType()));
	}

	private Player playerWithLevel(int level) throws ReflectiveOperationException {
		Player player = objenesis.newInstance(Player.class);
		PlayerCommonData commonData = new PlayerCommonData(level);
		setField(commonData, "level", level);
		setField(player, "playerCommonData", commonData);
		return player;
	}

	private static EventsWindow eventWindow(int id, int minLevel, int maxLevel) throws Exception {
		EventsWindow eventWindow = new EventsWindow();
		setField(eventWindow, "id", id);
		setField(eventWindow, "min_level", minLevel);
		setField(eventWindow, "max_level", maxLevel);
		setField(eventWindow, "pStart", xmlCalendar(ZonedDateTime.now().minusDays(1)));
		setField(eventWindow, "pEnd", xmlCalendar(ZonedDateTime.now().plusDays(1)));
		return eventWindow;
	}

	private static Object xmlCalendar(ZonedDateTime dateTime) throws Exception {
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(dateTime));
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

}
