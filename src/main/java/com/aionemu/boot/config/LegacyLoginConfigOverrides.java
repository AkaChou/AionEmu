package com.aionemu.boot.config;

import com.aionemu.loginserver.configs.Config;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 将 Boot 绑定的遗留登录属性应用到登录服 {@link Config}。
 * Applies Boot-bound legacy login properties to the login-server {@link Config}.
 */
@Component
@RequiredArgsConstructor
public class LegacyLoginConfigOverrides {

    private final LegacyLoginProperties legacyLoginProperties;

    /**
     * 构建登录服可消费的 {@link Properties} 覆盖集。
     * Builds the login-server {@link Properties} override set.
     *
     * override properties
     */
    public Properties loginProperties() {
        Properties properties = new Properties();
        legacyLoginProperties.getProperty().forEach(properties::setProperty);
        return properties;
    }

    /**
     * 将覆盖属性写入登录服配置。
     * Writes override properties into the login-server configuration.
     */
    public void applyToLoginConfig() {
        Config.setBootOverrides(loginProperties());
    }
}
