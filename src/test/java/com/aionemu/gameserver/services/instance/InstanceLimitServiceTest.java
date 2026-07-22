package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.model.gameobjects.player.PlayerInstanceLimit;

class InstanceLimitServiceTest {

	@Test
	void calculatesRetailResetTimesInChinaZone() {
		long monday0830 = ZonedDateTime.of(2026, 7, 20, 8, 30, 0, 0, InstanceLimitService.RETAIL_ZONE)
				.toInstant().toEpochMilli();
		assertEquals(ZonedDateTime.of(2026, 7, 20, 9, 0, 0, 0, InstanceLimitService.RETAIL_ZONE)
				.toInstant().toEpochMilli(), InstanceLimitService.nextReset(row("Daily", "0900", ""), monday0830));
		assertEquals(ZonedDateTime.of(2026, 7, 22, 9, 0, 0, 0, InstanceLimitService.RETAIL_ZONE)
				.toInstant().toEpochMilli(), InstanceLimitService.nextReset(row("Weekly", "0900", "Wed,Fri"), monday0830));
		assertEquals(monday0830 + 45 * 60_000L,
				InstanceLimitService.nextReset(row("Relative", "45", ""), monday0830));
		assertEquals(monday0830 + 7080 * 60_000L,
				InstanceLimitService.nextReset(row("Daily", "7080", ""), monday0830));
		assertEquals(monday0830 + 18_000L,
				InstanceLimitService.nextReset(row("Daily", "0900", ""), monday0830, 0.01));
	}

	@Test
	void sharesSyncKeysAndCarriesUnusedEntriesWithinCap() {
		DataManager.RETAIL_INSTANCE_DATA = RetailInstanceData.load(
				new File("src/main/resources/aion/definitions/compact/instance"),
				new File("src/main/resources/aion/definitions/schemas/retail-instance-data.xsd"));
		assertEquals(InstanceLimitService.limitKey(300020000), InstanceLimitService.limitKey(900130000));

		long now = ZonedDateTime.of(2026, 7, 20, 10, 0, 0, 0, InstanceLimitService.RETAIL_ZONE)
				.toInstant().toEpochMilli();
		PlayerInstanceLimit limit = new PlayerInstanceLimit(52, now - 1, 1, 2, 3, 3);
		Row cooldown = new Row(Map.of("type", "Daily", "value", "0900", "maxcount", "3",
				"extra_count_buildup", "2", "extra_count_buildup_level", "3"));
		InstanceLimitService.refresh(limit, cooldown, now, 1);
		assertEquals(0, limit.getUsed());
		assertEquals(3, limit.getBonusAvailable());
		assertEquals(0, limit.getPurchasedCount());
		assertEquals(0, limit.getPurchaseStep());
	}

	private static Row row(String type, String value, String typeValue) {
		return new Row(Map.of("type", type, "value", value, "typevalue", typeValue));
	}
}
