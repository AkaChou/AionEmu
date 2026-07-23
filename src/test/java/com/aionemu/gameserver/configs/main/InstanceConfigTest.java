package com.aionemu.gameserver.configs.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.aionemu.commons.configuration.ConfigurableProcessor;

class InstanceConfigTest {

	@Test
	void bindsAndParsesInstanceProperties() throws Exception {
		boolean allowSoloEntry = InstanceConfig.ALLOW_SOLO_ENTRY;
		int specialServerCond = InstanceConfig.SPECIAL_SERVER_COND;
		double cooldownRate = InstanceConfig.COOLDOWN_RATE;
		int destroyDelay = InstanceConfig.DESTROY_DELAY_SECONDS;
		int soloDestroyDelay = InstanceConfig.SOLO_DESTROY_DELAY_SECONDS;
		boolean scalingEnable = InstanceConfig.SCALING_ENABLE;
		float hpFloor = InstanceConfig.SCALING_HP_FLOOR;
		float dmgFloor = InstanceConfig.SCALING_DMG_FLOOR;
		String cooldownMaps = getPrivateString("cooldownExcludedMaps");
		String scalingMaps = getPrivateString("scalingExcludedMaps");
		Properties properties = new Properties();
		properties.setProperty("gameserver.instance.allow_solo_entry", "false");
		properties.setProperty("gameserver.instance.special_server_cond", "1");
		properties.setProperty("gameserver.instances.cooldown.rate", "0.01");
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

			assertFalse(InstanceConfig.ALLOW_SOLO_ENTRY);
			assertEquals(1, InstanceConfig.SPECIAL_SERVER_COND);
			assertEquals(0.01, InstanceConfig.COOLDOWN_RATE);
			assertTrue(InstanceConfig.isCooldownExcluded(300080000));
			assertFalse(InstanceConfig.isCooldownExcluded(300060000));
			assertEquals(90, InstanceConfig.DESTROY_DELAY_SECONDS);
			assertEquals(30, InstanceConfig.SOLO_DESTROY_DELAY_SECONDS);
			assertTrue(InstanceConfig.SCALING_ENABLE);
			assertEquals(0.4f, InstanceConfig.SCALING_HP_FLOOR);
			assertEquals(0.6f, InstanceConfig.SCALING_DMG_FLOOR);
			assertTrue(InstanceConfig.isScalingExcluded(300060000));
		} finally {
			InstanceConfig.ALLOW_SOLO_ENTRY = allowSoloEntry;
			InstanceConfig.SPECIAL_SERVER_COND = specialServerCond;
			InstanceConfig.COOLDOWN_RATE = cooldownRate;
			InstanceConfig.DESTROY_DELAY_SECONDS = destroyDelay;
			InstanceConfig.SOLO_DESTROY_DELAY_SECONDS = soloDestroyDelay;
			InstanceConfig.SCALING_ENABLE = scalingEnable;
			InstanceConfig.SCALING_HP_FLOOR = hpFloor;
			InstanceConfig.SCALING_DMG_FLOOR = dmgFloor;
			setPrivateString("cooldownExcludedMaps", cooldownMaps);
			setPrivateString("scalingExcludedMaps", scalingMaps);
			InstanceConfig.refresh();
		}
	}

	@Test
	void rejectsInvalidSpecialServerCondition() {
		int specialServerCond = InstanceConfig.SPECIAL_SERVER_COND;
		try {
			InstanceConfig.SPECIAL_SERVER_COND = 2;
			assertThrows(IllegalArgumentException.class, InstanceConfig::refresh);
		} finally {
			InstanceConfig.SPECIAL_SERVER_COND = specialServerCond;
			InstanceConfig.refresh();
		}
	}

	@Test
	void rejectsInvalidCooldownRates() {
		double cooldownRate = InstanceConfig.COOLDOWN_RATE;
		try {
			for (double invalid : new double[] { -0.01, 0.001, 1.01, Double.NaN }) {
				InstanceConfig.COOLDOWN_RATE = invalid;
				assertThrows(IllegalArgumentException.class, InstanceConfig::refresh);
			}
		} finally {
			InstanceConfig.COOLDOWN_RATE = cooldownRate;
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
