package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class DataManagerTest {

	@Test
	void itemDataLoadingStartsBeforeMainStaticDataFinishes() {
		StaticData staticData = new StaticData();
		ItemData itemData = new ItemData();
		CountDownLatch itemStarted = new CountDownLatch(1);
		CountDownLatch staticFinished = new CountDownLatch(1);
		AtomicBoolean itemStartedBeforeStaticReturned = new AtomicBoolean();
		XmlDataLoader loader = new XmlDataLoader() {
			@Override
			public StaticData loadStaticData() {
				assertTrue(await(itemStarted), "item data loading should start while main static data is still loading");
				itemStartedBeforeStaticReturned.set(true);
				staticFinished.countDown();
				return staticData;
			}

			@Override
			public ItemData loadItemData() {
				itemStarted.countDown();
				assertTrue(await(staticFinished), "item data loading should overlap main static data loading");
				return itemData;
			}
		};

		DataManager.LoadedStaticData loaded = DataManager.loadStaticData(loader);

		assertTrue(itemStartedBeforeStaticReturned.get());
		assertSame(staticData, loaded.staticData());
		assertSame(itemData, loaded.itemData());
		assertNotNull(loaded);
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
