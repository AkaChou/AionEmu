package com.aionemu.gameserver.model.gameobjects.base;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.basespawns.BaseSpawnTemplate;

/**
 * 基础 NPC 游戏对象。
 * Base Npc game object.
 *
 * @author Ranastic
 */

public class BaseNpc extends Npc {
	private int baseId;

	public BaseNpc(int objId, NpcController controller, BaseSpawnTemplate spawnTemplate, NpcTemplate objectTemplate) {
		super(objId, controller, spawnTemplate, objectTemplate);
		this.baseId = spawnTemplate.getId();
	}

	/** 返回 base id / Returns the base id */
	public int getBaseId() {
		return baseId;
	}

	/** 获取刷新点。 / Returns the spawn. */
	@Override
	public BaseSpawnTemplate getSpawn() {
		return (BaseSpawnTemplate) super.getSpawn();
	}

	/**
	 * @param creature 要检查的目标生物 / creature to check
	 * @return 是否视其为敌对目标 / Whether the creature is considered an enemy
	 */
	@Override
	public boolean isEnemyFrom(Creature creature) {
		if (creature instanceof BaseNpc) {
			return true;
		} else {
			return super.isEnemyFrom(creature);
		}
	}
}
