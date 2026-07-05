package com.aionemu.gameserver.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.model.templates.event.EventsWindow;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EVENT_WINDOW_ITEMS;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class SMEventWindowItemsTest {

	@Test
	void constructorCopiesActiveEventsCollection() throws ReflectiveOperationException {
		List<EventsWindow> activeEvents = new ArrayList<EventsWindow>();
		activeEvents.add(new EventsWindow());

		SM_EVENT_WINDOW_ITEMS packet = new SM_EVENT_WINDOW_ITEMS(activeEvents);
		activeEvents.clear();

		assertEquals(1, activeEvents(packet).size());
	}

	@SuppressWarnings("unchecked")
	private static Collection<EventsWindow> activeEvents(SM_EVENT_WINDOW_ITEMS packet) throws ReflectiveOperationException {
		Field field = SM_EVENT_WINDOW_ITEMS.class.getDeclaredField("active_events_packet");
		field.setAccessible(true);
		return (Collection<EventsWindow>) field.get(packet);
	}
}
