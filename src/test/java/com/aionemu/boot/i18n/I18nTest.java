package com.aionemu.boot.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class I18nTest {

    private StaticMessageSource messageSource;

    @BeforeEach
    void setUp() {
        messageSource = new StaticMessageSource();
        messageSource.addMessage("console.startup.successful", Locale.ENGLISH, "Startup successful");
        messageSource.addMessage("console.startup.successful", Locale.SIMPLIFIED_CHINESE, "启动成功");
        messageSource.addMessage("test.port", Locale.SIMPLIFIED_CHINESE, "端口：{0}");
        messageSource.addMessage(
            "console.memory.status",
            Locale.ENGLISH,
            "Memory: allocated={0} MB free={1} MB used={2} MB max={3} MB"
        );
        messageSource.addMessage(
            "console.memory.status",
            Locale.SIMPLIFIED_CHINESE,
            "内存: 已分配={0} MB 空闲={1} MB 已用={2} MB 最大={3} MB"
        );
        I18n.setMessageSource(messageSource);
        I18n.applyCountryCode(1);
    }

    @AfterEach
    void tearDown() {
        I18n.setMessageSource(null);
        I18n.applyCountryCode(1);
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void englishByDefault() {
        assertEquals("Startup successful", I18n.get("console.startup.successful"));
    }

    @Test
    void chineseWhenCountryCodeIs5() {
        I18n.applyCountryCode(5);
        assertEquals("启动成功", I18n.get("console.startup.successful"));
        assertEquals(
            "内存: 已分配=1 MB 空闲=2 MB 已用=3 MB 最大=4 MB",
            I18n.get("console.memory.status", 1L, 2L, 3L, 4L)
        );
    }

    @Test
    void numbersAreNotLocaleGroupedInLogs() {
        I18n.applyCountryCode(5);
        assertEquals("端口：9014", I18n.get("test.port", 9014));
    }

    @Test
    void startupLocaleUsesLegacyCountryCode() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of("gameserver.country.code", "5")));
        new I18nConfig(messageSource, environment).wireI18n();
        assertEquals(Locale.SIMPLIFIED_CHINESE, I18n.currentLocale());
    }

    @Test
    void autoCountryCode99UsesSystemLocale() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of("gameserver.country.code", "99")));
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
            new I18nConfig(messageSource, environment).wireI18n();
            assertEquals(Locale.SIMPLIFIED_CHINESE, I18n.currentLocale());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void englishWhenCountryCodeIsNot5() {
        I18n.applyCountryCode(1);
        assertEquals("Startup successful", I18n.get("console.startup.successful"));
        assertEquals(
            "Memory: allocated=1 MB free=2 MB used=3 MB max=4 MB",
            I18n.get("console.memory.status", 1L, 2L, 3L, 4L)
        );
    }

    @Test
    void missingKeyReturnsCode() {
        assertEquals("missing.key", I18n.get("missing.key"));
    }
}
