package com.aionemu.testutil;

import com.aionemu.commons.services.ServiceContext;
import java.lang.reflect.Field;

/**
 * 测试运行器会预置自身作用域；在清理后统一归还上下文，避免测试间相互污染。
 * The IDE runner preinstalls its own scope; unwind contexts after tests so they cannot leak into later tests.
 */
public final class ServiceContextUnwind {

	private ServiceContextUnwind() {
	}

	public static void unwind() {
		try {
			Field field = ServiceContext.class.getDeclaredField("CURRENT");
			field.setAccessible(true);
			ThreadLocal<?> current = (ThreadLocal<?>) field.get(null);
			current.remove();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to unwind test service context", e);
		}
	}
}
