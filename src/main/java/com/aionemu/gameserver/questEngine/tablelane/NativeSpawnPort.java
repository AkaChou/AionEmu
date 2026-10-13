package com.aionemu.gameserver.questEngine.tablelane;

import java.util.concurrent.ThreadLocalRandom;

import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.geo.GeoService;

/**
 * 原生任务车道的刷怪端口：DD 附加动作 case 5（`Spawn Npcs`，原版执行器 case 5 →
 * `IUserImp::Spawn`，NP `NpcAIOrderFunc.cpp` @1401877b0）的唯一出口。
 * <p>
 * 原版语义（步 f 逐字坐实）：count 只逐只生成；Relative（半径参数 ≥1）= 每只在中心 ±半径的
 * **正方形**内均匀采样，最多 0x20 次，逐次查可行走格子 + 世界 AABB + 高差容差 4 + 路径可达
 * （`World_RandomWalkableSpawnLocation`），全败回退中心坐标；Absolute = 坐标原样不校正；
 * time = 刷怪单存活参数（到期回收者原版侧不可见 = 登记偏差）。
 * <p>
 * The spawn port of the native quest lane: the single exit for the DD Spawn Npcs extra action
 * (retail executor case 5 → IUserImp::Spawn). Retail semantics (step-f evidence): Relative spawns
 * sample a uniform square of ±radius per NPC (up to 32 tries) through the walkable-grid + AABB +
 * z-tolerance-4 + path check of World_RandomWalkableSpawnLocation and fall back to the center;
 * Absolute coords are used verbatim; the retail despawner of the time parameter stays un-adjudicated
 * (registered deviation).
 */
public interface NativeSpawnPort {

	/** 随机采样半径（原版执行器 case 5 Relative 形的半径参数）。 / The Relative sampling radius. */
	float RELATIVE_RADIUS = 5.0f;
	/** 高差容差（原版 z 容差参数 = 4）。 / The retail z-tolerance argument. */
	float Z_TOLERANCE = 4.0f;
	/** 采样重试上限（原版 0x20）。 / The retail retry cap. */
	int SAMPLE_TRIES = 0x20;

	/**
	 * 刷 count 只 NPC：relative = 玩家周围半径内可行走采样，否则精确坐标；lifeSeconds 到点回收。
	 * Spawns count NPCs around the player (walkable sampling) or at exact coords, despawned after
	 * lifeSeconds.
	 */
	void spawn(Player player, int npcId, int count, boolean relative, float x, float y, float z, int headingDegrees,
			int lifeSeconds);

	/** 生产实现（SpawnEngine + 地理采样 + 定时回收）。 / Live: SpawnEngine + geo sampling + scheduled despawn. */
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
			GeoMap geo = GeoService.getInstance().getGeoMap(worldId);
			for (int index = 0; index < count; index++) {
				float spawnX = baseX;
				float spawnY = baseY;
				float spawnZ = baseZ;
				if (relative) {
					float[] point = sampleWalkable(geo, instanceId, baseX, baseY, baseZ);
					spawnX = point[0];
					spawnY = point[1];
					spawnZ = point[2];
				}
				VisibleObject object = SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, spawnX, spawnY, spawnZ, heading),
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

		/**
		 * 原版 `World_RandomWalkableSpawnLocation` 镜像：中心 ±半径正方形均匀采样 ×32，逐次在
		 * 中心 ±100 高度窗内投射地表（NaN = 不可站立）+ 高差容差 + 步行路径可达；全败回退中心
		 * （原版同款回退）。地理地图占位（未加载）时也回退中心。
		 * Mirror of World_RandomWalkableSpawnLocation: uniform square sampling around the center,
		 * ground cast within ±100 of the center z (NaN = not standable) + z-tolerance + walker-path
		 * check per try, center fallback on exhaustion or placeholder geo.
		 */
		private static float[] sampleWalkable(GeoMap geo, int instanceId, float centerX, float centerY,
				float centerZ) {
			if (geo == null || !geo.hasTerrain()) {
				return new float[] {centerX, centerY, centerZ};
			}
			ThreadLocalRandom random = ThreadLocalRandom.current();
			for (int attempt = 0; attempt < SAMPLE_TRIES; attempt++) {
				float x = (float) random.nextDouble(centerX - RELATIVE_RADIUS, centerX + RELATIVE_RADIUS);
				float y = (float) random.nextDouble(centerY - RELATIVE_RADIUS, centerY + RELATIVE_RADIUS);
				float z = geo.getZ(x, y, centerZ + 2, centerZ - 100, instanceId);
				if (Float.isNaN(z) || Math.abs(z - centerZ) >= Z_TOLERANCE) {
					continue;
				}
				if (geo.canPassWalker(centerX, centerY, centerZ, x, y, z, RELATIVE_RADIUS, instanceId)) {
					return new float[] {x, y, z};
				}
			}
			return new float[] {centerX, centerY, centerZ};
		}
	}
}
