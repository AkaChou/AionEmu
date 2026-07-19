package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class DataManagerTest {

	@Test
	void itemSkillAndRetailInstanceDataLoadingStartBeforeMainStaticDataFinishes() {
		StaticData staticData = new StaticData();
		ItemData itemData = new ItemData();
		SkillData skillData = new SkillData();
		RetailInstanceData retailInstanceData = RetailInstanceData.load(
			new File("src/main/resources/aion/definitions/compact/instance"),
			new File("src/main/resources/aion/definitions/schemas/retail-instance-data.xsd"));
		CountDownLatch itemStarted = new CountDownLatch(1);
		CountDownLatch skillStarted = new CountDownLatch(1);
		CountDownLatch retailInstanceStarted = new CountDownLatch(1);
		XmlDataLoader loader = new XmlDataLoader() {
			@Override
			public StaticData loadStaticData(Supplier<SkillData> skillDataSupplier) {
				assertTrue(await(itemStarted), "item data loading should start while main static data is still loading");
				assertTrue(await(skillStarted), "skill data loading should start while main static data is still loading");
				assertTrue(await(retailInstanceStarted),
					"retail instance data loading should start while main static data is still loading");
				staticData.skillData = skillDataSupplier.get();
				return staticData;
			}

			@Override
			public ItemData loadItemData() {
				itemStarted.countDown();
				return itemData;
			}

			@Override
			public SkillData loadSkillData() {
				skillStarted.countDown();
				return skillData;
			}

			@Override
			public RetailInstanceData loadRetailInstanceData() {
				retailInstanceStarted.countDown();
				return retailInstanceData;
			}
		};

		DataManager.LoadedStaticData loaded = DataManager.loadStaticData(loader);

		assertSame(staticData, loaded.staticData());
		assertSame(itemData, loaded.itemData());
		assertSame(skillData, loaded.staticData().skillData);
		assertSame(retailInstanceData, loaded.retailInstanceData());
		assertNotNull(loaded);
	}

	@Test
	void staticDataAssignmentFailureAbortsStartup() {
		RuntimeException cause = new RuntimeException("boom");
		CompletableFuture<Void> future = new CompletableFuture<>();
		future.completeExceptionally(cause);

		IllegalStateException thrown = assertThrows(IllegalStateException.class,
			() -> DataManager.awaitStaticDataAssignment(future));

		assertSame(cause, thrown.getCause());
	}

	@Test
	void staticDataAssignmentInterruptRestoresInterruptFlag() {
		CompletableFuture<Void> future = new CompletableFuture<>();
		Thread.currentThread().interrupt();

		try {
			assertThrows(IllegalStateException.class, () -> DataManager.awaitStaticDataAssignment(future));
			assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
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
