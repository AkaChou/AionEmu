package com.aionemu.gameserver.spawnengine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单个世界下按实例划分的巡逻编队容器。
 * Holds per-instance walker formations for one world.
 *
 * @author Rolandas
 */
public class WorldWalkerFormations {

	/**
	 * 实例 ID → 实例巡逻编队。
	 * Instance id to instance walker formations.
	 */
	private Map<Integer, InstanceWalkerFormations> formations;

	/**
	 * 创建空的世界巡逻编队容器。
	 * Creates an empty world walker formation holder.
	 */
	public WorldWalkerFormations() {
		formations = new ConcurrentHashMap<>();
	}

	/**
	 * 获取指定实例的巡逻编队；不存在时自动创建。
	 * Returns formations for the instance, creating if absent.
	 *
	 * @param instanceId 实例 ID / instance id
	 * @return 实例巡逻编队 / instance walker formations
	 */
	protected InstanceWalkerFormations getInstanceFormations(int instanceId) {
		InstanceWalkerFormations instanceFormation = formations.get(instanceId);
		if (instanceFormation == null) {
			instanceFormation = new InstanceWalkerFormations();
			formations.put(instanceId, instanceFormation);
		}
		return instanceFormation;
	}
}
