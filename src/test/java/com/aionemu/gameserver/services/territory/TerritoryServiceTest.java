package com.aionemu.gameserver.services.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TerritoryServiceTest {

	@Test
	void storesBuffsInJdkMap() throws Exception {
		TerritoryService service = new TerritoryService();

		Map<Integer, TerritoryBuff> buffs = buffs(service);

		assertEquals(HashMap.class, buffs.getClass());
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, TerritoryBuff> buffs(TerritoryService service) throws Exception {
		Field field = TerritoryService.class.getDeclaredField("buffs");
		field.setAccessible(true);
		return (Map<Integer, TerritoryBuff>) field.get(service);
	}
}
