package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class LegacyConfigOverridesTest {

    @Test
    void gamePropertiesCollectLegacyKeysAndKeepSpringPrecedence() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
            "commandLine",
            Map.of(
                "aion.legacy.game.property.gameserver.name", "command-line",
                "aion.services.game.enabled", "false"
            )
        ));
        environment.getPropertySources().addLast(new MapPropertySource(
            "applicationConfig",
            Map.of(
                "aion.legacy.game.property.gameserver.name", "application",
                "aion.legacy.game.property.gameserver.network.port", "7777"
            )
        ));

        Properties properties = new LegacyConfigOverrides(environment).gameProperties();

        assertEquals("command-line", properties.getProperty("gameserver.name"));
        assertEquals("7777", properties.getProperty("gameserver.network.port"));
        assertFalse(properties.containsKey("aion.services.game.enabled"));
        assertFalse(properties.containsKey("aion.legacy.game.property.gameserver.name"));
    }

    @Test
    void gamePropertiesMapStaticDataStartupDisplaySettings() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
            "applicationConfig",
            Map.of(
                "aion.game.static-data.progress.enabled", "false",
                "aion.game.static-data.summary-log.enabled", "true",
                "aion.game.startup.progress.enabled", "false"
            )
        ));

        Properties properties = new LegacyConfigOverrides(environment).gameProperties();

        assertEquals("false", properties.getProperty("gameserver.startup.progress.enable"));
        assertFalse(properties.containsKey("gameserver.staticdata.progress.enable"));
        assertFalse(properties.containsKey("gameserver.staticdata.summary.log"));
    }

    @Test
    void springConfigurationMetadataDescribesStartupProgressProperty() throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("META-INF/spring-configuration-metadata.json")) {
            assertNotNull(stream);
            String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertFalse(metadata.contains("aion.game.static-data.progress.enabled"));
            assertFalse(metadata.contains("aion.game.static-data.summary-log.enabled"));
            assertEquals(true, metadata.contains("\"name\": \"aion.game.startup.progress.enabled\""));
        }
    }
}
