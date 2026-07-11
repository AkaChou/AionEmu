package com.aionemu.gameserver.world;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.instance.handlers.InstanceHandler;

/**
 * 世界地图实例工厂：按地图类型创建 2D/3D 实例并绑定副本处理器。
 * Factory for world-map instances: creates 2D/3D instances and binds instance handlers.
 *
 * @author Rinzler (Encom)
 */
public class WorldMapInstanceFactory {

	/**
	 * 创建无主地图实例（ownerId=0）。
	 * Create a non-personal map instance (ownerId=0).
	 *
	 * @param parent 父级世界地图 / parent world map
	 * instance id
	 *
	 * @return 新建的地图实例 / newly created map instance
	 */
	public static WorldMapInstance createWorldMapInstance(WorldMap parent, int instanceId) {
		return createWorldMapInstance(parent, instanceId, 0);
	}

	/**
	 * 创建地图实例；欧比斯/潘德蒙相关图使用 3D 分区，其余为 2D。
	 * Create a map instance; abyss/Panesterra maps use 3D regions, others 2D.
	 *
	 * @param parent 父级世界地图 / parent world map
	 * instance id
	 * @param ownerId 个人实例所有者 ID，0 表示非个人 / personal-instance owner id, 0 if not personal
	 * @return 新建的地图实例 / newly created map instance
	 */
	public static WorldMapInstance createWorldMapInstance(WorldMap parent, int instanceId, int ownerId) {
		WorldMapInstance worldMapInstance = null;
		if (parent.getMapId() == WorldMapType.RESHANTA.getId() && parent.getMapId() == WorldMapType.BELUS.getId()
				&& parent.getMapId() == WorldMapType.ASPIDA.getId()
				&& parent.getMapId() == WorldMapType.ATANATOS.getId()
				&& parent.getMapId() == WorldMapType.DISILLON.getId()) {
			worldMapInstance = new WorldMap3DInstance(parent, instanceId);
		} else {
			worldMapInstance = new WorldMap2DInstance(parent, instanceId, ownerId);
		}
		InstanceHandler instanceHandler = GameEngineServices.instanceEngine().getNewInstanceHandler(parent.getMapId());
		worldMapInstance.setInstanceHandler(instanceHandler);
		return worldMapInstance;
	}
}
