package com.aionemu.boot.config;

import com.aionemu.gameserver.configs.Config;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

@Component
public class LegacyConfigOverrides {

    static final String GAME_PROPERTY_PREFIX = "aion.legacy.game.property.";
    private static final Map<String, String> GAME_PROPERTY_ALIASES = Map.of(
        "aion.game.startup.progress.enabled", "gameserver.startup.progress.enable"
    );

    private final Environment environment;

    public LegacyConfigOverrides(Environment environment) {
        this.environment = environment;
    }

    public Properties gameProperties() {
        Properties properties = new Properties();
        if (!(environment instanceof ConfigurableEnvironment configurableEnvironment)) {
            return properties;
        }

        for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
            if (!(propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource)) {
                continue;
            }
            for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                addGameProperty(properties, propertyName);
            }
        }
        return properties;
    }

    public void applyToGameConfig() {
        Config.setBootOverrides(gameProperties());
    }

    private void addGameProperty(Properties properties, String propertyName) {
        addGamePropertyAlias(properties, propertyName);
        if (!propertyName.startsWith(GAME_PROPERTY_PREFIX) || propertyName.length() == GAME_PROPERTY_PREFIX.length()) {
            return;
        }
        String legacyKey = propertyName.substring(GAME_PROPERTY_PREFIX.length());
        if (properties.containsKey(legacyKey)) {
            return;
        }
        String value = environment.getProperty(propertyName);
        if (value != null) {
            properties.setProperty(legacyKey, value);
        }
    }

    private void addGamePropertyAlias(Properties properties, String propertyName) {
        String legacyKey = GAME_PROPERTY_ALIASES.get(propertyName);
        if (legacyKey == null || properties.containsKey(legacyKey)) {
            return;
        }
        String value = environment.getProperty(propertyName);
        if (value != null) {
            properties.setProperty(legacyKey, value);
        }
    }
}
