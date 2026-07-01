package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ServiceInternalCollectionImplementationTest {

	@Test
	void surveyServiceUsesJdkMapInterfaceForActiveItems() throws Exception {
		assertEquals(Map.class, SurveyService.class.getDeclaredField("activeItems").getType());
	}

	@Test
	void eventServiceUsesJdkMapsForQuestIndexes() throws Exception {
		assertEquals(Map.class, EventService.class.getDeclaredField("eventsForStartQuest").getType());
		assertEquals(Map.class, EventService.class.getDeclaredField("eventsForMaintainQuest").getType());

		Method method = EventService.class.getDeclaredMethod("StartOrMaintainQuests", playerClass(), ListIteratorClass(),
				Map.class, boolean.class);
		assertEquals(Map.class, method.getParameterTypes()[2]);
	}

	@Test
	void siegeServiceUsesJdkMapsForTemporaryWorldViews() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/SiegeService.java"));

		assertFalse(source.contains("new FastMap<Integer, SiegeLocation>()"));
		assertFalse(source.contains("new FastMap<Integer, ArtifactLocation>()"));
	}

	private Class<?> playerClass() throws ClassNotFoundException {
		return Class.forName("com.aionemu.gameserver.model.gameobjects.player.Player");
	}

	private Class<?> ListIteratorClass() {
		return java.util.ListIterator.class;
	}
}
