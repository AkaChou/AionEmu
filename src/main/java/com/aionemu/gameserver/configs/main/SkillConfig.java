package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 技能冷却、DP 消耗与魔增上限配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Skill cooldown, DP consumption and magic boost cap configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class SkillConfig {

	/**
	 * 技能冷却时间全局倍率（0.01~1）。
	 * Global skill cooldown multiplier (0.01~1).
	 */
	@Property(key = "gameserver.skill.cooldown.multiplier", defaultValue = "1")
	public static double COOLDOWN_MULTIPLIER = 1;

	/**
	 * 施放技能时是否消耗 DP。
	 * Whether casting skills consumes DP.
	 */
	@Property(key = "gameserver.skill.dp.consume", defaultValue = "true")
	public static boolean CONSUME_DP = true;

	/**
	 * 魔法增幅（魔增）上限值。
	 * Magic boost cap value.
	 */
	@Property(key = "gameserver.magicboost.cap", defaultValue = "6500")
	public static int MAGICBOOST_CAP = 6500;

	@Getter
	private final SkillProperties skill = new SkillProperties();
	@Getter
	private final MagicboostProperties magicboost = new MagicboostProperties();

	@Getter
	public static class SkillProperties {
		private final CooldownProperties cooldown = new CooldownProperties();
		private final DpProperties dp = new DpProperties();

		@Getter
		public static class CooldownProperties {
			public double getMultiplier() {
				return COOLDOWN_MULTIPLIER;
			}

			public void setMultiplier(double multiplier) {
				COOLDOWN_MULTIPLIER = multiplier;
			}
		}

		@Getter
		public static class DpProperties {
			public boolean isConsume() {
				return CONSUME_DP;
			}

			public void setConsume(boolean consume) {
				CONSUME_DP = consume;
			}
		}
	}

	@Getter
	public static class MagicboostProperties {
		public int getCap() {
			return MAGICBOOST_CAP;
		}

		public void setCap(int cap) {
			MAGICBOOST_CAP = cap;
		}
	}

	/**
	 * 校验冷却倍率配置的合法性。
	 * Validates the cooldown multiplier configuration.
	 */
	public static void refresh() {
		if (COOLDOWN_MULTIPLIER < 0.01 || COOLDOWN_MULTIPLIER > 1) {
			throw new IllegalArgumentException("Skill cooldown multiplier must be between 0.01 and 1");
		}
	}

	/**
	 * 按倍率缩放冷却时间；非正冷却保持 0。
	 * Scales a cooldown by the multiplier; non-positive cooldowns stay 0.
	 * @param cooldown 原始冷却时间 / original cooldown
	 * @return 缩放后的冷却时间，至少为 1 / scaled cooldown, at least 1
	 */
	public static int scaleCooldown(int cooldown) {
		return cooldown <= 0 ? 0 : Math.max(1, (int) Math.round(cooldown * COOLDOWN_MULTIPLIER));
	}
}
