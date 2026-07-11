package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 经验、掉落与各类奖励倍率相关配置。
 * Experience, drop and various reward rate related configuration.
 */
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
}
