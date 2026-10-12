package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.persistence.dao.PlayerTransformDao;
import com.aionemu.gameserver.persistence.dao.impl.PlayerTransformDaoImpl;

/**
 * 传送域变身面板同步的静态绑定门面测试。
 * Tests for the teleport-domain transform-panel-sync static binding facade.
 */
class TransformPanelSyncBindingTest {

	@Test
	void bindPlayerTransformDAOIsReachableThroughTeleportServiceFacade() {
		PlayerTransformDao stubDao = new PlayerTransformDaoImpl();

		PlayerTransformDao previous = boundDao();
		try {
			TeleportService2.bindPlayerTransformDAO(stubDao);
			assertSame(stubDao, boundDao());
		} finally {
			restore(previous);
		}
	}

	@Test
	void bindingRejectsNullDao() {
		assertThrows(NullPointerException.class, () -> TeleportService2.bindPlayerTransformDAO(null));
	}

	@Test
	void unboundAccessorFailsFast() throws ReflectiveOperationException {
		PlayerTransformDao previous = boundDao();
		try {
			restore(null);
			java.lang.reflect.Method accessor = TransformPanelSync.class.getDeclaredMethod("playerTransformDAO");
			accessor.setAccessible(true);
			assertThrows(java.lang.reflect.InvocationTargetException.class, () -> accessor.invoke(null));
		} finally {
			restore(previous);
		}
	}

	private static PlayerTransformDao boundDao() {
		try {
			Field field = TransformPanelSync.class.getDeclaredField("boundPlayerTransformDAO");
			field.setAccessible(true);
			return (PlayerTransformDao) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void restore(PlayerTransformDao dao) {
		try {
			Field field = TransformPanelSync.class.getDeclaredField("boundPlayerTransformDAO");
			field.setAccessible(true);
			field.set(null, dao);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
