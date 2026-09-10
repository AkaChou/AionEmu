package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 验证玩家尚未生成时刷新附近任务不会访问不存在的地图区域。
 * Verifies that nearby-quest refresh skips safely when a player has not spawned into a map region yet.
 */
class PlayerControllerNearbyQuestTest {

	/**
	 * 登录阶段任务提交可能早于玩家生成；此时刷新应安全跳过，等待后续世界同步再次刷新。
	 * Quest commits may run before player spawn during login; refresh should skip safely until the later world sync.
	 */
	@Test
	void skipsRefreshBeforePlayerIsSpawned() throws Exception {
		ObjenesisStd objenesis = new ObjenesisStd();
		Player player = objenesis.newInstance(Player.class);
		setField(VisibleObject.class, player, "position", new WorldPosition(210010000));
		PlayerController controller = new PlayerController();
		controller.setOwner(player);

		assertDoesNotThrow(controller::updateNearbyQuests);
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
