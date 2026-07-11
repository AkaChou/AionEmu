package com.aionemu.gameserver.model.gameobjects;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.HomingGameStats;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 追踪弹游戏对象。
 * Homing game object.
 */

public class Homing extends SummonedObject<Creature> {
	private int attackCount;
	private int skillId;
	private int activeSkillId;

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

	/** 设置攻击数量 / Sets the attack count*/
	public void setAttackCount(int attackCount) {
		this.attackCount = attackCount;
	}

	/** 返回攻击数量 / Returns the attack count*/
	public int getAttackCount() {
		return attackCount;
	}

	/** 是否敌对。 / Whether Enemy. */
	@Override
	public boolean isEnemy(Creature creature) {
		return getCreator().isEnemy(creature);
	}

	/**
	 * @param player 是否 enemy 从 / 是否 enemy 从。 / Whether enemy from / Whether enemy from
	 */
	@Override
	public boolean isEnemyFrom(Player player) {
		return getCreator() != null ? getCreator().isEnemyFrom(player) : false;
	}

	/** 返回 npc object type / Returns the npc object type */
	@Override
	public NpcObjectType getNpcObjectType() {
		return NpcObjectType.HOMING;
	}

	/** 返回大师名称 / Returns the master name */
	@Override
	public String getMasterName() {
		return StringUtils.EMPTY;
	}

	/** 返回攻击类型 / Returns the attack type*/
	@Override
	public ItemAttackType getAttackType() {
		if ((getName().contains("wind")) || (getName().contains("cyclone"))) {
			return ItemAttackType.MAGICAL_WIND;
		}
		return ItemAttackType.PHYSICAL;
	}

	/** 返回技能 ID / Returns the skill id */
	public int getSkillId() {
		return skillId;
	}

	/** 返回当前技能 ID / Returns the active skill id */
	public int getActiveSkillId() {
		return activeSkillId;
	}

	/** 设置当前技能 ID / Sets the active skill id */
	public void setActiveSkillId(int activeSkillId) {
		this.activeSkillId = activeSkillId;
	}
}
