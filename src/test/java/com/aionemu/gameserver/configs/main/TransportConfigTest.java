package com.aionemu.gameserver.configs.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.aionemu.commons.configuration.ConfigurableProcessor;

class TransportConfigTest {

	@Test
	void usesDefaultTransportTimes() {
		int castTime = TransportConfig.HOTSPOT_CAST_TIME_SECONDS;
		int hotspotCooldown = TransportConfig.HOTSPOT_COOLDOWN_SECONDS;

		try {
			ConfigurableProcessor.process(TransportConfig.class, new Properties());
			TransportConfig.refresh();

			assertEquals(2, TransportConfig.HOTSPOT_CAST_TIME_SECONDS);
			assertEquals(5, TransportConfig.HOTSPOT_COOLDOWN_SECONDS);
		} finally {
			TransportConfig.HOTSPOT_CAST_TIME_SECONDS = castTime;
			TransportConfig.HOTSPOT_COOLDOWN_SECONDS = hotspotCooldown;
		}
	}

	@Test
	void bindsCustomTransportTimes() {
		int castTime = TransportConfig.HOTSPOT_CAST_TIME_SECONDS;
		int hotspotCooldown = TransportConfig.HOTSPOT_COOLDOWN_SECONDS;
		Properties properties = new Properties();
		properties.setProperty("gameserver.transport.hotspot.cast_time_seconds", "7");
		properties.setProperty("gameserver.transport.hotspot.cooldown_seconds", "12");

		try {
			ConfigurableProcessor.process(TransportConfig.class, properties);
			TransportConfig.refresh();

			assertEquals(7, TransportConfig.HOTSPOT_CAST_TIME_SECONDS);
			assertEquals(12, TransportConfig.HOTSPOT_COOLDOWN_SECONDS);
		} finally {
			TransportConfig.HOTSPOT_CAST_TIME_SECONDS = castTime;
			TransportConfig.HOTSPOT_COOLDOWN_SECONDS = hotspotCooldown;
		}
	}

	@Test
	void rejectsNegativeTransportTimes() {
		int hotspotCooldown = TransportConfig.HOTSPOT_COOLDOWN_SECONDS;
		Properties properties = new Properties();
		properties.setProperty("gameserver.transport.hotspot.cooldown_seconds", "-1");

		try {
			ConfigurableProcessor.process(TransportConfig.class, properties);
			assertThrows(IllegalArgumentException.class, TransportConfig::refresh);
		} finally {
			TransportConfig.HOTSPOT_COOLDOWN_SECONDS = hotspotCooldown;
		}
	}
}
