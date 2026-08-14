package com.aionemu.gameserver.model.skill;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate;

/**
 * NPC 技能模板条目，用于技能相关逻辑。
 * Npc Skill Template Entry for skill logic.
 */

class NpcSkillTemplateEntry extends NpcSkillEntry {

	private final NpcSkillTemplate template;

	public NpcSkillTemplateEntry(NpcSkillTemplate template) {
		super(template.getSkillid(), template.getSkillLevel());
		this.template = template;
	}

	/** 是否就绪。 / Whether Ready. */
	@Override
	public boolean isReady(int hpPercentage, long fightingTimeInMSec) {
		if (!hasUsesLeft() || hasCooldown() || !chanceReady()) {
			return false;
		}

		switch (template.getConjunctionType()) {
		case XOR:
			return (hpReady(hpPercentage) && !timeReady(fightingTimeInMSec))
					|| (!hpReady(hpPercentage) && timeReady(fightingTimeInMSec));
		case OR:
			return hpReady(hpPercentage) || timeReady(fightingTimeInMSec);
		case AND:
			return hpReady(hpPercentage) && timeReady(fightingTimeInMSec);
		default:
			return false;
		}
	}

	/** 概率就绪 / chance Ready. */
	@Override
	public boolean chanceReady() {
		return Rnd.get(0, 100) < template.getProbability();
	}

	/** 生命就绪 / Hp Ready */
	@Override
	public boolean hpReady(int hpPercentage) {
		if (template.getMaxhp() == 0 && template.getMinhp() == 0) {
			return true;
		} else if (template.getMaxhp() >= hpPercentage && template.getMinhp() <= hpPercentage) {
			return true;
		} else {
			return false;
		}
	}

	/** 时间就绪 / time Ready. */
	@Override
	public boolean timeReady(long fightingTimeInMSec) {
		if (template.getMaxTime() == 0 && template.getMinTime() == 0) {
			return true;
		} else if (template.getMaxTime() >= fightingTimeInMSec && template.getMinTime() <= fightingTimeInMSec) {
			return true;
		} else {
			return false;
		}
	}

	/** 是否拥有冷却。 / Whether cooldown. */
	@Override
	public boolean hasCooldown() {
		return SkillConfig.scaleCooldown(template.getCooldown()) > (System.currentTimeMillis() - lastTimeUsed);
	}

	/** 生成时使用 / Use in spawned. */
	@Override
	public boolean UseInSpawned() {
		return template.getUseInSpawned();
	}

	@Override
	public boolean isUltraSkill() {
		return template.isUltraSkill();
	}

	@Override
	public boolean hasUsesLeft() {
		return template.getCount() == 0 || useCount < template.getCount();
	}
}
