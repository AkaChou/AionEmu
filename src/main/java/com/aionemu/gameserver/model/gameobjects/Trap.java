package com.aionemu.gameserver.model.gameobjects;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.model.stats.container.TrapGameStats;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 陷阱游戏对象。
 * Trap game object.
 *
 * @author ATracer
 */
public class Trap extends SummonedObject<Creature> {

	/**
	 * 构造陷阱。
	 * Constructs a trap.
	 *
	 * @param objId 对象 ID / object id
	 * @param controller NPC 控制器 / NPC controller
	 * @param spawnTemplate 生成模板 / spawn template
	 * @param objectTemplate NPC 模板 / NPC template
	 */
	public Trap(int objId, NpcController controller, SpawnTemplate spawnTemplate, NpcTemplate objectTemplate) {
		super(objId, controller, spawnTemplate, objectTemplate, objectTemplate.getLevel());
	}

	@Override
	protected void setupStatContainers(byte level) {
		setGameStats(new TrapGameStats(this));
		setLifeStats(new NpcLifeStats(this));
	}

	/** 获取等级。 / Returns the level. */
	@Override
	public byte getLevel() {
		return (getCreator() == null ? 1 : getCreator().getLevel());
	}

	/** 是否敌对。 / Whether enemy. */
	@Override
	public boolean isEnemy(Creature creature) {
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
		return getCreator() != null ? getCreator().isEnemyFrom(player) : false;
	}

	/**
	 * 返回 NPC 对象类型 TRAP。
	 * Returns NpcObjectType.TRAP.
	 *
	 * @return NPC 对象类型 / NpcObjectType.TRAP
	 */
	@Override
	public NpcObjectType getNpcObjectType() {
		return NpcObjectType.TRAP;
	}

	/** 返回主人名称 / Returns the master name */
	@Override
	public String getMasterName() {
		return StringUtils.EMPTY;
	}
}
