package com.aionemu.boot.config;

import com.aionemu.gameserver.configs.Config;
import java.util.Properties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

@Component
public class LegacyConfigOverrides {

    static final String GAME_PROPERTY_PREFIX = "aion.legacy.game.property.";

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
}
