package com.aionemu.boot.i18n;

import java.util.Locale;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 面向非注入调用点的 Spring {@link MessageSource} 静态门面。
 * Static facade over Spring {@link MessageSource} for non-injected call sites.
 * <p>
 * 语言规则：国家码 5 → zh_CN，否则 en。
 * Locale rule: country code 5 → zh_CN, otherwise en.
 */
@UtilityClass
public class I18n {

    private final Locale ZH_CN = Locale.SIMPLIFIED_CHINESE;
    private final Locale EN = Locale.ENGLISH;

    /**
     * 底层消息源。
     * Underlying message source.
     */
    @Setter
    private volatile MessageSource messageSource;
    private volatile Locale locale = EN;

    /**
     * 按遗留国家码切换当前语言。
     * Switch current locale by legacy country code.
     *
     * @param countryCode 国家码，5 表示简体中文 / Country code, 5 means Simplified Chinese
     */
    public void applyCountryCode(int countryCode) {
        locale = countryCode == 5 ? ZH_CN : EN;
        LocaleContextHolder.setDefaultLocale(locale);
        LocaleContextHolder.setLocale(locale, true);
    }

    /**
     * 返回当前语言环境（{@link LocaleContextHolder} 保证非 null）。
     * Return current locale ({@link LocaleContextHolder} never returns null).
     *
     * @return 当前语言环境 / Current locale
     */
    public Locale currentLocale() {
        return locale;
    }

    /**
     * 按消息键取值（无参数）。
     * Resolve message by code without arguments.
     *
     * Message code
     *
     * @param code @return 本地化文本；缺失时返回键本身 / Localized text, or the code if missing
     */
    public String get(String code) {
        return get(code, (Object[]) null);
    }

    /**
     * 按消息键与参数取值。数字参数会先转成原始字符串，避免本地化千分位。
     * Resolve message by code and args. Numbers are stringified first to avoid locale grouping.
     *
     * Message code
     *
     * @param args 消息参数 / Message arguments
     * @param args @return 本地化文本；缺失时返回键本身 / Localized text, or the code if missing
     */
    public String get(String code, Object... args) {
        MessageSource source = messageSource;
        if (source == null) {
            return code;
        }
        try {
            return source.getMessage(code, rawNumbers(args), currentLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    /**
     * 将数字参数转为原始字符串，避免 MessageFormat 按语言插入千分位。
     * Convert numeric args to raw strings so MessageFormat does not insert locale grouping.
     */
    private Object[] rawNumbers(Object[] args) {
        if (args == null) {
            return null;
        }
        Object[] normalized = args.clone();
        for (int i = 0; i < normalized.length; i++) {
            if (normalized[i] instanceof Number number) {
                normalized[i] = number.toString();
            }
        }
        return normalized;
    }
}
