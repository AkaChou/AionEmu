package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 队伍之门游戏对象。
 * Group Gate game object.
 *
 * @author LokiReborn
 */
public class GroupGate extends SummonedObject<Creature> {

	/**
	 * 构造队伍之门。
	 * Constructs a group gate.
	 *
	 * @param objId 对象 ID / object id
	 * @param controller NPC 控制器 / NPC controller
	 * @param spawnTemplate 生成模板 / spawn template
	 * @param objectTemplate NPC 模板 / NPC template
	 */
	public GroupGate(int objId, NpcController controller, SpawnTemplate spawnTemplate, NpcTemplate objectTemplate) {
		super(objId, controller, spawnTemplate, objectTemplate, (byte) 1);
	}

	/** 是否敌对。 / Whether Enemy. */
	@Override
	public boolean isEnemy(Creature creature) {
		return getCreator().isEnemy(creature);
	}

	/**
	 * 返回 NPC 对象类型 GROUPGATE。
	 * Returns NpcObjectType.GROUPGATE.
	 *
	 * @return NPC 对象类型 / NpcObjectType.GROUPGATE
	 */
	@Override
	public NpcObjectType getNpcObjectType() {
		return NpcObjectType.GROUPGATE;
	}
}
