package com.aionemu.boot.config;

import com.aionemu.loginserver.configs.Config;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public class LegacyLoginConfigOverrides {

    private final LegacyLoginProperties legacyLoginProperties;

    public LegacyLoginConfigOverrides(LegacyLoginProperties legacyLoginProperties) {
        this.legacyLoginProperties = legacyLoginProperties;
    }

    public Properties loginProperties() {
        Properties properties = new Properties();
        legacyLoginProperties.getProperty().forEach(properties::setProperty);
        return properties;
    }

    public void applyToLoginConfig() {
        Config.setBootOverrides(loginProperties());
    }
}
