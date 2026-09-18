package com.aionemu.commons.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@link ConfigurableProcessor} 的来源优先级契约。
 * Source-precedence contract for {@link ConfigurableProcessor}.
 */
class ConfigurableProcessorSourceResolverTest {

	/**
	 * 测试用配置载体。
	 * Configuration holder used by this test.
	 */
	static class SampleConfig {
		@Property(key = "sample.value", defaultValue = "default")
		static String VALUE;
	}

	@AfterEach
	void clearResolver() {
		ConfigSourceResolverHolder.publish(null);
		SampleConfig.VALUE = null;
	}

	/**
	 * 发布解析器时以解析器值为准；未发布时保持原有 Properties 行为。
	 * The published resolver wins while it is active; without one the original Properties behavior is unchanged.
	 */
	@Test
	void resolverTakesPrecedenceOverPropertiesAndDefaults() {
		Properties fileProperties = new Properties();
		fileProperties.setProperty("sample.value", "from-file");

		ConfigurableProcessor.process(SampleConfig.class, fileProperties);
		assertEquals("from-file", SampleConfig.VALUE);

		ConfigSourceResolverHolder.publish(key -> "sample.value".equals(key) ? "from-resolver" : null);
		ConfigurableProcessor.process(SampleConfig.class, fileProperties);
		assertEquals("from-resolver", SampleConfig.VALUE);

		ConfigSourceResolverHolder.publish(null);
		ConfigurableProcessor.process(SampleConfig.class, new Properties());
		assertEquals("default", SampleConfig.VALUE);
	}

	/**
	 * 解析器对未知键返回 null 时不得吞掉文件值或默认值。
	 * A resolver returning null for an unknown key must not swallow the file value or the default.
	 */
	@Test
	void nullFromResolverFallsBackToProperties() {
		ConfigSourceResolverHolder.publish(key -> null);

		Properties fileProperties = new Properties();
		fileProperties.setProperty("sample.value", "from-file");
		ConfigurableProcessor.process(SampleConfig.class, fileProperties);
		assertEquals("from-file", SampleConfig.VALUE);

		ConfigurableProcessor.process(SampleConfig.class, new Properties());
		assertEquals("default", SampleConfig.VALUE);
		assertNull(ConfigSourceResolverHolder.resolve("sample.unknown"));
	}
}
