package com.aionemu.gameserver.spawnengine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按世界与实例缓存巡逻编队数据的静态缓存。
 * Static cache of walker formations keyed by world and instance.
 *
 * @author Rolandas
 */
class WalkerFormationsCache {

	/**
	 * 世界 ID → 世界级巡逻编队容器。
	 * World id to world-level walker formation holder.
	 */
	private static Map<Integer, WorldWalkerFormations> formations = new ConcurrentHashMap<>();

	/**
	 * 禁止实例化。
	 * Prevents instantiation.
	 */
	private WalkerFormationsCache() {
	}

	/**
	 * 获取指定世界与实例的巡逻编队容器；不存在时自动创建。
	 * Returns the instance walker formations for the world, creating if absent.
	 *
	 * @param worldId 世界 ID / world id
	 * @param instanceId 实例 ID / instance id
	 * @return 实例巡逻编队 / instance walker formations
	 */
	protected static InstanceWalkerFormations getInstanceFormations(int worldId, int instanceId) {
		WorldWalkerFormations wwf = formations.get(worldId);
		if (wwf == null) {
			wwf = new WorldWalkerFormations();
			formations.put(worldId, wwf);
		}
		return wwf.getInstanceFormations(instanceId);
	}

	/**
	 * 实例销毁时清理对应巡逻编队缓存。
	 * Clears walker formation cache when an instance is destroyed.
	 *
	 * @param worldId 世界 ID / world id
	 * @param instanceId 实例 ID / instance id
	 */
	protected static void onInstanceDestroy(int worldId, int instanceId) {
		getInstanceFormations(worldId, instanceId).onInstanceDestroy();
	}
}
