package com.aionemu.boot.config;

import com.aionemu.gameserver.configs.Config;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class LegacyConfigOverrides {

    private static final String LOGIN_ADDRESS_KEY = "gameserver.network.login.address";
    private static final String IPCONFIG_DEFAULT_KEY = "gameserver.network.ipconfig.default";

    private final LegacyGameProperties legacyGameProperties;
    private final AionGameProperties gameProperties;

    public LegacyConfigOverrides(LegacyGameProperties legacyGameProperties, AionGameProperties gameProperties) {
        this.legacyGameProperties = legacyGameProperties;
        this.gameProperties = gameProperties;
    }

    public Properties gameProperties() {
        Properties properties = new Properties();
        legacyGameProperties.getProperty().forEach(properties::setProperty);
        addNetworkAliases(properties);
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

    private void addNetworkAliases(Properties properties) {
        String externalIp = gameProperties.getNetwork().getExternalIp();
        if (externalIp == null || externalIp.isBlank()) {
            return;
        }

        externalIp = externalIp.trim();
        properties.setProperty(IPCONFIG_DEFAULT_KEY, externalIp);
        properties.setProperty(LOGIN_ADDRESS_KEY, replaceLoginAddressHost(
            properties.getProperty(LOGIN_ADDRESS_KEY),
            externalIp
        ));
    }

    private String replaceLoginAddressHost(String loginAddress, String host) {
        if (loginAddress == null || loginAddress.isBlank()) {
            return host + ":9014";
        }
        int portSeparator = loginAddress.lastIndexOf(':');
        if (portSeparator < 0 || portSeparator == loginAddress.length() - 1) {
            return host;
        }
        return host + loginAddress.substring(portSeparator);
    }
}
