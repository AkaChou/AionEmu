package com.aionemu.boot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AionGameStartupProgressProperty {

    // Keeps the dynamic legacy override key visible to Spring tooling and IDE inspections.
    AionGameStartupProgressProperty(@Value("${aion.game.startup.progress.enabled:true}") boolean startupProgressEnabled) {
    }
}
