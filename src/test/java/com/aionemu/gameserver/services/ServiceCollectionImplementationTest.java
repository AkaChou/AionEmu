package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.utils.audit.GMService;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;

class ServiceCollectionImplementationTest {

	@Test
	void utilityServicesUseJdkMaps() throws Exception {
		assertHashMap(new ChatProcessor(), "commands");
		assertHashMap(new ChatProcessor(), "accessLevel");
		String announceLevels = AdminConfig.ANNOUNCE_LEVEL_LIST;
		try {
			AdminConfig.ANNOUNCE_LEVEL_LIST = "*";
			assertHashMap(GMService.getInstance(), "gms");
		} finally {
			AdminConfig.ANNOUNCE_LEVEL_LIST = announceLevels;
		}
	}

	@Test
	void serviceListFieldsExposeJdkListInterface() throws Exception {
		assertFieldType(AdminService.class, "list", List.class);
		assertFieldType(CuringZoneService.class, "curingObjects", List.class);
		assertFieldType(SpringZoneService.class, "springObjects", List.class);
	}

	private void assertFieldType(Class<?> type, String fieldName, Class<?> expectedType) throws Exception {
		assertEquals(expectedType, type.getDeclaredField(fieldName).getType());
	}

	private void assertHashMap(Object target, String fieldName) throws Exception {
		assertFieldImplementation(target, fieldName, HashMap.class);
	}

	private void assertFieldImplementation(Object target, String fieldName, Class<?> expectedType) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);

		assertEquals(expectedType, field.get(target).getClass());
	}
}
