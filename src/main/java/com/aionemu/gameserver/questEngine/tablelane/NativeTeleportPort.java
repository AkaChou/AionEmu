package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * 原生任务车道的传送端口：DD 附加动作 case 3（`Teleport To`，真端执行器 case 3 →
 * `IUserImp::Teleport(world, x, y, z+1, heading, 1)`，NP `NpcAIOrderFunc.cpp`）的唯一出口。
 * 真端 z 抬 1.0 落地、heading = 度（宿主内部转 6 位朝向）；本端口同语义转换。
 * <p>
 * The teleport port of the native quest lane: the single exit for the DD Teleport extra action
 * (retail executor case 3 → IUserImp::Teleport with z+1 and degree heading).
 */
public interface NativeTeleportPort {

	/** 传送到世界坐标（z 抬 1.0；heading 为度）。 / Teleports to world coords (z+1; heading in degrees). */
	void teleport(Player player, int worldId, float x, float y, float z, int headingDegrees);

	/** 生产实现（TeleportService2）。 / The live implementation (TeleportService2). */
	static NativeTeleportPort live() {
		return Live.INSTANCE;
	}

	/** 生产实现持有者。 / Live implementation holder. */
	final class Live implements NativeTeleportPort {

		private static final NativeTeleportPort INSTANCE = new Live();

		private Live() {
		}

		@Override
		public void teleport(Player player, int worldId, float x, float y, float z, int headingDegrees) {
			TeleportService2.teleportTo(player, worldId, x, y, z + 1.0f, (byte) Math.floorDiv(headingDegrees, 3));
		}
	}
}
