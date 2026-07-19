package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.model.gameobjects.player.PlayerInstanceLimit;

class LunaInstanceServiceTest {

	@Test
	void appliesFreeTurnsPaidStepsAndHardCap() {
		long now = time(2026, 7, 20, 10, 0);
		PlayerInstanceLimit limit = new PlayerInstanceLimit(LunaInstanceService.limitKey(45), time(2026, 7, 21, 9, 0),
				0, 0, 0, 0);
		Row dungeon = row(Map.of("price_ratio", "0"));
		Row price = row(Map.of("free_turn", "1", "price_max_count", "1", "price01", "20",
				"reset_type", "Daily", "value", "900"));

		assertEquals(0, LunaInstanceService.status(dungeon, price, limit, 75, now).price());
		limit.setUsed(1);
		assertEquals(20, LunaInstanceService.status(dungeon, price, limit, 75, now).price());
		limit.setUsed(2);
		assertFalse(LunaInstanceService.status(dungeon, price, limit, 75, now).allowed());
	}

	@Test
	void appliesRetailLevelPriceRatioFormula() {
		long now = time(2026, 7, 20, 10, 0);
		PlayerInstanceLimit limit = new PlayerInstanceLimit(LunaInstanceService.limitKey(47), time(2026, 7, 22, 9, 0),
				1, 0, 0, 0);
		Row dungeon = row(Map.of("price_ratio", "10"));
		Row price = row(Map.of("free_turn", "1", "price_max_count", "5", "price01", "20",
				"reset_type", "Weekly", "type_value", "wed", "value", "900"));

		assertEquals(24, LunaInstanceService.status(dungeon, price, limit, 21, now).price());
	}

	@Test
	void resetsDailyAndWeeklyCountsInChinaZone() {
		long monday0830 = time(2026, 7, 20, 8, 30);
		assertEquals(time(2026, 7, 20, 9, 0), LunaInstanceService.nextReset(
				row(Map.of("reset_type", "Daily", "value", "900")), monday0830));
		assertEquals(time(2026, 7, 22, 9, 0), LunaInstanceService.nextReset(
				row(Map.of("reset_type", "Weekly", "type_value", "wed", "value", "900")), monday0830));

		PlayerInstanceLimit limit = new PlayerInstanceLimit(LunaInstanceService.limitKey(45), monday0830 - 1,
				2, 1, 1, 1);
		LunaInstanceService.refresh(row(Map.of("reset_type", "Daily", "value", "900")), limit, monday0830);
		assertEquals(0, limit.getUsed());
		assertEquals(0, limit.getBonusAvailable());
		assertEquals(0, limit.getPurchasedCount());
	}

	@Test
	void usesRetailHourlyMinuteSchedule() {
		assertTrue(LunaInstanceService.isOpen(row(Map.of("active", "1", "mon_am8", "60")),
				time(2026, 7, 20, 8, 59)));
		assertFalse(LunaInstanceService.isOpen(row(Map.of("active", "1", "mon_am8", "30")),
				time(2026, 7, 20, 8, 30)));
	}

	private static Row row(Map<String, String> values) {
		return new Row(values);
	}

	private static long time(int year, int month, int day, int hour, int minute) {
		return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, InstanceLimitService.RETAIL_ZONE)
				.toInstant().toEpochMilli();
	}
}
