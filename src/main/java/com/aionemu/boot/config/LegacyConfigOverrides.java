package com.aionemu.boot.config;

import com.aionemu.gameserver.configs.Config;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class LegacyConfigOverrides {

    private final LegacyGameProperties legacyGameProperties;
    private final AionGameProperties gameProperties;

    public LegacyConfigOverrides(LegacyGameProperties legacyGameProperties, AionGameProperties gameProperties) {
        this.legacyGameProperties = legacyGameProperties;
        this.gameProperties = gameProperties;
    }

    public Properties gameProperties() {
        Properties properties = new Properties();
        legacyGameProperties.getProperty().forEach(properties::setProperty);
        addGamePropertyAlias(properties);
        return properties;
    }

    public void applyToGameConfig() {
        Config.setBootOverrides(gameProperties());
    }

    private void addGamePropertyAlias(Properties properties) {
        String legacyKey = "gameserver.startup.progress.enable";
        if (properties.containsKey(legacyKey)) {
            return;
        }
        Boolean startupProgressEnabled = gameProperties.getStartup().getProgress().getEnabled();
        if (startupProgressEnabled != null) {
            properties.setProperty(legacyKey, startupProgressEnabled.toString());
        }
    }
}
