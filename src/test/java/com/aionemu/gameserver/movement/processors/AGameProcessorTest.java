package com.aionemu.gameserver.movement.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.movement.processors.movement.MovementProcessor;

/**
 * {@link AGameProcessor} 告警阈值构造契约。
 * Warning-threshold construction contract for {@link AGameProcessor}.
 */
class AGameProcessorTest {

	/**
	 * 显式阈值必须被采用，且不再依赖静态门面。
	 * An explicit threshold must be adopted instead of reading the static facade.
	 */
	@Test
	void explicitRuntimeThresholdIsUsed() throws Exception {
		MovementProcessor processor = new MovementProcessor(4321L);
		try {
			assertEquals(4321L, thresholdOf(processor));
		} finally {
			shutdown(processor);
		}
	}

	/**
	 * 无参构造器仍沿用遗留静态门面的阈值（非 Bean 回退路径兼容）。
	 * The no-arg constructor still uses the legacy facade threshold for the non-bean fallback path.
	 */
	@Test
	void legacyConstructorUsesTheStaticFacadeThreshold() throws Exception {
		long facadeThreshold = com.aionemu.gameserver.configs.main.ThreadConfig
			.MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING;

		MovementProcessor processor = new MovementProcessor();
		try {
			assertEquals(facadeThreshold, thresholdOf(processor));
		} finally {
			shutdown(processor);
		}
	}

	private static long thresholdOf(MovementProcessor processor) throws Exception {
		Field field = AGameProcessor.class.getDeclaredField("maxRuntimeInMillisWithoutWarning");
		field.setAccessible(true);
		return (long) field.get(processor);
	}

	private static void shutdown(MovementProcessor processor) throws Exception {
		Field poolField = AGameProcessor.class.getDeclaredField("_processorPool");
		poolField.setAccessible(true);
		((java.util.concurrent.ScheduledThreadPoolExecutor) poolField.get(processor)).shutdownNow();
	}
}
