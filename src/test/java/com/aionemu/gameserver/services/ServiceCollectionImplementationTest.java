package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.instance.EngulfedOphidanBridgeService;
import com.aionemu.gameserver.services.instance.GrandArenaTrainingCampService;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;
import com.aionemu.gameserver.services.instance.IDRunService;
import com.aionemu.gameserver.services.instance.IdgelDomeLandmarkService;
import com.aionemu.gameserver.services.instance.IdgelDomeService;
import com.aionemu.gameserver.services.instance.IronWallWarfrontService;
import com.aionemu.gameserver.services.instance.KamarBattlefieldService;
import com.aionemu.gameserver.services.instance.SuspiciousOphidanBridgeService;
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
		assertFieldType(AsyunatarService.class, "playersWithCooldown", List.class);
		assertFieldType(DredgionService2.class, "playersWithCooldown", List.class);
		assertFieldType(EngulfedOphidanBridgeService.class, "playersWithCooldown", List.class);
		assertFieldType(GrandArenaTrainingCampService.class, "playersWithCooldown", List.class);
		assertFieldType(HallOfTenacityService.class, "playersWithCooldown", List.class);
		assertFieldType(IDRunService.class, "playersWithCooldown", List.class);
		assertFieldType(IdgelDomeLandmarkService.class, "playersWithCooldown", List.class);
		assertFieldType(IdgelDomeService.class, "playersWithCooldown", List.class);
		assertFieldType(IronWallWarfrontService.class, "playersWithCooldown", List.class);
		assertFieldType(KamarBattlefieldService.class, "playersWithCooldown", List.class);
		assertFieldType(SuspiciousOphidanBridgeService.class, "playersWithCooldown", List.class);
	}

	@Test
	void instanceCooldownServicesUseArrayLists() throws Exception {
		assertArrayList(new AsyunatarService(), "playersWithCooldown");
		assertArrayList(new EngulfedOphidanBridgeService(), "playersWithCooldown");
		assertArrayList(new GrandArenaTrainingCampService(), "playersWithCooldown");
		assertArrayList(new HallOfTenacityService(), "playersWithCooldown");
		assertArrayList(new IDRunService(), "playersWithCooldown");
		assertArrayList(new IdgelDomeLandmarkService(), "playersWithCooldown");
		assertArrayList(new IdgelDomeService(), "playersWithCooldown");
		assertArrayList(new IronWallWarfrontService(), "playersWithCooldown");
		assertArrayList(new KamarBattlefieldService(), "playersWithCooldown");
		assertArrayList(new SuspiciousOphidanBridgeService(), "playersWithCooldown");
	}

	private void assertFieldType(Class<?> type, String fieldName, Class<?> expectedType) throws Exception {
		assertEquals(expectedType, type.getDeclaredField(fieldName).getType());
	}

	private void assertHashMap(Object target, String fieldName) throws Exception {
		assertFieldImplementation(target, fieldName, HashMap.class);
	}

	private void assertArrayList(Object target, String fieldName) throws Exception {
		assertFieldImplementation(target, fieldName, ArrayList.class);
	}

	private void assertFieldImplementation(Object target, String fieldName, Class<?> expectedType) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);

		assertEquals(expectedType, field.get(target).getClass());
	}
}
