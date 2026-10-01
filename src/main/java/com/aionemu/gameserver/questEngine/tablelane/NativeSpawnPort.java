package com.aionemu.gameserver.questEngine.tablelane;

import java.util.concurrent.ThreadLocalRandom;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;

/**
 * 原生任务车道的刷怪端口：DD 附加动作 case 5（`Spawn Npcs`，真端执行器 case 5 →
 * `IUserImp::Spawn`，NP `NpcAIOrderFunc.cpp`：每只 `World_CreateNPC`，Relative = 玩家位置
 * 随机可行走点半径内、Absolute = 精确坐标；time = 刷怪单存活参数）的唯一出口。
 * <p>
 * The spawn port of the native quest lane: the single exit for the DD Spawn Npcs extra action
 * (retail executor case 5 → IUserImp::Spawn).
 * <p>
 * 已知偏差（步 f 激活批前闭合）：真端 Relative 用 `World_RandomWalkableSpawnLocation`
 * 校验可行走；本实现取玩家位置半径内均匀随机偏移，不做可行走校验。生产路由集为空 ⇒ 无行为影响。
 */
public interface NativeSpawnPort {

	/**
	 * 刷 count 只 NPC：relative = 玩家周围半径内随机偏移，否则精确坐标；lifeSeconds 到点回收。
	 * Spawns count NPCs around the player (random offset) or at exact coords, despawned after lifeSeconds.
	 */
	void spawn(Player player, int npcId, int count, boolean relative, float x, float y, float z, int headingDegrees,
			int lifeSeconds);

	/** 生产实现（SpawnEngine + 定时回收）。 / The live implementation (SpawnEngine + scheduled despawn). */
	static NativeSpawnPort live() {
		return Live.INSTANCE;
	}

	/** 生产实现持有者。 / Live implementation holder. */
	final class Live implements NativeSpawnPort {

		private static final NativeSpawnPort INSTANCE = new Live();

		private Live() {
		}

		@Override
		public void spawn(Player player, int npcId, int count, boolean relative, float x, float y, float z,
				int headingDegrees, int lifeSeconds) {
			if (count <= 0) {
				return;
			}
			byte heading = (byte) Math.floorDiv(headingDegrees, 3);
			float baseX = relative ? player.getX() : x;
			float baseY = relative ? player.getY() : y;
			float baseZ = relative ? player.getZ() : z;
			int worldId = player.getWorldId();
			int instanceId = player.getInstanceId();
			for (int index = 0; index < count; index++) {
				float spawnX = baseX;
				float spawnY = baseY;
				if (relative) {
					double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
					double radius = Math.sqrt(ThreadLocalRandom.current().nextDouble()) * 5.0;
					spawnX += (float) (Math.cos(angle) * radius);
					spawnY += (float) (Math.sin(angle) * radius);
				}
				VisibleObject object = SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, spawnX, spawnY, baseZ, heading),
					instanceId);
				if (object != null && lifeSeconds > 0) {
					ThreadPoolManager.getInstance().schedule(() -> {
						if (object.isSpawned()) {
							object.getController().delete();
						}
					}, lifeSeconds * 1000L);
				}
			}
		}
	}
}
