package com.aionemu.gameserver.taskmanager.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

class TemporaryTradeTimeTaskTest {

	@Test
	void addTaskIndexesItemByInstanceAndObjectId() {
		TemporaryTradeTimeTask task = new TemporaryTradeTimeTask();
		Item item = newItem(1, 60);

		task.addTask(item, List.of(10, 20));

		assertTrue(task.hasItem(item));
		assertTrue(task.canTrade(item, 10));
		assertFalse(task.canTrade(item, 30));
		assertSame(item, task.getItem(1));
	}

	@Test
	void expiredItemIsRemovedFromBothIndexes() {
		TemporaryTradeTimeTask task = new TemporaryTradeTimeTask();
		Item item = newItem(2, -1);

		task.addTask(item, List.of());
		task.run();

		assertFalse(task.hasItem(item));
		assertFalse(task.canTrade(item, 10));
		assertEquals(0, item.getTemporaryExchangeTime());
		assertEquals(null, task.getItem(2));
	}

	private static Item newItem(int objectId, int secondsFromNow) {
		Item item = new Item(objectId, new TestItemTemplate());
		item.setTemporaryExchangeTime((int) (System.currentTimeMillis() / 1000) + secondsFromNow);
		return item;
	}

	private static final class TestItemTemplate extends ItemTemplate {

		@Override
		public int getActivationCount() {
			return 0;
		}

		@Override
		public int getExpireTime() {
			return 0;
		}

		@Override
		public int getOptionSlotBonus() {
			return 0;
		}

		@Override
		public int getSkinSkill() {
			return 0;
		}

		@Override
		public int getRandomBonusId() {
			return 0;
		}

		@Override
		public int getNameId() {
			return 1;
		}
	}
}
