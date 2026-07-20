package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 技能冷却、DP 消耗、魔增上限与束缚行为配置。
 * Skill cooldown, DP consumption, magic boost cap and root behavior configuration.
 */
public class SkillConfig {

	@Property(key = "gameserver.skill.cooldown.multiplier", defaultValue = "1")
	public static double COOLDOWN_MULTIPLIER = 1;

	@Property(key = "gameserver.skill.dp.consume", defaultValue = "true")
	public static boolean CONSUME_DP = true;

	@Property(key = "gameserver.magicboost.cap", defaultValue = "3400")
	public static int MAGICBOOST_CAP = 3400;

	@Property(key = "gameserver.skill.root.break.on.dot", defaultValue = "false")
	public static boolean ROOT_BREAK_ON_DOT = false;

	public static void refresh() {
		if (COOLDOWN_MULTIPLIER < 0.01 || COOLDOWN_MULTIPLIER > 1) {
			throw new IllegalArgumentException("Skill cooldown multiplier must be between 0.01 and 1");
		}
	}

	public static int scaleCooldown(int cooldown) {
		return cooldown <= 0 ? 0 : Math.max(1, (int) Math.round(cooldown * COOLDOWN_MULTIPLIER));
	}
}
