package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dao.PlayerTransformDAO;

/**
 * 传送域变身面板同步的静态绑定门面测试。
 * Tests for the teleport-domain transform-panel-sync static binding facade.
 */
class TransformPanelSyncBindingTest {

	@Test
	void bindPlayerTransformDAOIsReachableThroughTeleportServiceFacade() {
		PlayerTransformDAO stubDao = new PlayerTransformDAO() {
			@Override
			public void loadPlTransfo(com.aionemu.gameserver.model.gameobjects.player.Player player) {
			}

			@Override
			public boolean storePlTransfo(int playerId, int panelId, int itemId) {
				return true;
			}

			@Override
			public boolean deletePlTransfo(int playerId) {
				return true;
			}

			@Override
			public boolean supports(String databaseName, int majorVersion, int minorVersion) {
				return true;
			}
		};

		PlayerTransformDAO previous = boundDao();
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
	void unboundAccessorFallsBackWithoutError() throws ReflectiveOperationException {
		PlayerTransformDAO previous = boundDao();
		try {
			restore(null);
			// 触发兜底构建：离线环境应构造 PlayerTransfoDAO 实现（不抛异常）
			java.lang.reflect.Method accessor = TransformPanelSync.class.getDeclaredMethod("playerTransformDAO");
			accessor.setAccessible(true);
			assertNotNull(accessor.invoke(null));
		} finally {
			restore(previous);
		}
	}

	private static PlayerTransformDAO boundDao() {
		try {
			Field field = TransformPanelSync.class.getDeclaredField("boundPlayerTransformDAO");
			field.setAccessible(true);
			return (PlayerTransformDAO) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void restore(PlayerTransformDAO dao) {
		try {
			Field field = TransformPanelSync.class.getDeclaredField("boundPlayerTransformDAO");
			field.setAccessible(true);
			field.set(null, dao);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
