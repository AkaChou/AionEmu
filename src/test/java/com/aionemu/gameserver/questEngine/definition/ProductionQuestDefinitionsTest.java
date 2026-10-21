package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductionQuestDefinitionsTest {

	private QuestCatalog originalCatalog;
	private Object originalOverlayView;
	private java.util.concurrent.Future<QuestCatalog> originalStartupPreload;
	private QuestCatalog originalStartupCatalog;

	@BeforeEach
	void captureCacheState() throws ReflectiveOperationException {
		originalCatalog = (QuestCatalog) field("catalog").get(null);
		originalOverlayView = field("overlayView").get(null);
		@SuppressWarnings("unchecked")
		java.util.concurrent.Future<QuestCatalog> preload =
			(java.util.concurrent.Future<QuestCatalog>) field("startupPreload").get(null);
		originalStartupPreload = preload;
		originalStartupCatalog = (QuestCatalog) field("startupCatalog").get(null);
		resetCache();
	}

	@AfterEach
	void restoreCacheState() throws ReflectiveOperationException {
		field("catalog").set(null, originalCatalog);
		field("overlayView").set(null, originalOverlayView);
		field("startupPreload").set(null, originalStartupPreload);
		field("startupCatalog").set(null, originalStartupCatalog);
	}

	@Test
	void preloadedCatalogIsSharedByAllReaders() {
		QuestCatalog expected = new ImmutableQuestCatalog(List.of());
		AtomicInteger compiles = new AtomicInteger();

		ProductionQuestDefinitions.preload(() -> {
			compiles.incrementAndGet();
			return expected;
		});

		assertSame(expected, ProductionQuestDefinitions.catalog());
		assertSame(expected, ProductionQuestDefinitions.catalog(() -> {
			throw new AssertionError("fallback compiler must not run after preload");
		}));
		ProductionQuestDefinitions.preload(() -> {
			compiles.incrementAndGet();
			return new ImmutableQuestCatalog(List.of());
		});

		assertEquals(1, compiles.get());
	}

	@Test
	void compiledCatalogIsReusedByTheOrdinaryReader() {
		QuestCatalog expected = new ImmutableQuestCatalog(List.of());
		AtomicInteger compiles = new AtomicInteger();

		assertSame(expected, ProductionQuestDefinitions.catalog(() -> {
			compiles.incrementAndGet();
			return expected;
		}));
		assertSame(expected, ProductionQuestDefinitions.catalog());

		assertEquals(1, compiles.get());
	}

	private void resetCache() throws ReflectiveOperationException {
		field("catalog").set(null, null);
		field("overlayView").set(null, null);
		field("startupPreload").set(null, null);
		field("startupCatalog").set(null, null);
	}

	private static java.lang.reflect.Field field(String name) throws NoSuchFieldException {
		java.lang.reflect.Field field = ProductionQuestDefinitions.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}
}
