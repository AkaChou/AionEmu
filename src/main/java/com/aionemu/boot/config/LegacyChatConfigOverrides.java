package com.aionemu.boot.config;

import com.aionemu.chatserver.configs.Config;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public class LegacyChatConfigOverrides {

    private final LegacyChatProperties legacyChatProperties;

    public LegacyChatConfigOverrides(LegacyChatProperties legacyChatProperties) {
        this.legacyChatProperties = legacyChatProperties;
    }

    public Properties chatProperties() {
        Properties properties = new Properties();
        legacyChatProperties.getProperty().forEach(properties::setProperty);
        return properties;
    }

    public void applyToChatConfig() {
        Config.setBootOverrides(chatProperties());
    }
}
