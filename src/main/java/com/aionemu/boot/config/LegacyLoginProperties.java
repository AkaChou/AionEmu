package com.aionemu.boot.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 {@code aion.legacy.login} 前缀的遗留登录键值属性。
 * Legacy login key/value properties bound under {@code aion.legacy.login}.
 */
@Getter
@ConfigurationProperties(prefix = "aion.legacy.login")
public class LegacyLoginProperties {

    private final Map<String, String> property = new LinkedHashMap<>();
}
