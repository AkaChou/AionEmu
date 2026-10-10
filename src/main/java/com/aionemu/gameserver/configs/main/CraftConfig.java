package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 制作与采集相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Crafting and gathering related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class CraftConfig {
	/**
	 * 是否允许制作技能不受等级限制提升。
	 * Whether craft skills can level up without level restrictions.
	 */
	@Property(key = "gameserver.craft.skills.unrestricted.levelup.enable", defaultValue = "false")
	public static boolean UNABLE_CRAFT_SKILLS_UNRESTRICTED_LEVELUP;
	/**
	 * 专家制作技能数量上限。
	 * Maximum expert crafting skills.
	 */
	@Property(key = "gameserver.craft.max.expert.skills", defaultValue = "2")
	public static int MAX_EXPERT_CRAFTING_SKILLS;
	/**
	 * 大师制作技能数量上限。
	 * Maximum master crafting skills.
	 */
	@Property(key = "gameserver.craft.max.master.skills", defaultValue = "1")
	public static int MAX_MASTER_CRAFTING_SKILLS;
	/**
	 * 普通玩家制作暴击率。
	 * Craft critical rate for regular players.
	 */
	@Property(key = "gameserver.craft.critical.rate.regular", defaultValue = "15")
	public static int CRAFT_CRIT_RATE;
	/**
	 * 高级会员制作暴击率。
	 * Craft critical rate for premium members.
	 */
	@Property(key = "gameserver.craft.critical.rate.premium", defaultValue = "15")
	public static int PREMIUM_CRAFT_CRIT_RATE;
	/**
	 * VIP 会员制作暴击率。
	 * Craft critical rate for VIP members.
	 */
	@Property(key = "gameserver.craft.critical.rate.vip", defaultValue = "15")
	public static int VIP_CRAFT_CRIT_RATE;
	/**
	 * 普通玩家制作连击率。
	 * Craft combo rate for regular players.
	 */
	@Property(key = "gameserver.craft.combo.rate.regular", defaultValue = "25")
	public static int CRAFT_COMBO_RATE;
	/**
	 * 高级会员制作连击率。
	 * Craft combo rate for premium members.
	 */
	@Property(key = "gameserver.craft.combo.rate.premium", defaultValue = "25")
	public static int PREMIUM_CRAFT_COMBO_RATE;
	/**
	 * VIP 会员制作连击率。
	 * Craft combo rate for VIP members.
	 */
	@Property(key = "gameserver.craft.combo.rate.vip", defaultValue = "25")
	public static int VIP_CRAFT_COMBO_RATE;
	/**
	 * 是否启用制作任务检查（硬编码默认关闭）。
	 * Whether craft task check is enabled (hardcoded default false).
	 */
	public static boolean CRAFT_CHECK_TASK = false;
	/**
	 * 紫色暴击概率。
	 * Chance for purple craft critical.
	 */
	@Property(key = "gameserver.craft.chance.purple.crit", defaultValue = "1")
	public static int CRAFT_CHANCE_PURPLE_CRIT;
	/**
	 * 蓝色暴击概率。
	 * Chance for blue craft critical.
	 */
	@Property(key = "gameserver.craft.chance.blue.crit", defaultValue = "10")
	public static int CRAFT_CHANCE_BLUE_CRIT;
	/**
	 * 瞬间完成概率。
	 * Chance for instant craft completion.
	 */
	@Property(key = "gameserver.craft.chance.instant", defaultValue = "100")
	public static int CRAFT_CHANCE_INSTANT;
	/**
	 * 是否启用采集保护。
	 * Whether gathering protection is enabled.
	 */
	@Property(key = "gameserver.protection.gather.enable", defaultValue = "true")
	public static boolean PROTECTION_GATHER_ENABLE;

	@Getter
	private final CraftProperties craft = new CraftProperties();
	@Getter
	private final ProtectionProperties protection = new ProtectionProperties();

	@Getter
	public static class CraftProperties {
		private final SkillsProperties skills = new SkillsProperties();
		private final MaxProperties max = new MaxProperties();
		private final CriticalProperties critical = new CriticalProperties();
		private final ComboProperties combo = new ComboProperties();
		private final ChanceProperties chance = new ChanceProperties();

		@Getter
		public static class SkillsProperties {
			private final UnrestrictedProperties unrestricted = new UnrestrictedProperties();

			@Getter
			public static class UnrestrictedProperties {
				private final LevelupProperties levelup = new LevelupProperties();

				public static class LevelupProperties {
					public boolean isEnable() {
						return UNABLE_CRAFT_SKILLS_UNRESTRICTED_LEVELUP;
					}

					public void setEnable(boolean enable) {
						UNABLE_CRAFT_SKILLS_UNRESTRICTED_LEVELUP = enable;
					}
				}
			}
		}

		@Getter
		public static class MaxProperties {
			private final ExpertProperties expert = new ExpertProperties();
			private final MasterProperties master = new MasterProperties();

			public static class ExpertProperties {
				public int getSkills() {
					return MAX_EXPERT_CRAFTING_SKILLS;
				}

				public void setSkills(int skills) {
					MAX_EXPERT_CRAFTING_SKILLS = skills;
				}
			}

			public static class MasterProperties {
				public int getSkills() {
					return MAX_MASTER_CRAFTING_SKILLS;
				}

				public void setSkills(int skills) {
					MAX_MASTER_CRAFTING_SKILLS = skills;
				}
			}
		}

		@Getter
		public static class CriticalProperties {
			private final RateProperties rate = new RateProperties();

			public static class RateProperties {
				public int getRegular() {
					return CRAFT_CRIT_RATE;
				}

				public void setRegular(int regular) {
					CRAFT_CRIT_RATE = regular;
				}

				public int getPremium() {
					return PREMIUM_CRAFT_CRIT_RATE;
				}

				public void setPremium(int premium) {
					PREMIUM_CRAFT_CRIT_RATE = premium;
				}

				public int getVip() {
					return VIP_CRAFT_CRIT_RATE;
				}

				public void setVip(int vip) {
					VIP_CRAFT_CRIT_RATE = vip;
				}
			}
		}

		@Getter
		public static class ComboProperties {
			private final RateProperties rate = new RateProperties();

			public static class RateProperties {
				public int getRegular() {
					return CRAFT_COMBO_RATE;
				}

				public void setRegular(int regular) {
					CRAFT_COMBO_RATE = regular;
				}

				public int getPremium() {
					return PREMIUM_CRAFT_COMBO_RATE;
				}

				public void setPremium(int premium) {
					PREMIUM_CRAFT_COMBO_RATE = premium;
				}

				public int getVip() {
					return VIP_CRAFT_COMBO_RATE;
				}

				public void setVip(int vip) {
					VIP_CRAFT_COMBO_RATE = vip;
				}
			}
		}

		@Getter
		public static class ChanceProperties {
			private final PurpleProperties purple = new PurpleProperties();
			private final BlueProperties blue = new BlueProperties();

			public int getInstant() {
				return CRAFT_CHANCE_INSTANT;
			}

			public void setInstant(int instant) {
				CRAFT_CHANCE_INSTANT = instant;
			}

			public static class PurpleProperties {
				public int getCrit() {
					return CRAFT_CHANCE_PURPLE_CRIT;
				}

				public void setCrit(int crit) {
					CRAFT_CHANCE_PURPLE_CRIT = crit;
				}
			}

			public static class BlueProperties {
				public int getCrit() {
					return CRAFT_CHANCE_BLUE_CRIT;
				}

				public void setCrit(int crit) {
					CRAFT_CHANCE_BLUE_CRIT = crit;
				}
			}
		}
	}

	@Getter
	public static class ProtectionProperties {
		private final GatherProperties gather = new GatherProperties();

		public static class GatherProperties {
			public boolean isEnable() {
				return PROTECTION_GATHER_ENABLE;
			}

			public void setEnable(boolean enable) {
				PROTECTION_GATHER_ENABLE = enable;
			}
		}
	}
}
