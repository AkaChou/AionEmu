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
	 *
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
	 * 清空指定类型的 provider。
	 * Clears the provider of the given type.
	 *
	 * @param type 服务类型 / service type
	 */
	public static void clear(Class<?> type) {
		apply(type, null);
	}

	private static void apply(Class<?> type, ObjectProvider<?> provider) {
		try {
			type.getMethod("setInstanceProvider", ObjectProvider.class).invoke(null, provider);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("无法设置 provider / cannot install provider: " + type.getName(), e);
		}
	}
}
