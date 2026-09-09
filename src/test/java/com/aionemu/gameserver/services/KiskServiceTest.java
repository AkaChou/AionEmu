package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;

class KiskServiceTest {

	@Test
	void playerKiskStateUsesConcurrentMaps() throws ReflectiveOperationException {
		KiskService service = new KiskService();

		assertInstanceOf(ConcurrentMap.class, fieldValue(service, "boundButOfflinePlayer"));
		assertInstanceOf(ConcurrentMap.class, fieldValue(service, "ownerPlayer"));
	}

	private static Object fieldValue(KiskService service, String fieldName) throws ReflectiveOperationException {
		Field field = KiskService.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(service);
	}
}
