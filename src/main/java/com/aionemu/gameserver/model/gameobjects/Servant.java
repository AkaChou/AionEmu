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
	 * 构造仆从。
	 * Constructs a servant.
	 *
	 * @param objId 对象 ID / object id
	 * @param controller NPC 控制器 / NPC controller
	 * @param spawnTemplate 生成模板 / spawn template
	 * @param objectTemplate NPC 模板 / NPC template
	 * @param level 等级 / level
	 */
	public Servant(int objId, NpcController controller, SpawnTemplate spawnTemplate, NpcTemplate objectTemplate,
			byte level) {
		super(objId, controller, spawnTemplate, objectTemplate, level);
	}

	/** 是否敌对。 / Whether enemy. */
	@Override
	public final boolean isEnemy(Creature creature) {
		return getCreator().isEnemy(creature);
	}

	/**
	 * 判断玩家是否为敌对目标。
	 * Whether the player is an enemy.
	 *
	 * @param player 玩家 / player
	 * @return 是否敌对 / whether enemy
	  */
	@Override
	public boolean isEnemyFrom(Player player) {
		return getCreator() != null && getCreator().isEnemyFrom(player);
	}

	/** 返回 NPC 对象类型 / Returns the npc object type */
	@Override
	public NpcObjectType getNpcObjectType() {
		return objectType;
	}

	/** 设置 NPC 对象类型 / Sets the npc object type */
	public void setNpcObjectType(NpcObjectType objectType) {
		this.objectType = objectType;
	}

	/** 返回主人名称 / Returns the master name */
	@Override
	public String getMasterName() {
		return StringUtils.EMPTY;
	}
}
