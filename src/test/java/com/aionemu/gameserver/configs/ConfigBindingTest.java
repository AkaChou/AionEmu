package com.aionemu.gameserver.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import com.aionemu.commons.configs.DatabaseConfig;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.gameserver.configs.administration.PanelConfig;
import com.aionemu.gameserver.configs.main.BrokerConfig;
import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.configs.main.PvPConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConfigBindingTest {
	@AfterEach
	void resetDefaults() {
		Properties defaults = new Properties();
		ConfigurableProcessor.process(PanelConfig.class, defaults);
		ConfigurableProcessor.process(BrokerConfig.class, defaults);
		ConfigurableProcessor.process(EventsConfig.class, defaults);
		ConfigurableProcessor.process(PvPConfig.class, defaults);
		ConfigurableProcessor.process(DatabaseConfig.class, defaults);
	}

	@Test
	void bindsRestoredConfigurationKeys() {
		Properties properties = new Properties();
		properties.setProperty("gameserver.administration.startquestpanel", "4");
		properties.setProperty("gameserver.broker.anti.hack.punishment", "1");
		properties.setProperty("gameserver.broker.items.expire.time", "12");
		properties.setProperty("gameserver.event.abyss.rewards", "187050043,187050041");
		properties.setProperty("gameserver.pvp.raw.killcount.uniquemonster", "36");
		properties.setProperty("database.hikari.maxLifetime", "600000");
		properties.setProperty("database.hikari.connectionTestQuery", "SELECT 2");

		ConfigurableProcessor.process(PanelConfig.class, properties);
		ConfigurableProcessor.process(BrokerConfig.class, properties);
		ConfigurableProcessor.process(EventsConfig.class, properties);
		ConfigurableProcessor.process(PvPConfig.class, properties);
		ConfigurableProcessor.process(DatabaseConfig.class, properties);

		assertEquals(4, PanelConfig.STARTQUEST_PANEL_LEVEL);
		assertEquals(1, BrokerConfig.ANTI_HACK_PUNISHMENT);
		assertEquals(12, BrokerConfig.ITEMS_EXPIRE_TIME);
		assertEquals("187050043,187050041", EventsConfig.ABYSS_EVENT_REWARDS);
		assertEquals(36, PvPConfig.INSANEMONSTER_KILL_COUNT);
		assertEquals(600000, DatabaseConfig.HIKARI_MAX_LIFETIME);
		assertEquals("SELECT 2", DatabaseConfig.HIKARI_CONNECTION_TEST_QUERY);
	}
}
