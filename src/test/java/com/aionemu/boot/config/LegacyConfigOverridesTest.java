package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
