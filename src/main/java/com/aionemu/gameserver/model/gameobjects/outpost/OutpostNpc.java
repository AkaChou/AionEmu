package com.aionemu.gameserver.model.gameobjects.outpost;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.outpostspawns.OutpostSpawnTemplate;
import lombok.Getter;

/**
 * 前哨 NPC 游戏对象。
 * Outpost Npc game object.
 */

@Getter
public class OutpostNpc extends Npc {
	/** 返回 outpost id / Returns the outpost id */
	private final int outpostId;

	public OutpostNpc(int objId, NpcController controller, OutpostSpawnTemplate spawnTemplate,
			NpcTemplate objectTemplate) {
		super(objId, controller, spawnTemplate, objectTemplate);
		this.outpostId = spawnTemplate.getId();
	}

	/** 获取刷新点。 / Returns the spawn. */
	@Override
	public OutpostSpawnTemplate getSpawn() {
		return (OutpostSpawnTemplate) super.getSpawn();
	}

	/**
	 * @param creature 待判断的生物 / creature to check
	 * @return 是否为敌对目标 / Whether enemy from
	 */
	@Override
	public boolean isEnemyFrom(Creature creature) {
		if (creature instanceof OutpostNpc) {
			return true;
		} else {
			return super.isEnemyFrom(creature);
		}
	}
}
