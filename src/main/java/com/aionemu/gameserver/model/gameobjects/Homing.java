package com.aionemu.gameserver.model.gameobjects;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.HomingGameStats;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * 追踪弹游戏对象。
 * Homing game object.
 */

@Getter
@Setter
public class Homing extends SummonedObject<Creature> {
	/** 设置攻击数量 / Sets the attack count. */
	private int attackCount;
	/** 返回技能 ID / Returns the skill id */
	private final int skillId;
	/** 返回当前技能 ID / Returns the active skill id */
	private int activeSkillId;
	private int homingId;

	public Homing(int objId, NpcController controller, SpawnTemplate spawnTemplate, NpcTemplate objectTemplate,
			byte level, int skillId) {
		super(objId, controller, spawnTemplate, objectTemplate, level);
		this.skillId = skillId;
	}

	@Override
	protected void setupStatContainers(byte level) {
		setGameStats(new HomingGameStats(this));
		setLifeStats(new NpcLifeStats(this));
	}

	/** 是否敌对。 / Whether Enemy. */
	@Override
	public boolean isEnemy(Creature creature) {
		return getCreator().isEnemy(creature);
	}

	/**
	 * 判断玩家是否为敌对目标。
	 * Whether the player is an enemy.
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
		return NpcObjectType.HOMING;
	}

	/** 返回主人名称 / Returns the master name */
	@Override
	public String getMasterName() {
		return StringUtils.EMPTY;
	}

	/** 返回攻击类型 / Returns the attack type. */
	@Override
	public ItemAttackType getAttackType() {
		if ((getName().contains("wind")) || (getName().contains("cyclone"))) {
			return ItemAttackType.MAGICAL_WIND;
		}
		return ItemAttackType.PHYSICAL;
	}
}
