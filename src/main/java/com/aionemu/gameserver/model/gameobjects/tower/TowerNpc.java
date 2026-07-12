package com.aionemu.gameserver.model.gameobjects.tower;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.towerofeternityspawns.TowerOfEternitySpawnTemplate;

/**
 * 高塔 NPC 游戏对象。
 * Tower Npc game object.
 */

public class TowerNpc extends Npc {
	private int towerId;

	public TowerNpc(int objId, NpcController controller, TowerOfEternitySpawnTemplate spawnTemplate,
			NpcTemplate objectTemplate) {
		super(objId, controller, spawnTemplate, objectTemplate);
		this.towerId = spawnTemplate.getId();
	}

	/** 返回 eternity tower id / Returns the eternity tower id */
	public int getEternityTowerId() {
		return towerId;
	}

	/** 获取刷新点。 / Returns the spawn. */
	@Override
	public TowerOfEternitySpawnTemplate getSpawn() {
		return (TowerOfEternitySpawnTemplate) super.getSpawn();
	}

	/**
	 * @param creature 是否为敌对目标。 / Whether enemy from
	  */
	@Override
	public boolean isEnemyFrom(Creature creature) {
		if (creature instanceof TowerNpc) {
			return true;
		} else {
			return super.isEnemyFrom(creature);
		}
	}
}
