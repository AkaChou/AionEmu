package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
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

        LegacyGameProperties legacyGameProperties = bindLegacyGameProperties(environment);

        Properties properties = new LegacyConfigOverrides(
            legacyGameProperties,
            new AionGameProperties()
        ).gameProperties();

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

        AionGameProperties gameProperties = bindAionGameProperties(environment);

        Properties properties = new LegacyConfigOverrides(
            new LegacyGameProperties(),
            gameProperties
        ).gameProperties();

        assertEquals("false", properties.getProperty("gameserver.startup.progress.enable"));
        assertFalse(properties.containsKey("gameserver.staticdata.progress.enable"));
        assertFalse(properties.containsKey("gameserver.staticdata.summary.log"));
    }

    @Test
    void gamePropertiesKeepLegacyNetworkPropertiesWhenExternalIpIsNotConfigured() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
            "applicationConfig",
            Map.of("aion.legacy.game.property.gameserver.network.login.address", "192.168.1.18:9014")
        ));

        AionGameProperties gameProperties = bindAionGameProperties(environment);
        LegacyGameProperties legacyGameProperties = bindLegacyGameProperties(environment);

        Properties properties = new LegacyConfigOverrides(
            legacyGameProperties,
            gameProperties
        ).gameProperties();

        assertEquals("192.168.1.18:9014", properties.getProperty("gameserver.network.login.address"));
        assertFalse(properties.containsKey("gameserver.network.ipconfig.default"));
    }

    @Test
    void legacyGamePropertiesBindExistingDottedPropertyKeys() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
            "commandLine",
            Map.of(
                "aion.legacy.game.property.gameserver.name", "command-line",
                "aion.legacy.game.property.gameserver.network.port", "7777"
            )
        ));

        LegacyGameProperties properties = bindLegacyGameProperties(environment);

        assertEquals("command-line", properties.getProperty().get("gameserver.name"));
        assertEquals("7777", properties.getProperty().get("gameserver.network.port"));
        assertEquals(2, properties.getProperty().size());
    }

    @Test
    void aionGamePropertiesBindStartupProgressAlias() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
            "applicationConfig",
            Map.of("aion.game.startup.progress.enabled", "false")
        ));

        AionGameProperties properties = bindAionGameProperties(environment);

        assertEquals(Boolean.FALSE, properties.getStartup().getProgress().getEnabled());
    }

    @Test
    void springConfigurationMetadataDescribesStartupProgressProperty() throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("META-INF/spring-configuration-metadata.json")) {
            assertNotNull(stream);
            String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertFalse(metadata.contains("aion.game.static-data.progress.enabled"));
            assertFalse(metadata.contains("aion.game.static-data.summary-log.enabled"));
            assertEquals(true, metadata.contains("\"name\": \"aion.game.startup.progress.enabled\""));
            assertFalse(metadata.contains("aion.game.network.external-ip"));
            assertEquals(true, metadata.contains("\"name\": \"aion.legacy.game.property\""));
        }
    }

    private LegacyGameProperties bindLegacyGameProperties(StandardEnvironment environment) {
        LegacyGameProperties properties = new LegacyGameProperties();
        Binder.get(environment).bind("aion.legacy.game", Bindable.ofInstance(properties));
        return properties;
    }

    private AionGameProperties bindAionGameProperties(StandardEnvironment environment) {
        AionGameProperties properties = new AionGameProperties();
        Binder.get(environment).bind("aion.game", Bindable.ofInstance(properties));
        return properties;
    }
}
