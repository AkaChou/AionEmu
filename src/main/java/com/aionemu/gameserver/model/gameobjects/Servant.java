package com.aionemu.gameserver.model.gameobjects;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 仆从游戏对象。
 * Servant game object.
 *
 * @author ATracer
 */
public class Servant extends SummonedObject<Creature> {
	private NpcObjectType objectType;

	/**
	 * @param objId
	 * @param controller
	 * @param spawnTemplate
	 * @param objectTemplate
	 * @param level
	 */
	public Servant(int objId, NpcController controller, SpawnTemplate spawnTemplate, NpcTemplate objectTemplate,
			byte level) {
		super(objId, controller, spawnTemplate, objectTemplate, level);
	}

	/** 是否敌对。 / Whether Enemy. */
	@Override
	public final boolean isEnemy(Creature creature) {
		return getCreator().isEnemy(creature);
	}

	/**
	 * @param player 是否为敌对目标。 / Whether enemy from
	  */
	@Override
	public boolean isEnemyFrom(Player player) {
		return getCreator() != null && getCreator().isEnemyFrom(player);
	}

	/** 返回 npc object type / Returns the npc object type */
	@Override
	public NpcObjectType getNpcObjectType() {
		return objectType;
	}

	/** SetsNPC 对象类型 / Sets the npc object type */
	public void setNpcObjectType(NpcObjectType objectType) {
		this.objectType = objectType;
	}

	/** 返回大师名称 / Returns the master name */
	@Override
	public String getMasterName() {
		return StringUtils.EMPTY;
	}
}
