package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class DataManagerTest {

	@Test
	void itemAndSkillDataLoadingStartBeforeMainStaticDataFinishes() {
		StaticData staticData = new StaticData();
		ItemData itemData = new ItemData();
		SkillData skillData = new SkillData();
		CountDownLatch itemStarted = new CountDownLatch(1);
		CountDownLatch skillStarted = new CountDownLatch(1);
		Map<String, Long> capturedTimings = new ConcurrentHashMap<>();
		XmlDataLoader loader = new XmlDataLoader() {
			@Override
			public StaticData loadStaticData(Supplier<SkillData> skillDataSupplier,
				ConcurrentMap<String, Long> phaseTimings) {
				assertTrue(await(itemStarted), "item data loading should start while main static data is still loading");
				assertTrue(await(skillStarted), "skill data loading should start while main static data is still loading");
				staticData.skillData = skillDataSupplier.get();
				return staticData;
			}

			@Override
			public ItemData loadItemData() {
				itemStarted.countDown();
				return itemData;
			}

			@Override
			public SkillData loadSkillData(ConcurrentMap<String, Long> phaseTimings) {
				phaseTimings.put("SkillStreamJaxbWork", 0L);
				skillStarted.countDown();
				return skillData;
			}

			@Override
			public void logStaticDataPhaseTimings(Map<String, Long> phaseTimings) {
				capturedTimings.putAll(phaseTimings);
			}
		};

		DataManager.LoadedStaticData loaded = DataManager.loadStaticData(loader);

		assertSame(staticData, loaded.staticData());
		assertSame(itemData, loaded.itemData());
		assertSame(skillData, loaded.staticData().skillData);
		assertNotNull(loaded);
		assertTrue(capturedTimings.containsKey("ItemData"));
		assertTrue(capturedTimings.containsKey("SkillData"));
		assertTrue(capturedTimings.containsKey("SkillStreamJaxbWork"));
	}

	@Test
	void staticDataFailureIsPropagatedInsteadOfReturningPartialData() {
		IllegalStateException expected = new IllegalStateException("static data failed");
		XmlDataLoader loader = new XmlDataLoader() {
			@Override
			public StaticData loadStaticData(Supplier<SkillData> skillDataSupplier,
				ConcurrentMap<String, Long> phaseTimings) {
				throw expected;
			}

			@Override
			public ItemData loadItemData() {
				return new ItemData();
			}

			@Override
			public SkillData loadSkillData(ConcurrentMap<String, Long> phaseTimings) {
				return new SkillData();
			}
		};

		assertSame(expected, assertThrows(IllegalStateException.class, () -> DataManager.loadStaticData(loader)));
	}

	@Test
	void duplicateConstructionFailsFastBeforeLoadingData() throws Exception {
		AtomicBoolean constructed = constructionGuard();
		constructed.set(true);

		try {
			IllegalStateException thrown = assertThrows(IllegalStateException.class, DataManager::new);

			assertTrue(thrown.getMessage().contains("Duplicate"));
		} finally {
			constructed.set(false);
		}
	}

	private AtomicBoolean constructionGuard() throws ReflectiveOperationException {
		Field field = DataManager.class.getDeclaredField("CONSTRUCTED");
		field.setAccessible(true);
		return (AtomicBoolean) field.get(null);
	}

	private boolean await(CountDownLatch latch) {
		try {
			return latch.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
