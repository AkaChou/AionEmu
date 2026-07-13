package com.aionemu.boot.i18n;

import com.aionemu.gameserver.configs.main.GSConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 启动时按 {@code gameserver.country.code} 绑定默认语言，并注入 {@link I18n}。
 * Bind default locale from {@code gameserver.country.code} at startup and wire {@link I18n}.
 */
@Configuration
@RequiredArgsConstructor
public class I18nConfig {

    private final MessageSource messageSource;
    private final Environment environment;

    /**
     * 初始化默认语言与静态门面。
     * Initialize default locale and the static facade.
     */
    @PostConstruct
    void wireI18n() {
        I18n.setMessageSource(messageSource);
        int countryCode = environment.getProperty("gameserver.country.code", Integer.class, 99);
        I18n.applyCountryCode(GSConfig.resolveCountryCode(countryCode));
    }
}
