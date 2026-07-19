package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
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

	private void assertHashMap(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);

		assertEquals(HashMap.class, field.get(target).getClass());
	}

}
