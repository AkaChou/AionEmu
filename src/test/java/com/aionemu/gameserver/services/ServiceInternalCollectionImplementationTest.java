package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.Test;

class ServiceInternalCollectionImplementationTest {

	@Test
	void surveyServiceUsesConcurrentMapForActiveItems() throws Exception {
		assertEquals(ConcurrentMap.class, SurveyService.class.getDeclaredField("activeItems").getType());
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

	@Test
	void playerEnterWorldPendingSetUsesConcurrentKeySetWithoutExplicitLock() throws Exception {
		String source = Files
				.readString(Path.of("src/main/java/com/aionemu/gameserver/services/player/PlayerEnterWorldService.java"));

		assertTrue(source.contains("ConcurrentHashMap.newKeySet()"));
		assertFalse(source.contains("synchronized (pendingEnterWorld)"));
	}

	@Test
	void limitedItemTradeServiceUsesCompactHashMapIndexWithoutDoubleLookup() throws Exception {
		String source = Files
				.readString(Path.of("src/main/java/com/aionemu/gameserver/services/LimitedItemTradeService.java"));

		assertTrue(source.contains("new HashMap<Integer, LimitedTradeNpc>()"));
		assertFalse(source.contains("new LinkedHashMap<Integer, LimitedTradeNpc>()"));
		assertFalse(source.contains("if (!limitedTradeNpcs.containsKey(npcId))"));
	}

	@Test
	void dropRegistrationUsesHashSetForNoReductionMapLookups() throws Exception {
		String source = Files
				.readString(Path.of("src/main/java/com/aionemu/gameserver/services/drop/DropRegistrationService.java"));

		assertTrue(source.contains("private Set<Integer> noReductionMaps;"));
		assertTrue(source.contains("noReductionMaps = new HashSet<Integer>();"));
		assertFalse(source.contains("private List<Integer> noReductionMaps;"));
	}

	private Class<?> playerClass() throws ClassNotFoundException {
		return Class.forName("com.aionemu.gameserver.model.gameobjects.player.Player");
	}

	private Class<?> ListIteratorClass() {
		return java.util.ListIterator.class;
	}
}
