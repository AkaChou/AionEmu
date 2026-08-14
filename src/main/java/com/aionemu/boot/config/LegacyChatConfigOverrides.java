package com.aionemu.boot.config;

import com.aionemu.chatserver.configs.Config;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 将 Boot 绑定的遗留聊天属性应用到聊天服 {@link Config}。
 * Applies Boot-bound legacy chat properties to the chat-server {@link Config}.
 */
@Component
@RequiredArgsConstructor
public class LegacyChatConfigOverrides {

    private final LegacyChatProperties legacyChatProperties;

    /**
     * 构建聊天服可消费的 {@link Properties} 覆盖集。
     * Builds the chat-server {@link Properties} override set.
     *
     * @return 覆盖属性集 / override properties
     */
    public Properties chatProperties() {
        Properties properties = new Properties();
        legacyChatProperties.getProperty().forEach(properties::setProperty);
        return properties;
    }

    /**
     * 将覆盖属性写入聊天服配置。
     * Writes override properties into the chat-server configuration.
     */
    public void applyToChatConfig() {
        Config.setBootOverrides(chatProperties());
    }
}
