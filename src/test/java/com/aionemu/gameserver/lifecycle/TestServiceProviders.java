package com.aionemu.gameserver.lifecycle;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * 测试用 provider 工具：为已退役双源类安装/清理 Spring provider。
 * Test-scope provider helper: installs and clears the Spring provider for retired dual-source classes.
 */
public final class TestServiceProviders {

	private TestServiceProviders() {
	}

	/**
	 * 为指定类型安装返回给定实例的 provider。
	 * Installs a provider that returns the given instance for the type.
	 * @param type     被测服务类型 / service type
	 * @param instance 测试实例 / test instance
	 * @param <T>      服务类型 / service type
	 */
	public static <T> void install(Class<T> type, T instance) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(type.getName(), instance);
		apply(type, beanFactory.getBeanProvider(type));
	}

	/**
	 * 清空指定类型的 provider 与已解析缓存。
	 * Clears the provider and the resolved cache of the given type.
	 * @param type 服务类型 / service type
	 */
	public static void clear(Class<?> type) {
		apply(type, null);
		resetResolvedInstance(type);
	}

	/**
	 * 直接清空静态已解析缓存，避免 provider 之外的第二层缓存跨测试泄漏。
	 * Clears the static resolved cache directly so the second-level cache does not leak across tests.
	 * @param type 服务类型 / service type
	 */
	private static void resetResolvedInstance(Class<?> type) {
		try {
			var field = type.getDeclaredField("resolvedInstance");
			field.setAccessible(true);
			field.set(null, null);
		} catch (NoSuchFieldException e) {
			// 该类没有已解析缓存，无需处理。
			// No resolved cache on this type; nothing to clear.
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("无法清空实例缓存 / cannot clear resolved instance: " + type.getName(), e);
		}
	}

	private static void apply(Class<?> type, ObjectProvider<?> provider) {
		try {
			type.getMethod("setInstanceProvider", ObjectProvider.class).invoke(null, provider);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("无法设置 provider / cannot install provider: " + type.getName(), e);
		}
	}
}
