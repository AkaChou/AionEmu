package com.aionemu.boot.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "aion.legacy.login")
public class LegacyLoginProperties {

    private final Map<String, String> property = new LinkedHashMap<>();
}
