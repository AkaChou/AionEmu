package com.aionemu.gameserver.model.skill;

/**
 * NPC 技能 Parameter 条目，用于技能相关逻辑。
 * Npc Skill Parameter Entry for skill logic.
 */

class NpcSkillParameterEntry extends NpcSkillEntry {

	public NpcSkillParameterEntry(int skillId, int skillLevel) {
		super(skillId, skillLevel);
	}

	/** 是否就绪。 / Whether Ready. */
	@Override
	public boolean isReady(int hpPercentage, long fightingTimeInMSec) {
		return true;
	}

	/** 概率就绪 / chance Ready. */
	@Override
	public boolean chanceReady() {
		return true;
	}

	/** 生命就绪 / Hp Ready */
	@Override
	public boolean hpReady(int hpPercentage) {
		return true;
	}

	/** 时间就绪 / time Ready. */
	@Override
	public boolean timeReady(long fightingTimeInMSec) {
		return true;
	}

	/** 是否拥有冷却。 / Whether cooldown. */
	@Override
	public boolean hasCooldown() {
		return false;
	}

	/** 刷新时使用 / Use In Spawned. */
	@Override
	public boolean UseInSpawned() {
		return true;
	}
}
