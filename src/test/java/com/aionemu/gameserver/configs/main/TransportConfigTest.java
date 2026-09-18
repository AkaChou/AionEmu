package com.aionemu.gameserver.configs.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.testutil.ConfigSnapshot;

class TransportConfigTest {

	@Test
	void usesDefaultTransportTimes() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(TransportConfig.class,
			"HOTSPOT_CAST_TIME_SECONDS", "HOTSPOT_COOLDOWN_SECONDS");

		try {
			ConfigurableProcessor.process(TransportConfig.class, new Properties());
			TransportConfig.refresh();

			assertEquals(2, TransportConfig.HOTSPOT_CAST_TIME_SECONDS);
			assertEquals(5, TransportConfig.HOTSPOT_COOLDOWN_SECONDS);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsCustomTransportTimes() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(TransportConfig.class,
			"HOTSPOT_CAST_TIME_SECONDS", "HOTSPOT_COOLDOWN_SECONDS");
		Properties properties = new Properties();
		properties.setProperty("gameserver.transport.hotspot.cast_time_seconds", "7");
		properties.setProperty("gameserver.transport.hotspot.cooldown_seconds", "12");

		try {
			ConfigurableProcessor.process(TransportConfig.class, properties);
			TransportConfig.refresh();

			assertEquals(7, TransportConfig.HOTSPOT_CAST_TIME_SECONDS);
			assertEquals(12, TransportConfig.HOTSPOT_COOLDOWN_SECONDS);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void rejectsNegativeTransportTimes() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(TransportConfig.class, "HOTSPOT_COOLDOWN_SECONDS");
		Properties properties = new Properties();
		properties.setProperty("gameserver.transport.hotspot.cooldown_seconds", "-1");

		try {
			ConfigurableProcessor.process(TransportConfig.class, properties);
			assertThrows(IllegalArgumentException.class, TransportConfig::refresh);
		} finally {
			snapshot.restore();
		}
	}
}
