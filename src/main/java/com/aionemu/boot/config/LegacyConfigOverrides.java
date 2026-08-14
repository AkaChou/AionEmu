package com.aionemu.boot.config;

import com.aionemu.gameserver.configs.Config;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 将 Boot 绑定的遗留游戏属性与启动进度别名应用到游戏服 {@link Config}。
 * Applies Boot-bound legacy game properties and startup-progress aliases to the game-server {@link Config}.
 */
@Component
@RequiredArgsConstructor
public class LegacyConfigOverrides {

    private final LegacyGameProperties legacyGameProperties;
    private final AionGameProperties gameProperties;

    /**
     * 构建游戏服可消费的 {@link Properties} 覆盖集（含进度开关别名）。
     * Builds the game-server {@link Properties} override set (including progress-toggle aliases).
     *
     * @return 覆盖属性集 / override properties
     */
    public Properties gameProperties() {
        Properties properties = new Properties();
        legacyGameProperties.getProperty().forEach(properties::setProperty);
        addGamePropertyAlias(properties);
        return properties;
    }

    /**
     * 将覆盖属性写入游戏服配置。
     * Writes override properties into the game-server configuration.
     */
    public void applyToGameConfig() {
        Config.setBootOverrides(gameProperties());
    }

    /**
     * 在缺少遗留键时，用 {@code aion.game.startup.progress.enabled} 补齐进度开关别名。
     * Fills the startup-progress legacy key from {@code aion.game.startup.progress.enabled} when missing.
     *
     * @param properties 待补齐的属性集 / properties being enriched
     */
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
