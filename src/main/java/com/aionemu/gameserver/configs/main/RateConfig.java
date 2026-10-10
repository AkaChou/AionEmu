package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 经验、掉落与各类奖励倍率相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Experience, drop and various reward rate related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Getter
@Component
@ConfigurationProperties(prefix = "gameserver")
public class RateConfig {

	/**
	 * 普通玩家荣耀点损失倍率。
	 * Regular player glory point loss rate.
	 */
	@Property(key = "gameserver.rate.regular.gp.player.loss", defaultValue = "1.0")
	public static float GP_PLAYER_LOSS_RATE;
	/**
	 * 高级会员荣耀点损失倍率。
	 * Premium player glory point loss rate.
	 */
	@Property(key = "gameserver.rate.premium.gp.player.loss", defaultValue = "2.0")
	public static float PREMIUM_GP_PLAYER_LOSS_RATE;
	/**
	 * VIP 荣耀点损失倍率。
	 * VIP player glory point loss rate.
	 */
	@Property(key = "gameserver.rate.vip.gp.player.loss", defaultValue = "3.0")
	public static float VIP_GP_PLAYER_LOSS_RATE;
	/**
	 * 普通玩家经验倍率。
	 * Regular player XP rate.
	 */
	@Property(key = "gameserver.rate.regular.xp", defaultValue = "1")
	public static float XP_RATE;
	/**
	 * 高级会员经验倍率。
	 * Premium player XP rate.
	 */
	@Property(key = "gameserver.rate.premium.xp", defaultValue = "2")
	public static float PREMIUM_XP_RATE;
	/**
	 * VIP 经验倍率。
	 * VIP player XP rate.
	 */
	@Property(key = "gameserver.rate.vip.xp", defaultValue = "3")
	public static float VIP_XP_RATE;
	/**
	 * 普通玩家组队经验倍率。
	 * Regular group XP rate.
	 */
	@Property(key = "gameserver.rate.regular.group.xp", defaultValue = "1")
	public static float GROUPXP_RATE;
	/**
	 * 高级会员组队经验倍率。
	 * Premium group XP rate.
	 */
	@Property(key = "gameserver.rate.premium.group.xp", defaultValue = "2")
	public static float PREMIUM_GROUPXP_RATE;
	/**
	 * VIP 组队经验倍率。
	 * VIP group XP rate.
	 */
	@Property(key = "gameserver.rate.vip.group.xp", defaultValue = "3")
	public static float VIP_GROUPXP_RATE;
	/**
	 * 普通玩家任务经验倍率。
	 * Regular quest XP rate.
	 */
	@Property(key = "gameserver.rate.regular.quest.xp", defaultValue = "1")
	public static float QUEST_XP_RATE;
	/**
	 * 高级会员任务经验倍率。
	 * Premium quest XP rate.
	 */
	@Property(key = "gameserver.rate.premium.quest.xp", defaultValue = "2")
	public static float PREMIUM_QUEST_XP_RATE;
	/**
	 * VIP 任务经验倍率。
	 * VIP quest XP rate.
	 */
	@Property(key = "gameserver.rate.vip.quest.xp", defaultValue = "3")
	public static float VIP_QUEST_XP_RATE;
	/**
	 * 普通玩家采集经验倍率。
	 * Regular gathering XP rate.
	 */
	@Property(key = "gameserver.rate.regular.gathering.xp", defaultValue = "1")
	public static float GATHERING_XP_RATE;
	/**
	 * 高级会员采集经验倍率。
	 * Premium gathering XP rate.
	 */
	@Property(key = "gameserver.rate.premium.gathering.xp", defaultValue = "2")
	public static float PREMIUM_GATHERING_XP_RATE;
	/**
	 * VIP 采集经验倍率。
	 * VIP gathering XP rate.
	 */
	@Property(key = "gameserver.rate.vip.gathering.xp", defaultValue = "3")
	public static float VIP_GATHERING_XP_RATE;
	/**
	 * 普通玩家采集次数倍率。
	 * Regular gathering count rate.
	 */
	@Property(key = "gameserver.rate.regular.gathering.count", defaultValue = "1")
	public static int GATHERING_COUNT_RATE;
	/**
	 * 高级会员采集次数倍率。
	 * Premium gathering count rate.
	 */
	@Property(key = "gameserver.rate.premium.gathering.count", defaultValue = "1")
	public static int PREMIUM_GATHERING_COUNT_RATE;
	/**
	 * VIP 采集次数倍率。
	 * VIP gathering count rate.
	 */
	@Property(key = "gameserver.rate.vip.gathering.count", defaultValue = "1")
	public static int VIP_GATHERING_COUNT_RATE;
	/**
	 * 普通玩家制作经验倍率。
	 * Regular crafting XP rate.
	 */
	@Property(key = "gameserver.rate.regular.crafting.xp", defaultValue = "1")
	public static float CRAFTING_XP_RATE;
	/**
	 * 高级会员制作经验倍率。
	 * Premium crafting XP rate.
	 */
	@Property(key = "gameserver.rate.premium.crafting.xp", defaultValue = "2")
	public static float PREMIUM_CRAFTING_XP_RATE;
	/**
	 * VIP 制作经验倍率。
	 * VIP crafting XP rate.
	 */
	@Property(key = "gameserver.rate.vip.crafting.xp", defaultValue = "3")
	public static float VIP_CRAFTING_XP_RATE;
	/**
	 * 普通玩家任务基纳倍率。
	 * Regular quest kinah rate.
	 */
	@Property(key = "gameserver.rate.regular.quest.kinah", defaultValue = "1")
	public static float QUEST_KINAH_RATE;
	/**
	 * 高级会员任务基纳倍率。
	 * Premium quest kinah rate.
	 */
	@Property(key = "gameserver.rate.premium.quest.kinah", defaultValue = "2")
	public static float PREMIUM_QUEST_KINAH_RATE;
	/**
	 * VIP 任务基纳倍率。
	 * VIP quest kinah rate.
	 */
	@Property(key = "gameserver.rate.vip.quest.kinah", defaultValue = "3")
	public static float VIP_QUEST_KINAH_RATE;
	/**
	 * 普通玩家掉落倍率。
	 * Regular drop rate.
	 */
	@Property(key = "gameserver.rate.regular.drop", defaultValue = "1")
	public static float DROP_RATE;
	/**
	 * 高级会员掉落倍率。
	 * Premium drop rate.
	 */
	@Property(key = "gameserver.rate.premium.drop", defaultValue = "2")
	public static float PREMIUM_DROP_RATE;
	/**
	 * VIP 掉落倍率。
	 * VIP drop rate.
	 */
	@Property(key = "gameserver.rate.vip.drop", defaultValue = "3")
	public static float VIP_DROP_RATE;
	/**
	 * 普通玩家 AP 获取倍率。
	 * Regular player AP gain rate.
	 */
	@Property(key = "gameserver.rate.regular.ap.player.gain", defaultValue = "1")
	public static float AP_PLAYER_GAIN_RATE;
	/**
	 * 高级会员 AP 获取倍率。
	 * Premium player AP gain rate.
	 */
	@Property(key = "gameserver.rate.premium.ap.player.gain", defaultValue = "2")
	public static float PREMIUM_AP_PLAYER_GAIN_RATE;
	/**
	 * VIP AP 获取倍率。
	 * VIP player AP gain rate.
	 */
	@Property(key = "gameserver.rate.vip.ap.player.gain", defaultValue = "3")
	public static float VIP_AP_PLAYER_GAIN_RATE;
	/**
	 * 普通玩家击杀经验获取倍率。
	 * Regular player XP gain rate from player kills.
	 */
	@Property(key = "gameserver.rate.regular.xp.player.gain", defaultValue = "1")
	public static float XP_PLAYER_GAIN_RATE;
	/**
	 * 高级会员击杀经验获取倍率。
	 * Premium player XP gain rate from player kills.
	 */
	@Property(key = "gameserver.rate.premium.xp.player.gain", defaultValue = "2")
	public static float PREMIUM_XP_PLAYER_GAIN_RATE;
	/**
	 * VIP 击杀经验获取倍率。
	 * VIP player XP gain rate from player kills.
	 */
	@Property(key = "gameserver.rate.vip.xp.player.gain", defaultValue = "3")
	public static float VIP_XP_PLAYER_GAIN_RATE;
	/**
	 * 普通玩家 AP 损失倍率。
	 * Regular player AP loss rate.
	 */
	@Property(key = "gameserver.rate.regular.ap.player.loss", defaultValue = "1")
	public static float AP_PLAYER_LOSS_RATE;
	/**
	 * 高级会员 AP 损失倍率。
	 * Premium player AP loss rate.
	 */
	@Property(key = "gameserver.rate.premium.ap.player.loss", defaultValue = "2")
	public static float PREMIUM_AP_PLAYER_LOSS_RATE;
	/**
	 * VIP AP 损失倍率。
	 * VIP player AP loss rate.
	 */
	@Property(key = "gameserver.rate.vip.ap.player.loss", defaultValue = "3")
	public static float VIP_AP_PLAYER_LOSS_RATE;
	/**
	 * 普通玩家 NPC AP 倍率。
	 * Regular AP rate from NPCs.
	 */
	@Property(key = "gameserver.rate.regular.ap.npc", defaultValue = "1")
	public static float AP_NPC_RATE;
	/**
	 * 高级会员 NPC AP 倍率。
	 * Premium AP rate from NPCs.
	 */
	@Property(key = "gameserver.rate.premium.ap.npc", defaultValue = "2")
	public static float PREMIUM_AP_NPC_RATE;
	/**
	 * VIP NPC AP 倍率。
	 * VIP AP rate from NPCs.
	 */
	@Property(key = "gameserver.rate.vip.ap.npc", defaultValue = "3")
	public static float VIP_AP_NPC_RATE;
	/**
	 * 普通玩家 NPC DP 倍率。
	 * Regular DP rate from NPCs.
	 */
	@Property(key = "gameserver.rate.regular.dp.npc", defaultValue = "1")
	public static float DP_NPC_RATE;
	/**
	 * 高级会员 NPC DP 倍率。
	 * Premium DP rate from NPCs.
	 */
	@Property(key = "gameserver.rate.premium.dp.npc", defaultValue = "2")
	public static float PREMIUM_DP_NPC_RATE;
	/**
	 * VIP NPC DP 倍率。
	 * VIP DP rate from NPCs.
	 */
	@Property(key = "gameserver.rate.vip.dp.npc", defaultValue = "3")
	public static float VIP_DP_NPC_RATE;
	/**
	 * 普通玩家 DP 倍率。
	 * Regular player DP rate.
	 */
	@Property(key = "gameserver.rate.regular.dp.player", defaultValue = "1")
	public static float DP_PLAYER_RATE;
	/**
	 * 高级会员 DP 倍率。
	 * Premium player DP rate.
	 */
	@Property(key = "gameserver.rate.premium.dp.player", defaultValue = "2")
	public static float PREMIUM_DP_PLAYER_RATE;
	/**
	 * VIP DP 倍率。
	 * VIP player DP rate.
	 */
	@Property(key = "gameserver.rate.vip.dp.player", defaultValue = "3")
	public static float VIP_DP_PLAYER_RATE;
	/**
	 * 战舰奖励倍率。
	 * Dredgion reward rate.
	 */
	@Property(key = "gameserver.rate.dredgion", defaultValue = "1.6")
	public static float DREDGION_REWARD_RATE;

	/**
	 * 普通玩家孤独竞技场奖励倍率。
	 * Regular Discipline arena reward rate.
	 */
	@Property(key = "gameserver.rate.regular.pvparena.discipline", defaultValue = "1")
	public static float PVP_ARENA_DISCIPLINE_REWARD_RATE;
	/**
	 * 高级会员孤独竞技场奖励倍率。
	 * Premium Discipline arena reward rate.
	 */
	@Property(key = "gameserver.rate.premium.pvparena.discipline", defaultValue = "1")
	public static float PREMIUM_PVP_ARENA_DISCIPLINE_REWARD_RATE;
	/**
	 * VIP 孤独竞技场奖励倍率。
	 * VIP Discipline arena reward rate.
	 */
	@Property(key = "gameserver.rate.vip.pvparena.discipline", defaultValue = "1")
	public static float VIP_PVP_ARENA_DISCIPLINE_REWARD_RATE;
	/**
	 * 普通玩家混沌竞技场奖励倍率。
	 * Regular Chaos arena reward rate.
	 */
	@Property(key = "gameserver.rate.regular.pvparena.chaos", defaultValue = "1")
	public static float PVP_ARENA_CHAOS_REWARD_RATE;
	/**
	 * 高级会员混沌竞技场奖励倍率。
	 * Premium Chaos arena reward rate.
	 */
	@Property(key = "gameserver.rate.premium.pvparena.chaos", defaultValue = "1")
	public static float PREMIUM_PVP_ARENA_CHAOS_REWARD_RATE;
	/**
	 * VIP 混沌竞技场奖励倍率。
	 * VIP Chaos arena reward rate.
	 */
	@Property(key = "gameserver.rate.vip.pvparena.chaos", defaultValue = "1")
	public static float VIP_PVP_ARENA_CHAOS_REWARD_RATE;
	/**
	 * 普通玩家合作竞技场奖励倍率。
	 * Regular Harmony arena reward rate.
	 */
	@Property(key = "gameserver.rate.regular.pvparena.harmony", defaultValue = "1")
	public static float PVP_ARENA_HARMONY_REWARD_RATE;
	/**
	 * 高级会员合作竞技场奖励倍率。
	 * Premium Harmony arena reward rate.
	 */
	@Property(key = "gameserver.rate.premium.pvparena.harmony", defaultValue = "1")
	public static float PREMIUM_PVP_ARENA_HARMONY_REWARD_RATE;
	/**
	 * VIP 合作竞技场奖励倍率。
	 * VIP Harmony arena reward rate.
	 */
	@Property(key = "gameserver.rate.vip.pvparena.harmony", defaultValue = "1")
	public static float VIP_PVP_ARENA_HARMONY_REWARD_RATE;
	/**
	 * 普通玩家荣耀竞技场奖励倍率。
	 * Regular Glory arena reward rate.
	 */
	@Property(key = "gameserver.rate.regular.pvparena.glory", defaultValue = "1")
	public static float PVP_ARENA_GLORY_REWARD_RATE;
	/**
	 * 高级会员荣耀竞技场奖励倍率。
	 * Premium Glory arena reward rate.
	 */
	@Property(key = "gameserver.rate.premium.pvparena.glory", defaultValue = "1")
	public static float PREMIUM_PVP_ARENA_GLORY_REWARD_RATE;
	/**
	 * VIP 荣耀竞技场奖励倍率。
	 * VIP Glory arena reward rate.
	 */
	@Property(key = "gameserver.rate.vip.pvparena.glory", defaultValue = "1")
	public static float VIP_PVP_ARENA_GLORY_REWARD_RATE;

	/**
	 * 普通玩家点券奖励倍率。
	 * Regular toll reward rate.
	 */
	@Property(key = "gameserver.rate.regular.toll.reward", defaultValue = "1")
	public static float TOLL_REWARD_RATE;
	/**
	 * 高级会员点券奖励倍率。
	 * Premium toll reward rate.
	 */
	@Property(key = "gameserver.rate.premium.toll.reward", defaultValue = "1")
	public static float PREMIUM_TOLL_REWARD_RATE;
	/**
	 * VIP 点券奖励倍率。
	 * VIP toll reward rate.
	 */
	@Property(key = "gameserver.rate.vip.toll.reward", defaultValue = "1")
	public static float VIP_TOLL_REWARD_RATE;

	/**
	 * 普通玩家任务 AP 倍率。
	 * Regular quest AP rate.
	 */
	@Property(key = "gameserver.rate.regular.quest.ap", defaultValue = "1")
	public static float QUEST_AP_RATE;
	/**
	 * 高级会员任务 AP 倍率。
	 * Premium quest AP rate.
	 */
	@Property(key = "gameserver.rate.premium.quest.ap", defaultValue = "2")
	public static float PREMIUM_QUEST_AP_RATE;
	/**
	 * VIP 任务 AP 倍率。
	 * VIP quest AP rate.
	 */
	@Property(key = "gameserver.rate.vip.quest.ap", defaultValue = "3")
	public static float VIP_QUEST_AP_RATE;

	/**
	 * 普通玩家任务荣耀点倍率。
	 * Regular quest glory point rate.
	 */
	@Property(key = "gameserver.rate.regular.quest.gp", defaultValue = "1")
	public static float QUEST_GP_RATE;
	/**
	 * 高级会员任务荣耀点倍率。
	 * Premium quest glory point rate.
	 */
	@Property(key = "gameserver.rate.premium.quest.gp", defaultValue = "2")
	public static float PREMIUM_QUEST_GP_RATE;
	/**
	 * VIP 任务荣耀点倍率。
	 * VIP quest glory point rate.
	 */
	@Property(key = "gameserver.rate.vip.quest.gp", defaultValue = "3")
	public static float VIP_QUEST_GP_RATE;

	/**
	 * 普通玩家欧比斯登陆点任务倍率。
	 * Regular abyss landing quest operation rate.
	 */
	@Property(key = "gameserver.rate.regular.quest.abyss_op", defaultValue = "1")
	public static float QUEST_ABYSS_OP_RATE;
	/**
	 * 高级会员欧比斯登陆点任务倍率。
	 * Premium abyss landing quest operation rate.
	 */
	@Property(key = "gameserver.rate.premium.quest.abyss_op", defaultValue = "2")
	public static float PREMIUM_QUEST_ABYSS_OP_RATE;
	/**
	 * VIP 欧比斯登陆点任务倍率。
	 * VIP abyss landing quest operation rate.
	 */
	@Property(key = "gameserver.rate.vip.quest.abyss_op", defaultValue = "3")
	public static float VIP_QUEST_ABYSS_OP_RATE;

	/**
	 * 成长光环触发概率。
	 * Aura of Growth trigger chance.
	 */
	@Property(key = "gameserver.rate.aura.of.growth.chance", defaultValue = "50")
	public static int AURA_OF_GROWTH;

	/**
	 * 普通玩家任务经验加速倍率。
	 * Regular quest exp boost rate.
	 */
	@Property(key = "gameserver.rate.regular.quest.exp_boost", defaultValue = "1")
	public static float QUEST_EXP_BOOST_RATE;
	/**
	 * 高级会员任务经验加速倍率。
	 * Premium quest exp boost rate.
	 */
	@Property(key = "gameserver.rate.premium.quest.exp_boost", defaultValue = "2")
	public static float PREMIUM_QUEST_EXP_BOOST_RATE;
	/**
	 * VIP 任务经验加速倍率。
	 * VIP quest exp boost rate.
	 */
	@Property(key = "gameserver.rate.vip.quest.exp_boost", defaultValue = "3")
	public static float VIP_QUEST_EXP_BOOST_RATE;

	/**
	 * 普通玩家图鉴倍率。
	 * Regular Atreian Bestiary book rate.
	 */
	@Property(key = "gameserver.rate.regular.book", defaultValue = "1")
	public static float BOOK_RATE;
	/**
	 * 高级会员图鉴倍率。
	 * Premium Atreian Bestiary book rate.
	 */
	@Property(key = "gameserver.rate.premium.book", defaultValue = "2")
	public static float PREMIUM_BOOK_RATE;
	/**
	 * VIP 图鉴倍率。
	 * VIP Atreian Bestiary book rate.
	 */
	@Property(key = "gameserver.rate.vip.book", defaultValue = "3")
	public static float VIP_BOOK_RATE;

	/**
	 * 普通玩家宠物喂养倍率。
	 * Regular pet feeding rate.
	 */
	@Property(key = "gameserver.rate.regular.pet.feeding", defaultValue = "1")
	public static float PET_FEEDING_RATE;
	/**
	 * 高级会员宠物喂养倍率。
	 * Premium pet feeding rate.
	 */
	@Property(key = "gameserver.rate.premium.pet.feeding", defaultValue = "2")
	public static float PREMIUM_PET_FEEDING_RATE;
	/**
	 * VIP 宠物喂养倍率。
	 * VIP pet feeding rate.
	 */
	@Property(key = "gameserver.rate.vip.pet.feeding", defaultValue = "3")
	public static float VIP_PET_FEEDING_RATE;

	/**
	 * 普通怪物 HP 倍率。
	 * Normal monsters HP rate multiplier.
	 */
	@Property(key = "gameserver.rate.normal.mobs.rate.hp", defaultValue = "1.0")
	public static float NORMAL_MOBS_RATE_HP;
	/**
	 * 普通怪物攻击力倍率。
	 * Normal monsters power rate multiplier.
	 */
	@Property(key = "gameserver.rate.normal.mobs.rate.pw", defaultValue = "1.0")
	public static float NORMAL_MOBS_RATE_PW;

	/**
	 * 精英怪物 HP 倍率。
	 * Elite monsters HP rate multiplier.
	 */
	@Property(key = "gameserver.rate.elite.mobs.rate.hp", defaultValue = "1.0")
	public static float ELITE_MOBS_RATE_HP;
	/**
	 * 精英怪物攻击力倍率。
	 * Elite monsters power rate multiplier.
	 */
	@Property(key = "gameserver.rate.elite.mobs.rate.pw", defaultValue = "1.0")
	public static float ELITE_MOBS_RATE_PW;

	/**
	 * 英雄怪物 HP 倍率。
	 * Hero monsters HP rate multiplier.
	 */
	@Property(key = "gameserver.rate.hero.mobs.rate.hp", defaultValue = "1.0")
	public static float HERO_MOBS_RATE_HP;
	/**
	 * 英雄怪物攻击力倍率。
	 * Hero monsters power rate multiplier.
	 */
	@Property(key = "gameserver.rate.hero.mobs.rate.pw", defaultValue = "1.0")
	public static float HERO_MOBS_RATE_PW;

	/**
	 * 传说怪物 HP 倍率。
	 * Legendary monsters HP rate multiplier.
	 */
	@Property(key = "gameserver.rate.legendary.mobs.rate.hp", defaultValue = "1.0")
	public static float LEGENDARY_MOBS_RATE_HP;
	/**
	 * 传说怪物攻击力倍率。
	 * Legendary monsters power rate multiplier.
	 */
	@Property(key = "gameserver.rate.legendary.mobs.rate.pw", defaultValue = "1.0")
	public static float LEGENDARY_MOBS_RATE_PW;

	/**
	 * 全局伤害倍率。
	 * Global damage multiplier.
	 */
	@Property(key = "gameserver.damage.multiplier", defaultValue = "1.0")
	public static float DAMAGE_MULTIPLIER;

	/**
	 * 普通玩家荣耀点获取倍率。
	 * Regular player glory point gain rate.
	 */
	@Property(key = "gameserver.rate.regular.gp.player.gain", defaultValue = "1")
	public static float GP_PLAYER_GAIN_RATE;
	/**
	 * 高级会员荣耀点获取倍率。
	 * Premium player glory point gain rate.
	 */
	@Property(key = "gameserver.rate.premium.gp.player.gain", defaultValue = "2")
	public static float PREMIUM_GP_PLAYER_GAIN_RATE;
	/**
	 * VIP 荣耀点获取倍率。
	 * VIP player glory point gain rate.
	 */
	@Property(key = "gameserver.rate.vip.gp.player.gain", defaultValue = "3")
	public static float VIP_GP_PLAYER_GAIN_RATE;

	private final RateProperties rate = new RateProperties();
	private final DamageProperties damage = new DamageProperties();

	public enum MembershipTier {
		REGULAR,
		PREMIUM,
		VIP
	}

	@Getter
	public static class RateProperties {
		private final TierRateProperties regular = new TierRateProperties(MembershipTier.REGULAR);
		private final TierRateProperties premium = new TierRateProperties(MembershipTier.PREMIUM);
		private final TierRateProperties vip = new TierRateProperties(MembershipTier.VIP);

		private final AuraProperties aura = new AuraProperties();
		private final MobCategoryProperties normal = new MobCategoryProperties(
			val -> NORMAL_MOBS_RATE_HP = val, val -> NORMAL_MOBS_RATE_PW = val,
			() -> NORMAL_MOBS_RATE_HP, () -> NORMAL_MOBS_RATE_PW
		);
		private final MobCategoryProperties elite = new MobCategoryProperties(
			val -> ELITE_MOBS_RATE_HP = val, val -> ELITE_MOBS_RATE_PW = val,
			() -> ELITE_MOBS_RATE_HP, () -> ELITE_MOBS_RATE_PW
		);
		private final MobCategoryProperties hero = new MobCategoryProperties(
			val -> HERO_MOBS_RATE_HP = val, val -> HERO_MOBS_RATE_PW = val,
			() -> HERO_MOBS_RATE_HP, () -> HERO_MOBS_RATE_PW
		);
		private final MobCategoryProperties legendary = new MobCategoryProperties(
			val -> LEGENDARY_MOBS_RATE_HP = val, val -> LEGENDARY_MOBS_RATE_PW = val,
			() -> LEGENDARY_MOBS_RATE_HP, () -> LEGENDARY_MOBS_RATE_PW
		);

		public float getDredgion() {
			return DREDGION_REWARD_RATE;
		}

		public void setDredgion(float dredgion) {
			DREDGION_REWARD_RATE = dredgion;
		}

		@Getter
		public static class AuraProperties {
			private final OfProperties of = new OfProperties();

			@Getter
			public static class OfProperties {
				private final GrowthProperties growth = new GrowthProperties();

				public static class GrowthProperties {
					public int getChance() {
						return AURA_OF_GROWTH;
					}

					public void setChance(int chance) {
						AURA_OF_GROWTH = chance;
					}
				}
			}
		}

		@Getter
		public static class MobCategoryProperties {
			private final MobsProperties mobs;

			public MobCategoryProperties(Consumer<Float> hpSetter, Consumer<Float> pwSetter,
										 java.util.function.Supplier<Float> hpGetter, java.util.function.Supplier<Float> pwGetter) {
				this.mobs = new MobsProperties(hpSetter, pwSetter, hpGetter, pwGetter);
			}

			@Getter
			public static class MobsProperties {
				private final RateSubProperties rate;

				public MobsProperties(Consumer<Float> hpSetter, Consumer<Float> pwSetter,
									  java.util.function.Supplier<Float> hpGetter, java.util.function.Supplier<Float> pwGetter) {
					this.rate = new RateSubProperties(hpSetter, pwSetter, hpGetter, pwGetter);
				}

				public static class RateSubProperties {
					private final Consumer<Float> hpSetter;
					private final Consumer<Float> pwSetter;
					private final java.util.function.Supplier<Float> hpGetter;
					private final java.util.function.Supplier<Float> pwGetter;

					public RateSubProperties(Consumer<Float> hpSetter, Consumer<Float> pwSetter,
											 java.util.function.Supplier<Float> hpGetter, java.util.function.Supplier<Float> pwGetter) {
						this.hpSetter = hpSetter;
						this.pwSetter = pwSetter;
						this.hpGetter = hpGetter;
						this.pwGetter = pwGetter;
					}

					public float getHp() {
						return hpGetter.get();
					}

					public void setHp(float hp) {
						hpSetter.accept(hp);
					}

					public float getPw() {
						return pwGetter.get();
					}

					public void setPw(float pw) {
						pwSetter.accept(pw);
					}
				}
			}
		}

		@Getter
		public static class TierRateProperties {
			private final MembershipTier tier;
			private final GpProperties gp;
			private final GroupProperties group;
			private final QuestProperties quest;
			private final GatheringProperties gathering;
			private final CraftingProperties crafting;
			private final ApProperties ap;
			private final DpProperties dp;
			private final PvparenaProperties pvparena;
			private final TollProperties toll;
			private final PetProperties pet;

			public TierRateProperties(MembershipTier tier) {
				this.tier = tier;
				this.gp = new GpProperties(tier);
				this.group = new GroupProperties(tier);
				this.quest = new QuestProperties(tier);
				this.gathering = new GatheringProperties(tier);
				this.crafting = new CraftingProperties(tier);
				this.ap = new ApProperties(tier);
				this.dp = new DpProperties(tier);
				this.pvparena = new PvparenaProperties(tier);
				this.toll = new TollProperties(tier);
				this.pet = new PetProperties(tier);
			}

			public float getXp() {
				return switch (tier) {
					case REGULAR -> XP_RATE;
					case PREMIUM -> PREMIUM_XP_RATE;
					case VIP -> VIP_XP_RATE;
				};
			}

			public void setXp(float xp) {
				switch (tier) {
					case REGULAR -> XP_RATE = xp;
					case PREMIUM -> PREMIUM_XP_RATE = xp;
					case VIP -> VIP_XP_RATE = xp;
				}
			}

			public float getDrop() {
				return switch (tier) {
					case REGULAR -> DROP_RATE;
					case PREMIUM -> PREMIUM_DROP_RATE;
					case VIP -> VIP_DROP_RATE;
				};
			}

			public void setDrop(float drop) {
				switch (tier) {
					case REGULAR -> DROP_RATE = drop;
					case PREMIUM -> PREMIUM_DROP_RATE = drop;
					case VIP -> VIP_DROP_RATE = drop;
				}
			}

			public float getBook() {
				return switch (tier) {
					case REGULAR -> BOOK_RATE;
					case PREMIUM -> PREMIUM_BOOK_RATE;
					case VIP -> VIP_BOOK_RATE;
				};
			}

			public void setBook(float book) {
				switch (tier) {
					case REGULAR -> BOOK_RATE = book;
					case PREMIUM -> PREMIUM_BOOK_RATE = book;
					case VIP -> VIP_BOOK_RATE = book;
				}
			}

			@Getter
			public static class GpProperties {
				private final PlayerProperties player;

				public GpProperties(MembershipTier tier) {
					this.player = new PlayerProperties(tier);
				}

				public static class PlayerProperties {
					private final MembershipTier tier;

					public PlayerProperties(MembershipTier tier) {
						this.tier = tier;
					}

					public float getLoss() {
						return switch (tier) {
							case REGULAR -> GP_PLAYER_LOSS_RATE;
							case PREMIUM -> PREMIUM_GP_PLAYER_LOSS_RATE;
							case VIP -> VIP_GP_PLAYER_LOSS_RATE;
						};
					}

					public void setLoss(float loss) {
						switch (tier) {
							case REGULAR -> GP_PLAYER_LOSS_RATE = loss;
							case PREMIUM -> PREMIUM_GP_PLAYER_LOSS_RATE = loss;
							case VIP -> VIP_GP_PLAYER_LOSS_RATE = loss;
						}
					}

					public float getGain() {
						return switch (tier) {
							case REGULAR -> GP_PLAYER_GAIN_RATE;
							case PREMIUM -> PREMIUM_GP_PLAYER_GAIN_RATE;
							case VIP -> VIP_GP_PLAYER_GAIN_RATE;
						};
					}

					public void setGain(float gain) {
						switch (tier) {
							case REGULAR -> GP_PLAYER_GAIN_RATE = gain;
							case PREMIUM -> PREMIUM_GP_PLAYER_GAIN_RATE = gain;
							case VIP -> VIP_GP_PLAYER_GAIN_RATE = gain;
						}
					}
				}
			}

			public static class GroupProperties {
				private final MembershipTier tier;

				public GroupProperties(MembershipTier tier) {
					this.tier = tier;
				}

				public float getXp() {
					return switch (tier) {
						case REGULAR -> GROUPXP_RATE;
						case PREMIUM -> PREMIUM_GROUPXP_RATE;
						case VIP -> VIP_GROUPXP_RATE;
					};
				}

				public void setXp(float xp) {
					switch (tier) {
						case REGULAR -> GROUPXP_RATE = xp;
						case PREMIUM -> PREMIUM_GROUPXP_RATE = xp;
						case VIP -> VIP_GROUPXP_RATE = xp;
					}
				}
			}

			public static class QuestProperties {
				private final MembershipTier tier;

				public QuestProperties(MembershipTier tier) {
					this.tier = tier;
				}

				public float getXp() {
					return switch (tier) {
						case REGULAR -> QUEST_XP_RATE;
						case PREMIUM -> PREMIUM_QUEST_XP_RATE;
						case VIP -> VIP_QUEST_XP_RATE;
					};
				}

				public void setXp(float xp) {
					switch (tier) {
						case REGULAR -> QUEST_XP_RATE = xp;
						case PREMIUM -> PREMIUM_QUEST_XP_RATE = xp;
						case VIP -> VIP_QUEST_XP_RATE = xp;
					}
				}

				public float getKinah() {
					return switch (tier) {
						case REGULAR -> QUEST_KINAH_RATE;
						case PREMIUM -> PREMIUM_QUEST_KINAH_RATE;
						case VIP -> VIP_QUEST_KINAH_RATE;
					};
				}

				public void setKinah(float kinah) {
					switch (tier) {
						case REGULAR -> QUEST_KINAH_RATE = kinah;
						case PREMIUM -> PREMIUM_QUEST_KINAH_RATE = kinah;
						case VIP -> VIP_QUEST_KINAH_RATE = kinah;
					}
				}

				public float getAp() {
					return switch (tier) {
						case REGULAR -> QUEST_AP_RATE;
						case PREMIUM -> PREMIUM_QUEST_AP_RATE;
						case VIP -> VIP_QUEST_AP_RATE;
					};
				}

				public void setAp(float ap) {
					switch (tier) {
						case REGULAR -> QUEST_AP_RATE = ap;
						case PREMIUM -> PREMIUM_QUEST_AP_RATE = ap;
						case VIP -> VIP_QUEST_AP_RATE = ap;
					}
				}

				public float getGp() {
					return switch (tier) {
						case REGULAR -> QUEST_GP_RATE;
						case PREMIUM -> PREMIUM_QUEST_GP_RATE;
						case VIP -> VIP_QUEST_GP_RATE;
					};
				}

				public void setGp(float gp) {
					switch (tier) {
						case REGULAR -> QUEST_GP_RATE = gp;
						case PREMIUM -> PREMIUM_QUEST_GP_RATE = gp;
						case VIP -> VIP_QUEST_GP_RATE = gp;
					}
				}

				public float getAbyss_op() {
					return switch (tier) {
						case REGULAR -> QUEST_ABYSS_OP_RATE;
						case PREMIUM -> PREMIUM_QUEST_ABYSS_OP_RATE;
						case VIP -> VIP_QUEST_ABYSS_OP_RATE;
					};
				}

				public void setAbyss_op(float abyssOp) {
					switch (tier) {
						case REGULAR -> QUEST_ABYSS_OP_RATE = abyssOp;
						case PREMIUM -> PREMIUM_QUEST_ABYSS_OP_RATE = abyssOp;
						case VIP -> VIP_QUEST_ABYSS_OP_RATE = abyssOp;
					}
				}

				public float getExp_boost() {
					return switch (tier) {
						case REGULAR -> QUEST_EXP_BOOST_RATE;
						case PREMIUM -> PREMIUM_QUEST_EXP_BOOST_RATE;
						case VIP -> VIP_QUEST_EXP_BOOST_RATE;
					};
				}

				public void setExp_boost(float expBoost) {
					switch (tier) {
						case REGULAR -> QUEST_EXP_BOOST_RATE = expBoost;
						case PREMIUM -> PREMIUM_QUEST_EXP_BOOST_RATE = expBoost;
						case VIP -> VIP_QUEST_EXP_BOOST_RATE = expBoost;
					}
				}
			}

			public static class GatheringProperties {
				private final MembershipTier tier;

				public GatheringProperties(MembershipTier tier) {
					this.tier = tier;
				}

				public float getXp() {
					return switch (tier) {
						case REGULAR -> GATHERING_XP_RATE;
						case PREMIUM -> PREMIUM_GATHERING_XP_RATE;
						case VIP -> VIP_GATHERING_XP_RATE;
					};
				}

				public void setXp(float xp) {
					switch (tier) {
						case REGULAR -> GATHERING_XP_RATE = xp;
						case PREMIUM -> PREMIUM_GATHERING_XP_RATE = xp;
						case VIP -> VIP_GATHERING_XP_RATE = xp;
					}
				}

				public int getCount() {
					return switch (tier) {
						case REGULAR -> GATHERING_COUNT_RATE;
						case PREMIUM -> PREMIUM_GATHERING_COUNT_RATE;
						case VIP -> VIP_GATHERING_COUNT_RATE;
					};
				}

				public void setCount(int count) {
					switch (tier) {
						case REGULAR -> GATHERING_COUNT_RATE = count;
						case PREMIUM -> PREMIUM_GATHERING_COUNT_RATE = count;
						case VIP -> VIP_GATHERING_COUNT_RATE = count;
					}
				}
			}

			public static class CraftingProperties {
				private final MembershipTier tier;

				public CraftingProperties(MembershipTier tier) {
					this.tier = tier;
				}

				public float getXp() {
					return switch (tier) {
						case REGULAR -> CRAFTING_XP_RATE;
						case PREMIUM -> PREMIUM_CRAFTING_XP_RATE;
						case VIP -> VIP_CRAFTING_XP_RATE;
					};
				}

				public void setXp(float xp) {
					switch (tier) {
						case REGULAR -> CRAFTING_XP_RATE = xp;
						case PREMIUM -> PREMIUM_CRAFTING_XP_RATE = xp;
						case VIP -> VIP_CRAFTING_XP_RATE = xp;
					}
				}
			}

			@Getter
			public static class ApProperties {
				private final MembershipTier tier;
				private final PlayerProperties player;

				public ApProperties(MembershipTier tier) {
					this.tier = tier;
					this.player = new PlayerProperties(tier);
				}

				public float getNpc() {
					return switch (tier) {
						case REGULAR -> AP_NPC_RATE;
						case PREMIUM -> PREMIUM_AP_NPC_RATE;
						case VIP -> VIP_AP_NPC_RATE;
					};
				}

				public void setNpc(float npc) {
					switch (tier) {
						case REGULAR -> AP_NPC_RATE = npc;
						case PREMIUM -> PREMIUM_AP_NPC_RATE = npc;
						case VIP -> VIP_AP_NPC_RATE = npc;
					}
				}

				public static class PlayerProperties {
					private final MembershipTier tier;

					public PlayerProperties(MembershipTier tier) {
						this.tier = tier;
					}

					public float getGain() {
						return switch (tier) {
							case REGULAR -> AP_PLAYER_GAIN_RATE;
							case PREMIUM -> PREMIUM_AP_PLAYER_GAIN_RATE;
							case VIP -> VIP_AP_PLAYER_GAIN_RATE;
						};
					}

					public void setGain(float gain) {
						switch (tier) {
							case REGULAR -> AP_PLAYER_GAIN_RATE = gain;
							case PREMIUM -> PREMIUM_AP_PLAYER_GAIN_RATE = gain;
							case VIP -> VIP_AP_PLAYER_GAIN_RATE = gain;
						}
					}

					public float getLoss() {
						return switch (tier) {
							case REGULAR -> AP_PLAYER_LOSS_RATE;
							case PREMIUM -> PREMIUM_AP_PLAYER_LOSS_RATE;
							case VIP -> VIP_AP_PLAYER_LOSS_RATE;
						};
					}

					public void setLoss(float loss) {
						switch (tier) {
							case REGULAR -> AP_PLAYER_LOSS_RATE = loss;
							case PREMIUM -> PREMIUM_AP_PLAYER_LOSS_RATE = loss;
							case VIP -> VIP_AP_PLAYER_LOSS_RATE = loss;
						}
					}
				}
			}

			public static class DpProperties {
				private final MembershipTier tier;

				public DpProperties(MembershipTier tier) {
					this.tier = tier;
				}

				public float getNpc() {
					return switch (tier) {
						case REGULAR -> DP_NPC_RATE;
						case PREMIUM -> PREMIUM_DP_NPC_RATE;
						case VIP -> VIP_DP_NPC_RATE;
					};
				}

				public void setNpc(float npc) {
					switch (tier) {
						case REGULAR -> DP_NPC_RATE = npc;
						case PREMIUM -> PREMIUM_DP_NPC_RATE = npc;
						case VIP -> VIP_DP_NPC_RATE = npc;
					}
				}

				public float getPlayer() {
					return switch (tier) {
						case REGULAR -> DP_PLAYER_RATE;
						case PREMIUM -> PREMIUM_DP_PLAYER_RATE;
						case VIP -> VIP_DP_PLAYER_RATE;
					};
				}

				public void setPlayer(float player) {
					switch (tier) {
						case REGULAR -> DP_PLAYER_RATE = player;
						case PREMIUM -> PREMIUM_DP_PLAYER_RATE = player;
						case VIP -> VIP_DP_PLAYER_RATE = player;
					}
				}
			}

			public static class PvparenaProperties {
				private final MembershipTier tier;

				public PvparenaProperties(MembershipTier tier) {
					this.tier = tier;
				}

				public float getDiscipline() {
					return switch (tier) {
						case REGULAR -> PVP_ARENA_DISCIPLINE_REWARD_RATE;
						case PREMIUM -> PREMIUM_PVP_ARENA_DISCIPLINE_REWARD_RATE;
						case VIP -> VIP_PVP_ARENA_DISCIPLINE_REWARD_RATE;
					};
				}

				public void setDiscipline(float discipline) {
					switch (tier) {
						case REGULAR -> PVP_ARENA_DISCIPLINE_REWARD_RATE = discipline;
						case PREMIUM -> PREMIUM_PVP_ARENA_DISCIPLINE_REWARD_RATE = discipline;
						case VIP -> VIP_PVP_ARENA_DISCIPLINE_REWARD_RATE = discipline;
					}
				}

				public float getChaos() {
					return switch (tier) {
						case REGULAR -> PVP_ARENA_CHAOS_REWARD_RATE;
						case PREMIUM -> PREMIUM_PVP_ARENA_CHAOS_REWARD_RATE;
						case VIP -> VIP_PVP_ARENA_CHAOS_REWARD_RATE;
					};
				}

				public void setChaos(float chaos) {
					switch (tier) {
						case REGULAR -> PVP_ARENA_CHAOS_REWARD_RATE = chaos;
						case PREMIUM -> PREMIUM_PVP_ARENA_CHAOS_REWARD_RATE = chaos;
						case VIP -> VIP_PVP_ARENA_CHAOS_REWARD_RATE = chaos;
					}
				}

				public float getHarmony() {
					return switch (tier) {
						case REGULAR -> PVP_ARENA_HARMONY_REWARD_RATE;
						case PREMIUM -> PREMIUM_PVP_ARENA_HARMONY_REWARD_RATE;
						case VIP -> VIP_PVP_ARENA_HARMONY_REWARD_RATE;
					};
				}

				public void setHarmony(float harmony) {
					switch (tier) {
						case REGULAR -> PVP_ARENA_HARMONY_REWARD_RATE = harmony;
						case PREMIUM -> PREMIUM_PVP_ARENA_HARMONY_REWARD_RATE = harmony;
						case VIP -> VIP_PVP_ARENA_HARMONY_REWARD_RATE = harmony;
					}
				}

				public float getGlory() {
					return switch (tier) {
						case REGULAR -> PVP_ARENA_GLORY_REWARD_RATE;
						case PREMIUM -> PREMIUM_PVP_ARENA_GLORY_REWARD_RATE;
						case VIP -> VIP_PVP_ARENA_GLORY_REWARD_RATE;
					};
				}

				public void setGlory(float glory) {
					switch (tier) {
						case REGULAR -> PVP_ARENA_GLORY_REWARD_RATE = glory;
						case PREMIUM -> PREMIUM_PVP_ARENA_GLORY_REWARD_RATE = glory;
						case VIP -> VIP_PVP_ARENA_GLORY_REWARD_RATE = glory;
					}
				}
			}

			@Getter
			public static class TollProperties {
				private final MembershipTier tier;

				public TollProperties(MembershipTier tier) {
					this.tier = tier;
				}

				public float getReward() {
					return switch (tier) {
						case REGULAR -> TOLL_REWARD_RATE;
						case PREMIUM -> PREMIUM_TOLL_REWARD_RATE;
						case VIP -> VIP_TOLL_REWARD_RATE;
					};
				}

				public void setReward(float reward) {
					switch (tier) {
						case REGULAR -> TOLL_REWARD_RATE = reward;
						case PREMIUM -> PREMIUM_TOLL_REWARD_RATE = reward;
						case VIP -> VIP_TOLL_REWARD_RATE = reward;
					}
				}
			}

			public static class PetProperties {
				private final MembershipTier tier;

				public PetProperties(MembershipTier tier) {
					this.tier = tier;
				}

				public float getFeeding() {
					return switch (tier) {
						case REGULAR -> PET_FEEDING_RATE;
						case PREMIUM -> PREMIUM_PET_FEEDING_RATE;
						case VIP -> VIP_PET_FEEDING_RATE;
					};
				}

				public void setFeeding(float feeding) {
					switch (tier) {
						case REGULAR -> PET_FEEDING_RATE = feeding;
						case PREMIUM -> PREMIUM_PET_FEEDING_RATE = feeding;
						case VIP -> VIP_PET_FEEDING_RATE = feeding;
					}
				}
			}
		}
	}

	public static class DamageProperties {
		public float getMultiplier() {
			return DAMAGE_MULTIPLIER;
		}

		public void setMultiplier(float multiplier) {
			DAMAGE_MULTIPLIER = multiplier;
		}
	}
}
