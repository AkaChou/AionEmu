package com.aionemu.testutil;

import com.aionemu.boot.i18n.I18n;
import java.lang.reflect.Field;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * 在离线日志测试中加载真实双语消息，关闭时还原静态消息源，不修改语言设置。
 * Loads real bilingual messages for offline log tests, restoring the static source without changing locale settings.
 */
public final class LocalizedMessages implements AutoCloseable {

	private final MessageSource previous;

	/** 绑定真实消息资源。 / Binds the real message resources. */
	public LocalizedMessages() throws ReflectiveOperationException {
		Field field = I18n.class.getDeclaredField("messageSource");
		field.setAccessible(true);
		previous = (MessageSource) field.get(null);
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("messages");
		source.setDefaultEncoding("UTF-8");
		I18n.setMessageSource(source);
	}

	@Override
	public void close() {
		I18n.setMessageSource(previous);
	}
}
