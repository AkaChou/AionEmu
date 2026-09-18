package com.aionemu.gameserver.configs.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.testutil.ConfigSnapshot;

class InstanceConfigTest {

	@Test
	void bindsAndParsesInstanceProperties() throws Exception {
		ConfigSnapshot snapshot = ConfigSnapshot.of(InstanceConfig.class,
			"COOLDOWN_RATE", "DESTROY_DELAY_SECONDS", "SOLO_DESTROY_DELAY_SECONDS",
			"SCALING_ENABLE", "SCALING_HP_FLOOR", "SCALING_DMG_FLOOR");
		String cooldownMaps = getPrivateString("cooldownExcludedMaps");
		String scalingMaps = getPrivateString("scalingExcludedMaps");
		Properties properties = new Properties();
		properties.setProperty("gameserver.instances.cooldown.rate", "3");
		properties.setProperty("gameserver.instances.cooldown.filter", "300080000, 0");
		properties.setProperty("gameserver.instance.destroy_delay_seconds", "90");
		properties.setProperty("gameserver.instance.solo.destroy_delay_seconds", "30");
		properties.setProperty("gameserver.instance.scaling.enable", "true");
		properties.setProperty("gameserver.instance.scaling.hp_floor", "0.4");
		properties.setProperty("gameserver.instance.scaling.dmg_floor", "0.6");
		properties.setProperty("gameserver.instance.scaling.excluded_maps", "300060000");

		try {
			ConfigurableProcessor.process(InstanceConfig.class, properties);
			InstanceConfig.refresh();

			assertEquals(3, InstanceConfig.COOLDOWN_RATE);
			assertTrue(InstanceConfig.isCooldownExcluded(300080000));
			assertFalse(InstanceConfig.isCooldownExcluded(300060000));
			assertEquals(90, InstanceConfig.DESTROY_DELAY_SECONDS);
			assertEquals(30, InstanceConfig.SOLO_DESTROY_DELAY_SECONDS);
			assertTrue(InstanceConfig.SCALING_ENABLE);
			assertEquals(0.4f, InstanceConfig.SCALING_HP_FLOOR);
			assertEquals(0.6f, InstanceConfig.SCALING_DMG_FLOOR);
			assertTrue(InstanceConfig.isScalingExcluded(300060000));
		} finally {
			snapshot.restore();
			setPrivateString("cooldownExcludedMaps", cooldownMaps);
			setPrivateString("scalingExcludedMaps", scalingMaps);
			InstanceConfig.refresh();
		}
	}

	private static String getPrivateString(String name) throws Exception {
		var field = InstanceConfig.class.getDeclaredField(name);
		field.setAccessible(true);
		return (String) field.get(null);
	}

	private static void setPrivateString(String name, String value) throws Exception {
		var field = InstanceConfig.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(null, value);
	}
}
