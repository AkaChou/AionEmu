package com.aionemu.boot.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 {@code aion.legacy.game} 前缀的遗留游戏键值属性。
 * Legacy game key/value properties bound under {@code aion.legacy.game}.
 */
@Getter
@ConfigurationProperties(prefix = "aion.legacy.game")
public class LegacyGameProperties {

    private final Map<String, String> property = new LinkedHashMap<>();
}
