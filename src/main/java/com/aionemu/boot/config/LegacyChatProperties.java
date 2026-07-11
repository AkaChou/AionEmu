package com.aionemu.boot.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 {@code aion.legacy.chat} 前缀的遗留聊天键值属性。
 * Legacy chat key/value properties bound under {@code aion.legacy.chat}.
 */
@Getter
@ConfigurationProperties(prefix = "aion.legacy.chat")
public class LegacyChatProperties {

    private final Map<String, String> property = new LinkedHashMap<>();
}
