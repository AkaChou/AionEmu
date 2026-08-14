package com.aionemu.gameserver.spawnengine;

import java.util.function.Supplier;

import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

final class SpawnSurfaceResolver {

	private SpawnSurfaceResolver() {
	}

	static float resolve(SpawnTemplate spawn, int instanceId) {
		return resolve(spawn,
			() -> GameWorldServices.geoService().projectGroundZ(spawn.getWorldId(), spawn.getX(), spawn.getY(),
				spawn.getZ(), instanceId),
			() -> GameWorldServices.pathService().projectGroundPoint(spawn.getWorldId(), spawn.getX(), spawn.getY(),
				spawn.getZ()),
			() -> GameWorldServices.geoService().getTerrainZ(spawn.getWorldId(), spawn.getX(), spawn.getY()));
	}

	static float resolve(SpawnTemplate spawn, Supplier<Float> geoProjection, Supplier<float[]> pathProjection,
			Supplier<Float> terrainProjection) {
		if (!spawn.isResolveZ() || spawn.canFly()) {
			return spawn.getZ();
		}
		float cached = spawn.getResolvedZ();
		if (Float.isFinite(cached)) {
			return cached;
		}
		synchronized (spawn) {
			cached = spawn.getResolvedZ();
			if (Float.isFinite(cached)) {
				return cached;
			}
			Float geoZ = geoProjection.get();
			float resolved = geoZ != null && Float.isFinite(geoZ) ? geoZ : Float.NaN;
			if (!Float.isFinite(resolved)) {
				float[] pathPoint = pathProjection.get();
				if (pathPoint != null && pathPoint.length >= 3 && Float.isFinite(pathPoint[2])) {
					resolved = pathPoint[2];
				}
			}
			if (!Float.isFinite(resolved)) {
				Float terrainZ = terrainProjection.get();
				if (terrainZ != null && Float.isFinite(terrainZ)) {
					resolved = terrainZ;
				}
			}
			if (!Float.isFinite(resolved)) {
				resolved = spawn.getZ();
			}
			spawn.setResolvedZ(resolved);
			return resolved;
		}
	}
}
