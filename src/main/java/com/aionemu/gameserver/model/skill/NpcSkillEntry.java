package com.aionemu.gameserver.model.skill;

/**
 * NPC 技能条目，用于技能相关逻辑。
 * Npc Skill Entry for skill logic.
 */

public abstract class NpcSkillEntry extends SkillEntry {
	protected long lastTimeUsed = 0;

	public NpcSkillEntry(int skillId, int skillLevel) {
		super(skillId, skillLevel, 0, null, 0, false);
	}

	/** 是否就绪。 / Whether Ready. */
	public abstract boolean isReady(int hpPercentage, long fightingTimeInMSec);

	/** 概率就绪 / chance Ready. */
	public abstract boolean chanceReady();

	/** 生命就绪 / Hp Ready */
	public abstract boolean hpReady(int hpPercentage);

	/** 时间就绪 / time Ready. */
	public abstract boolean timeReady(long fightingTimeInMSec);

	/** 是否拥有冷却。 / Whether cooldown. */
	public abstract boolean hasCooldown();

	/** 刷新时使用 / Use In Spawned. */
	public abstract boolean UseInSpawned();

	/** 返回上次时间已用 / Returns the last time used*/
	public long getLastTimeUsed() {
		return lastTimeUsed;
	}

	/** 设置 last time used / Sets the last time used */
	public void setLastTimeUsed() {
		lastTimeUsed = System.currentTimeMillis();
	}
}
