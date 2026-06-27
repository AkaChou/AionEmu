package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;
import com.aionemu.gameserver.model.gameobjects.player.motion.MotionList;

class PlayerLazyMapImplementationTest {

	@Test
	void craftCooldownsUseJdkMapWhenInitialized() throws Exception {
		CraftCooldownList cooldowns = new CraftCooldownList(null);

		cooldowns.addCraftCooldown(1, 60);

		assertHashMap(cooldowns, "craftCooldowns");
	}

	@Test
	void houseObjectCooldownsUseJdkMapWhenInitialized() throws Exception {
		HouseObjectCooldownList cooldowns = new HouseObjectCooldownList(null);

		cooldowns.addHouseObjectCooldown(1, 60);

		assertHashMap(cooldowns, "houseObjectCooldowns");
	}

	@Test
	void motionsUseJdkMapsWhenInitialized() throws Exception {
		MotionList motions = new MotionList(null);

		motions.add(new Motion(1, 0, false), false);
		motions.add(new Motion(2, 0, true), false);

		assertHashMap(motions, "motions");
		assertHashMap(motions, "activeMotions");
	}

	@Test
	void portalCooldownsExposeJdkMapInterface() throws Exception {
		assertEquals(Map.class, PortalCooldownList.class.getDeclaredField("portalCooldowns").getType());
		assertEquals(Map.class, PortalCooldownList.class.getDeclaredMethod("getPortalCoolDowns").getReturnType());
		assertEquals(Map.class, portalCooldownSetterParameterType());
	}

	private void assertHashMap(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);

		assertEquals(HashMap.class, field.get(target).getClass());
	}

	private Class<?> portalCooldownSetterParameterType() {
		for (java.lang.reflect.Method method : PortalCooldownList.class.getDeclaredMethods()) {
			if (method.getName().equals("setPortalCoolDowns")) {
				return method.getParameterTypes()[0];
			}
		}
		throw new AssertionError("setPortalCoolDowns is missing");
	}
}
