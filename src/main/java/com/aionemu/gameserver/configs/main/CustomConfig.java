package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义功能与世界事件相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Custom features and world event related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Getter
@Component
@ConfigurationProperties(prefix = "gameserver")
public class CustomConfig {

	/**
	 * 是否在登录服显示 GM 信息。
	 * Whether to show GM info on the login server.
	 */
	@Property(key = "gameserver.loginserver.info", defaultValue = "false")
	public static boolean LOGIN_SERVER_INFO;

	/**
	 * 是否启用 PvE 点券掉落奖励。
	 * Whether PvE toll drop rewards are enabled.
	 */
	@Property(key = "gameserver.pve.toll.rewarding.enable", defaultValue = "false")
	public static boolean ENABLE_PVE_TOLL_REWARD;
	/**
	 * PvE 点券掉落触发概率。
	 * Chance to grant PvE toll reward.
	 */
	@Property(key = "gameserver.pve.toll.reward.chance", defaultValue = "50")
	public static int TOLL_PVE_CHANCE;
	/**
	 * PvE 点券掉落数量。
	 * Quantity of PvE toll reward.
	 */
	@Property(key = "gameserver.pve.toll.reward.quantity", defaultValue = "5")
	public static int TOLL_PVE_QUANTITY;
	/**
	 * 允许 PvE 点券掉落的世界 ID 列表。
	 * World IDs allowed for PvE toll rewards.
	 */
	@Property(key = "gameserver.pve.toll.reward.worldid", defaultValue = "5")
	public static String TOLL_PVE_WORLDID;

	/**
	 * 世界频道发言消耗的 AP。
	 * Abyss points cost for world channel chat.
	 */
	@Property(key = "gameserver.worldchannel.costs", defaultValue = "50000")
	public static int WORLD_CHANNEL_AP_COSTS;

	/**
	 * 是否免费使用阵营频道命令。
	 * Whether faction command usage is free.
	 */
	@Property(key = "gameserver.faction.free", defaultValue = "true")
	public static boolean FACTION_FREE_USE;
	/**
	 * 阵营频道命令使用价格。
	 * Price for faction command usage.
	 */
	@Property(key = "gameserver.faction.prices", defaultValue = "10000")
	public static int FACTION_USE_PRICE;
	/**
	 * 是否启用阵营命令频道。
	 * Whether faction command channel is enabled.
	 */
	@Property(key = "gameserver.faction.cmdchannel", defaultValue = "true")
	public static boolean FACTION_CMD_CHANNEL;
	/**
	 * 是否启用阵营聊天频道。
	 * Whether faction chat channels are enabled.
	 */
	@Property(key = "gameserver.faction.chatchannels", defaultValue = "false")
	public static boolean FACTION_CHAT_CHANNEL;
	/**
	 * 是否启用高级会员通知。
	 * Whether premium membership notifications are enabled.
	 */
	@Property(key = "gameserver.premium.notify.enable", defaultValue = "false")
	public static boolean PREMIUM_NOTIFY_ENABLE;
	/**
	 * 是否广播附魔成功公告。
	 * Whether enchant success announcements are enabled.
	 */
	@Property(key = "gameserver.enchant.announce.enable", defaultValue = "true")
	public static boolean ENABLE_ENCHANT_ANNOUNCE;
	/**
	 * 是否允许跨阵营说话。
	 * Whether speaking between factions is allowed.
	 */
	@Property(key = "gameserver.chat.factions.enable", defaultValue = "false")
	public static boolean SPEAKING_BETWEEN_FACTIONS;
	/**
	 * 私聊所需最低等级。
	 * Minimum level required to whisper.
	 */
	@Property(key = "gameserver.chat.whisper.level", defaultValue = "10")
	public static int LEVEL_TO_WHISPER;
	/**
	 * 是否启用跨阵营搜索模式。
	 * Whether faction-aware search mode is enabled.
	 */
	@Property(key = "gameserver.search.factions.mode", defaultValue = "false")
	public static boolean FACTIONS_SEARCH_MODE;
	/**
	 * 是否在搜索列表中显示 GM。
	 * Whether GMs appear in search lists.
	 */
	@Property(key = "gameserver.search.gm.list", defaultValue = "false")
	public static boolean SEARCH_GM_LIST;
	/**
	 * 是否允许跨阵营绑定点。
	 * Whether cross-faction binding is enabled.
	 */
	@Property(key = "gameserver.cross.faction.binding", defaultValue = "false")
	public static boolean ENABLE_CROSS_FACTION_BINDING;
	/**
	 * 是否启用简化二转流程。
	 * Whether simple second-class change is enabled.
	 */
	@Property(key = "gameserver.simple.secondclass.enable", defaultValue = "false")
	public static boolean ENABLE_SIMPLE_2NDCLASS;
	/**
	 * 是否使用技能链触发率。
	 * Whether skill chain trigger rates are used.
	 */
	@Property(key = "gameserver.skill.chain.triggerrate", defaultValue = "true")
	public static boolean SKILL_CHAIN_TRIGGERRATE;
	/**
	 * 管理员染色价格。
	 * Admin dye command price.
	 */
	@Property(key = "gameserver.admin.dye.price", defaultValue = "1000000")
	public static int DYE_PRICE;
	/**
	 * 基础飞行时间（秒）。
	 * Base flight time in seconds.
	 */
	@Property(key = "gameserver.base.flytime", defaultValue = "60")
	public static int BASE_FLYTIME;
	/**
	 * 是否禁用旧名优惠券。
	 * Whether old-name coupons are disabled.
	 */
	@Property(key = "gameserver.oldnames.coupon.disable", defaultValue = "false")
	public static boolean OLD_NAMES_COUPON_DISABLED;
	/**
	 * 是否禁用旧名命令。
	 * Whether old-name command is disabled.
	 */
	@Property(key = "gameserver.oldnames.command.disable", defaultValue = "true")
	public static boolean OLD_NAMES_COMMAND_DISABLED;
	/**
	 * 好友列表容量。
	 * Friend list size.
	 */
	@Property(key = "gameserver.friendlist.size", defaultValue = "90")
	public static int FRIENDLIST_SIZE;
	/**
	 * 基础任务日志栏位上限。
	 * Basic quest journal size limit.
	 */
	@Property(key = "gameserver.basic.questsize.limit", defaultValue = "40")
	public static int BASIC_QUEST_SIZE_LIMIT;
	/**
	 * 是否启用副本系统。
	 * Whether instances are enabled.
	 */
	@Property(key = "gameserver.instances.enable", defaultValue = "true")
	public static boolean ENABLE_INSTANCES;
	/**
	 * 是否启用基纳上限。
	 * Whether kinah cap is enabled.
	 */
	@Property(key = "gameserver.enable.kinah.cap", defaultValue = "false")
	public static boolean ENABLE_KINAH_CAP;
	/**
	 * 基纳上限数值。
	 * Kinah cap value.
	 */
	@Property(key = "gameserver.kinah.cap.value", defaultValue = "1000000000")
	public static long KINAH_CAP_VALUE;
	/**
	 * 导师组是否不获得 AP。
	 * Whether mentor groups receive no AP.
	 */
	@Property(key = "gameserver.noap.mentor.group", defaultValue = "false")
	public static boolean MENTOR_GROUP_AP;
	/**
	 * 是否显示对话框 ID。
	 * Whether dialog IDs are shown.
	 */
	@Property(key = "gameserver.dialog.show.id", defaultValue = "false")
	public static boolean ENABLE_SHOW_DIALOG_ID;
	/**
	 * 是否启用奖励服务。
	 * Whether reward service is enabled.
	 */
	@Property(key = "gameserver.reward.service.enable", defaultValue = "false")
	public static boolean ENABLE_REWARD_SERVICE;
	/**
	 * 是否启用每日限制系统。
	 * Whether daily limits system is enabled.
	 */
	@Property(key = "gameserver.limits.enable", defaultValue = "true")
	public static boolean LIMITS_ENABLED;
	/**
	 * 每日限制重置 Cron 时间。
	 * Cron schedule for limits update/reset.
	 */
	@Property(key = "gameserver.limits.update", defaultValue = "0 0 0 ? * *")
	public static String LIMITS_UPDATE;
	/**
	 * 每日限制倍率。
	 * Daily limits rate multiplier.
	 */
	@Property(key = "gameserver.limits.rate", defaultValue = "1")
	public static int LIMITS_RATE;
	/**
	 * 聊天文本最大长度。
	 * Maximum chat text length.
	 */
	@Property(key = "gameserver.chat.text.length", defaultValue = "150")
	public static int MAX_CHAT_TEXT_LENGHT;
	/**
	 * 欧比斯变身后下线是否保持状态。
	 * Whether abyss transform persists after logout.
	 */
	@Property(key = "gameserver.abyssxform.afterlogout", defaultValue = "false")
	public static boolean ABYSSXFORM_LOGOUT;
	/**
	 * 是否允许副本内决斗。
	 * Whether duels are enabled inside instances.
	 */
	@Property(key = "gameserver.instance.duel.enable", defaultValue = "true")
	public static boolean INSTANCE_DUEL_ENABLE;

	/**
	 * 是否启用地图骑乘限制。
	 * Whether ride restrictions are enabled.
	 */
	@Property(key = "gameserver.ride.restriction.enable", defaultValue = "true")
	public static boolean ENABLE_RIDE_RESTRICTION;
	/**
	 * 是否启用挑战任务。
	 * Whether challenge tasks are enabled.
	 */
	@Property(key = "gameserver.challenge.tasks.enabled", defaultValue = "false")
	public static boolean CHALLENGE_TASKS_ENABLED;
	/**
	 * 是否启用管理员点命令。
	 * Whether admin dot commands are enabled.
	 */
	@Property(key = "gameserver.commands.admin.dot.enable", defaultValue = "false")
	public static boolean ENABLE_ADMIN_DOT_COMMANDS;
	/**
	 * 呼唤特快邮差的冷却时间（秒）。
	 * Express courier summon cooldown in seconds.
	 */
	@Property(key = "gameserver.express.mail.cooldown_seconds", defaultValue = "60")
	public static int EXPRESS_MAIL_COOLDOWN_SECONDS;
	/**
	 * 是否启用时空裂隙。
	 * Whether rifts are enabled.
	 */
	@Property(key = "gameserver.rift.enable", defaultValue = "true")
	public static boolean RIFT_ENABLED;
	/**
	 * 时空裂隙持续时间。
	 * Rift duration.
	 */
	@Property(key = "gameserver.rift.duration", defaultValue = "1")
	public static int RIFT_DURATION;
	/**
	 * 是否启用玩家 AP 经验渐进倍率。
	 * Whether progressive AP rate for players is enabled.
	 */
	@Property(key = "gameserver.enable.exp.progressive.ap.player", defaultValue = "false")
	public static boolean ENABLE_EXP_PROGRESSIVE_AP_PLAYER;
	/**
	 * 是否启用 NPC AP 经验渐进倍率。
	 * Whether progressive AP rate for NPCs is enabled.
	 */
	@Property(key = "gameserver.enable.exp.progressive.ap.npc", defaultValue = "false")
	public static boolean ENABLE_EXP_PROGRESSIVE_AP_NPC;
	/**
	 * 是否启用狩猎经验渐进倍率。
	 * Whether progressive hunting XP rate is enabled.
	 */
	@Property(key = "gameserver.enable.exp.progressive.hunting", defaultValue = "false")
	public static boolean ENABLE_EXP_PROGRESSIVE_HUNTING;
	/**
	 * 是否启用组队狩猎经验渐进倍率。
	 * Whether progressive group hunting XP rate is enabled.
	 */
	@Property(key = "gameserver.enable.exp.progressive.group.hunting", defaultValue = "false")
	public static boolean ENABLE_EXP_PROGRESSIVE_GROUP_HUNTING;
	/**
	 * 是否启用任务经验渐进倍率。
	 * Whether progressive quest XP rate is enabled.
	 */
	@Property(key = "gameserver.enable.exp.progressive.quest", defaultValue = "false")
	public static boolean ENABLE_EXP_PROGRESSIVE_QUEST;
	/**
	 * 是否启用图鉴经验渐进倍率。
	 * Whether progressive bestiary/book XP rate is enabled.
	 */
	@Property(key = "gameserver.enable.exp.progressive.book", defaultValue = "false")
	public static boolean ENABLE_EXP_PROGRESSIVE_BOOK;

	/**
	 * 是否启用漩涡事件。
	 * Whether Vortex event is enabled.
	 */
	@Property(key = "gameserver.vortex.enable", defaultValue = "true")
	public static boolean VORTEX_ENABLED;
	/**
	 * 漩涡事件持续时间。
	 * Vortex event duration.
	 */
	@Property(key = "gameserver.vortex.duration", defaultValue = "2")
	public static int VORTEX_DURATION;

	/**
	 * 是否启用争议之地。
	 * Whether Dispute Land is enabled.
	 */
	@Property(key = "gameserver.dispute.land.enable", defaultValue = "true")
	public static boolean DISPUTE_LAND_ENABLED;
	/**
	 * 争议之地调度 Cron。
	 * Dispute Land schedule cron.
	 */
	@Property(key = "gameserver.dispute.land.schedule", defaultValue = "0 0 2 ? * *")
	public static String DISPUTE_LAND_SCHEDULE;
	/**
	 * 争议之地持续时间。
	 * Dispute Land duration.
	 */
	@Property(key = "gameserver.dispute.land.duration", defaultValue = "2")
	public static int DISPUTE_LAND_DURATION;

	/**
	 * 是否启用贝里特拉入侵。
	 * Whether Beritra invasion is enabled.
	 */
	@Property(key = "gameserver.beritra.enable", defaultValue = "true")
	public static boolean BERITRA_ENABLED;
	/**
	 * 贝里特拉入侵持续时间。
	 * Beritra invasion duration.
	 */
	@Property(key = "gameserver.beritra.duration", defaultValue = "2")
	public static int BERITRA_DURATION;

	/**
	 * 是否启用代理人战斗。
	 * Whether Agent Fight is enabled.
	 */
	@Property(key = "gameserver.agent.enable", defaultValue = "true")
	public static boolean AGENT_ENABLED;
	/**
	 * 代理人战斗持续时间。
	 * Agent Fight duration.
	 */
	@Property(key = "gameserver.agent.duration", defaultValue = "2")
	public static int AGENT_DURATION;

	/**
	 * 是否启用狂暴阿诺哈。
	 * Whether Berserk Anoha is enabled.
	 */
	@Property(key = "gameserver.anoha.enable", defaultValue = "true")
	public static boolean ANOHA_ENABLED;
	/**
	 * 狂暴阿诺哈持续时间。
	 * Berserk Anoha duration.
	 */
	@Property(key = "gameserver.anoha.duration", defaultValue = "1")
	public static int ANOHA_DURATION;

	/**
	 * 是否启用潘斯特拉（Svs）。
	 * Whether Panesterra (Svs) is enabled.
	 */
	@Property(key = "gameserver.svs.enable", defaultValue = "true")
	public static boolean SVS_ENABLED;
	/**
	 * 潘斯特拉持续时间。
	 * Panesterra (Svs) duration.
	 */
	@Property(key = "gameserver.svs.duration", defaultValue = "1")
	public static int SVS_DURATION;

	/**
	 * 是否启用 R.v.R 事件。
	 * Whether R.v.R event is enabled.
	 */
	@Property(key = "gameserver.rvr.enable", defaultValue = "true")
	public static boolean RVR_ENABLED;
	/**
	 * R.v.R 事件持续时间。
	 * R.v.R event duration.
	 */
	@Property(key = "gameserver.rvr.duration", defaultValue = "1")
	public static int RVR_DURATION;

	/**
	 * 是否启用莫尔泰努斯。
	 * Whether Moltenus is enabled.
	 */
	@Property(key = "gameserver.moltenus.enable", defaultValue = "true")
	public static boolean MOLTENUS_ENABLED;
	/**
	 * 莫尔泰努斯持续时间。
	 * Moltenus duration.
	 */
	@Property(key = "gameserver.moltenus.duration", defaultValue = "1")
	public static int MOLTENUS_DURATION;

	/**
	 * 是否启用动态裂隙。
	 * Whether Dynamic Rift is enabled.
	 */
	@Property(key = "gameserver.dynamic.rift.enable", defaultValue = "false")
	public static boolean DYNAMIC_RIFT_ENABLED;
	/**
	 * 动态裂隙龙之时间表 Cron。
	 * Dynamic Rift dragon schedule cron.
	 */
	@Property(key = "gameserver.dynamic.rift.dragon.schedule", defaultValue = "0 0 2 ? * *")
	public static String DYNAMIC_RIFT_DRAGON_SCHEDULE;
	/**
	 * 动态裂隙因德拉图时间表 Cron。
	 * Dynamic Rift Indratoo schedule cron.
	 */
	@Property(key = "gameserver.dynamic.rift.indratoo.schedule", defaultValue = "0 0 2 ? * *")
	public static String DYNAMIC_RIFT_INDRATOO_SCHEDULE;
	/**
	 * 术古商人联盟时间表 Cron。
	 * Shugo Merchant League schedule cron.
	 */
	@Property(key = "gameserver.shugo.merchant.league.schedule", defaultValue = "0 0 2 ? * *")
	public static String SHUGO_MERCHANT_LEAGUE_SCHEDULE;
	/**
	 * 动态裂隙持续时间。
	 * Dynamic Rift duration.
	 */
	@Property(key = "gameserver.dynamic.rift.duration", defaultValue = "1")
	public static int DYNAMIC_RIFT_DURATION;

	/**
	 * 是否启用永恒之塔。
	 * Whether Tower of Eternity is enabled.
	 */
	@Property(key = "gameserver.tower.of.eternity.enable", defaultValue = "true")
	public static boolean TOWER_OF_ETERNITY_ENABLED;
	/**
	 * 永恒之塔时间表 Cron。
	 * Tower of Eternity schedule cron.
	 */
	@Property(key = "gameserver.tower.of.eternity.schedule", defaultValue = "0 0 1 ? * *")
	public static String TOWER_OF_ETERNITY_SCHEDULE;
	/**
	 * 永恒之塔持续时间。
	 * Tower of Eternity duration.
	 */
	@Property(key = "gameserver.tower.of.eternity.duration", defaultValue = "1")
	public static int TOWER_OF_ETERNITY_DURATION;

	/**
	 * 是否启用佐尔希夫战舰。
	 * Whether Zorshiv Dredgion is enabled.
	 */
	@Property(key = "gameserver.zorshiv.dredgion.enable", defaultValue = "true")
	public static boolean ZORSHIV_DREDGION_ENABLED;
	/**
	 * 佐尔希夫战舰持续时间。
	 * Zorshiv Dredgion duration.
	 */
	@Property(key = "gameserver.zorshiv.dredgion.duration", defaultValue = "1")
	public static int ZORSHIV_DREDGION_DURATION;

	/**
	 * 是否启用现场音乐会。
	 * Whether Live Party Concert Hall (IU) is enabled.
	 */
	@Property(key = "gameserver.iu.enable", defaultValue = "false")
	public static boolean IU_ENABLED;
	/**
	 * 现场音乐会时间表 Cron。
	 * Live Party Concert Hall schedule cron.
	 */
	@Property(key = "gameserver.iu.schedule", defaultValue = "0 0 2 ? * *")
	public static String IU_SCHEDULE;
	/**
	 * 现场音乐会持续时间。
	 * Live Party Concert Hall duration.
	 */
	@Property(key = "gameserver.iu.duration", defaultValue = "1")
	public static int IU_DURATION;

	/**
	 * 是否启用梦魇马戏团。
	 * Whether Nightmare Circus is enabled.
	 */
	@Property(key = "gameserver.nightmare.circus.enable", defaultValue = "false")
	public static boolean NIGHTMARE_CIRCUS_ENABLE;
	/**
	 * 梦魇马戏团持续时间。
	 * Nightmare Circus duration.
	 */
	@Property(key = "gameserver.nightmare.circus.duration", defaultValue = "1")
	public static int NIGHTMARE_CIRCUS_DURATION;

	/**
	 * 是否启用征服/献祭事件。
	 * Whether Conquest/Offering is enabled.
	 */
	@Property(key = "gameserver.conquest.enable", defaultValue = "true")
	public static boolean CONQUEST_ENABLED;
	/**
	 * 征服/献祭持续时间。
	 * Conquest/Offering duration.
	 */
	@Property(key = "gameserver.conquest.duration", defaultValue = "1")
	public static int CONQUEST_DURATION;

	/**
	 * 是否启用伊迪安深渊。
	 * Whether Idian Depths is enabled.
	 */
	@Property(key = "gameserver.idian.depths.enable", defaultValue = "true")
	public static boolean IDIAN_DEPTHS_ENABLED;
	/**
	 * 伊迪安深渊时间表 Cron。
	 * Idian Depths schedule cron.
	 */
	@Property(key = "gameserver.idian.depths.schedule", defaultValue = "0 0 6 ? * *")
	public static String IDIAN_DEPTHS_SCHEDULE;
	/**
	 * 伊迪安深渊持续时间。
	 * Idian Depths duration.
	 */
	@Property(key = "gameserver.idian.depths.duration", defaultValue = "1")
	public static int IDIAN_DEPTHS_DURATION;

	/**
	 * 是否启用副本裂隙。
	 * Whether Instance Rift is enabled.
	 */
	@Property(key = "gameserver.instance.rift.enable", defaultValue = "true")
	public static boolean INSTANCE_RIFT_ENABLED;
	/**
	 * 副本裂隙持续时间。
	 * Instance Rift duration.
	 */
	@Property(key = "gameserver.instance.rift.duration", defaultValue = "24")
	public static int INSTANCE_RIFT_DURATION;

	/**
	 * 重连后是否传送到绑定点。
	 * Whether players are teleported to bind point after reconnect.
	 */
	@Property(key = "gameserver.reconnect.to.bind.point", defaultValue = "true")
	public static boolean ENABLE_RECONNECT_TO_BIND_POINT;

	/**
	 * 是否启用基地奖励。
	 * Whether base rewards are enabled.
	 */
	@Property(key = "gameserver.base.rewards.enable", defaultValue = "true")
	public static boolean ENABLE_BASE_REWARDS;

	/**
	 * 是否启用保护者/征服者系统。
	 * Whether Protector/Conqueror system is enabled.
	 */
	@Property(key = "gameserver.protector.conqueror.enable", defaultValue = "true")
	public static boolean PROTECTOR_CONQUEROR_ENABLE;
	/**
	 * 保护者/征服者生效的世界列表。
	 * Worlds handled by Protector/Conqueror system.
	 */
	@Property(key = "gameserver.protector.conqueror.handled.worlds", defaultValue = "")
	public static String PROTECTOR_CONQUEROR_WORLDS = "";
	/**
	 * 保护者/征服者击杀刷新间隔。
	 * Protector/Conqueror kills refresh interval.
	 */
	@Property(key = "gameserver.protector.conqueror.kills.refresh", defaultValue = "5")
	public static int PROTECTOR_CONQUEROR_REFRESH;
	/**
	 * 保护者/征服者击杀衰减量。
	 * Protector/Conqueror kills decrease amount.
	 */
	@Property(key = "gameserver.protector.conqueror.kills.decrease", defaultValue = "1")
	public static int PROTECTOR_CONQUEROR_DECREASE;
	/**
	 * 保护者/征服者等级差限制。
	 * Protector/Conqueror level difference limit.
	 */
	@Property(key = "gameserver.protector.conqueror.level.diff", defaultValue = "10")
	public static int PROTECTOR_CONQUEROR_LEVEL_DIFF;
	/**
	 * 保护者/征服者一阶击杀要求。
	 * Protector/Conqueror 1st rank kill requirement.
	 */
	@Property(key = "gameserver.protector.conqueror.1st.rank.kills", defaultValue = "5")
	public static int PROTECTOR_CONQUEROR_1ST_RANK_KILLS;
	/**
	 * 保护者/征服者二阶击杀要求。
	 * Protector/Conqueror 2nd rank kill requirement.
	 */
	@Property(key = "gameserver.protector.conqueror.2nd.rank.kills", defaultValue = "10")
	public static int PROTECTOR_CONQUEROR_2ND_RANK_KILLS;

	/**
	 * 神石触发率倍率。
	 * Godstone activation rate multiplier.
	 */
	@Property(key = "gameserver.rates.godstone.activation.rate", defaultValue = "1.0")
	public static float GODSTONE_ACTIVATION_RATE;

	/**
	 * 神石判定冷却时间（毫秒）。
	 * Godstone evaluation cooldown in milliseconds.
	 */
	@Property(key = "gameserver.rates.godstone.evaluation.cooldown_millis", defaultValue = "750")
	public static int GODSTONE_EVALUATION_COOLDOWN_MILLIS;

	/**
	 * 是否启用休憩能量。
	 * Whether Energy of Repose is enabled.
	 */
	@Property(key = "gameserver.energy.of.repose.enable", defaultValue = "true")
	public static boolean ENERGY_OF_REPOSE_ENABLE;

	/**
	 * 是否启用露娜币上限。
	 * Whether Luna currency cap is enabled.
	 */
	@Property(key = "gameserver.enable.luna.cap", defaultValue = "false")
	public static boolean ENABLE_LUNA_CAP;
	/**
	 * 露娜币上限数值。
	 * Luna currency cap value.
	 */
	@Property(key = "gameserver.luna.cap.value", defaultValue = "9999999")
	public static long LUNA_CAP_VALUE;

	/**
	 * 自定义 PK 标签格式。
	 * Custom PK tag format.
	 */
	@Property(key = "gameserver.pk.tag", defaultValue = "☠ %s")
	public static String TAG_PK;
	/**
	 * 自定义 PvE 标签格式。
	 * Custom PvE tag format.
	 */
	@Property(key = "gameserver.pve.tag", defaultValue = "⛨ %s")
	public static String TAG_PVE;

	/**
	 * 是否启用盗贼公会服务。
	 * Whether Thieves Guild service is enabled.
	 */
	@Property(key = "gameserver.thieves.guild.enable", defaultValue = "false")
	public static boolean THIEVES_ENABLE;

	/**
	 * 是否启用自动使用能量碎片。
	 * Whether auto powershard usage is enabled.
	 */
	@Property(key = "gameserver.enable.auto.powershard", defaultValue = "false")
	public static boolean ENABLE_AUTO_POWERSHARD;

	/**
	 * 禁用传送功能的 NPC ID 列表。
	 * NPC IDs for which teleporter/flight transporter is disabled.
	 */
	@Property(key = "gameserver.disable.teleport.npcs", defaultValue = "0")
	public static String DISABLE_TELEPORTER_NPCS;

	/**
	 * 新角色默认背包扩展格数。
	 * Default cube expansion size for a new character.
	 */
	@Property(key = "gameserver.cube.size", defaultValue = "0")
	public static int CUBE_SIZE;

	private final LoginserverProperties loginserver = new LoginserverProperties();
	private final PveProperties pve = new PveProperties();
	private final PkProperties pk = new PkProperties();
	private final WorldchannelProperties worldchannel = new WorldchannelProperties();
	private final FactionProperties faction = new FactionProperties();
	private final PremiumProperties premium = new PremiumProperties();
	private final EnchantCustomProperties enchant = new EnchantCustomProperties();
	private final ChatProperties chat = new ChatProperties();
	private final SearchProperties search = new SearchProperties();
	private final CrossProperties cross = new CrossProperties();
	private final SimpleProperties simple = new SimpleProperties();
	private final SkillProperties skill = new SkillProperties();
	private final AdminCustomProperties admin = new AdminCustomProperties();
	private final BaseProperties base = new BaseProperties();
	private final OldnamesProperties oldnames = new OldnamesProperties();
	private final FriendlistProperties friendlist = new FriendlistProperties();
	private final BasicProperties basic = new BasicProperties();
	private final InstancesProperties instances = new InstancesProperties();
	private final InstanceProperties instance = new InstanceProperties();
	private final EnableProperties enable = new EnableProperties();
	private final KinahProperties kinah = new KinahProperties();
	private final LunaProperties luna = new LunaProperties();
	private final NoapProperties noap = new NoapProperties();
	private final DialogProperties dialog = new DialogProperties();
	private final RewardProperties reward = new RewardProperties();
	private final LimitsProperties limits = new LimitsProperties();
	private final AbyssxformProperties abyssxform = new AbyssxformProperties();
	private final RideProperties ride = new RideProperties();
	private final ChallengeProperties challenge = new ChallengeProperties();
	private final CommandsProperties commands = new CommandsProperties();
	private final ExpressProperties express = new ExpressProperties();
	private final RiftProperties rift = new RiftProperties();
	private final VortexProperties vortex = new VortexProperties();
	private final DisputeProperties dispute = new DisputeProperties();
	private final BeritraProperties beritra = new BeritraProperties();
	private final AgentProperties agent = new AgentProperties();
	private final AnohaProperties anoha = new AnohaProperties();
	private final SvsProperties svs = new SvsProperties();
	private final RvrProperties rvr = new RvrProperties();
	private final MoltenusProperties moltenus = new MoltenusProperties();
	private final DynamicProperties dynamic = new DynamicProperties();
	private final ShugoProperties shugo = new ShugoProperties();
	private final TowerProperties tower = new TowerProperties();
	private final ZorshivProperties zorshiv = new ZorshivProperties();
	private final IuProperties iu = new IuProperties();
	private final NightmareProperties nightmare = new NightmareProperties();
	private final ConquestProperties conquest = new ConquestProperties();
	private final IdianProperties idian = new IdianProperties();
	private final ReconnectProperties reconnect = new ReconnectProperties();
	private final ProtectorProperties protector = new ProtectorProperties();
	private final RatesProperties rates = new RatesProperties();
	private final EnergyProperties energy = new EnergyProperties();
	private final ThievesProperties thieves = new ThievesProperties();
	private final DisableProperties disable = new DisableProperties();
	private final CubeProperties cube = new CubeProperties();

	public static class LoginserverProperties {
		public boolean isInfo() {
			return LOGIN_SERVER_INFO;
		}

		public void setInfo(boolean info) {
			LOGIN_SERVER_INFO = info;
		}
	}

	@Getter
	public static class PveProperties {
		private final PveTollProperties toll = new PveTollProperties();

		public String getTag() {
			return TAG_PVE;
		}

		public void setTag(String tag) {
			TAG_PVE = tag;
		}

		@Getter
		public static class PveTollProperties {
			private final RewardingProperties rewarding = new RewardingProperties();
			private final RewardProperties reward = new RewardProperties();

			public static class RewardingProperties {
				public boolean isEnable() {
					return ENABLE_PVE_TOLL_REWARD;
				}

				public void setEnable(boolean enable) {
					ENABLE_PVE_TOLL_REWARD = enable;
				}
			}

			public static class RewardProperties {
				public int getChance() {
					return TOLL_PVE_CHANCE;
				}

				public void setChance(int chance) {
					TOLL_PVE_CHANCE = chance;
				}

				public int getQuantity() {
					return TOLL_PVE_QUANTITY;
				}

				public void setQuantity(int quantity) {
					TOLL_PVE_QUANTITY = quantity;
				}

				public String getWorldid() {
					return TOLL_PVE_WORLDID;
				}

				public void setWorldid(String worldid) {
					TOLL_PVE_WORLDID = worldid;
				}
			}
		}
	}

	public static class PkProperties {
		public String getTag() {
			return TAG_PK;
		}

		public void setTag(String tag) {
			TAG_PK = tag;
		}
	}

	public static class WorldchannelProperties {
		public int getCosts() {
			return WORLD_CHANNEL_AP_COSTS;
		}

		public void setCosts(int costs) {
			WORLD_CHANNEL_AP_COSTS = costs;
		}
	}

	public static class FactionProperties {
		public boolean isFree() {
			return FACTION_FREE_USE;
		}

		public void setFree(boolean free) {
			FACTION_FREE_USE = free;
		}

		public int getPrices() {
			return FACTION_USE_PRICE;
		}

		public void setPrices(int prices) {
			FACTION_USE_PRICE = prices;
		}

		public boolean isCmdchannel() {
			return FACTION_CMD_CHANNEL;
		}

		public void setCmdchannel(boolean cmdchannel) {
			FACTION_CMD_CHANNEL = cmdchannel;
		}

		public boolean isChatchannels() {
			return FACTION_CHAT_CHANNEL;
		}

		public void setChatchannels(boolean chatchannels) {
			FACTION_CHAT_CHANNEL = chatchannels;
		}
	}

	@Getter
	public static class PremiumProperties {
		private final NotifyProperties notify = new NotifyProperties();

		public static class NotifyProperties {
			public boolean isEnable() {
				return PREMIUM_NOTIFY_ENABLE;
			}

			public void setEnable(boolean enable) {
				PREMIUM_NOTIFY_ENABLE = enable;
			}
		}
	}

	@Getter
	public static class EnchantCustomProperties {
		private final AnnounceProperties announce = new AnnounceProperties();

		public static class AnnounceProperties {
			public boolean isEnable() {
				return ENABLE_ENCHANT_ANNOUNCE;
			}

			public void setEnable(boolean enable) {
				ENABLE_ENCHANT_ANNOUNCE = enable;
			}
		}
	}

	@Getter
	public static class ChatProperties {
		private final FactionsProperties factions = new FactionsProperties();
		private final WhisperProperties whisper = new WhisperProperties();
		private final TextProperties text = new TextProperties();

		public static class FactionsProperties {
			public boolean isEnable() {
				return SPEAKING_BETWEEN_FACTIONS;
			}

			public void setEnable(boolean enable) {
				SPEAKING_BETWEEN_FACTIONS = enable;
			}
		}

		public static class WhisperProperties {
			public int getLevel() {
				return LEVEL_TO_WHISPER;
			}

			public void setLevel(int level) {
				LEVEL_TO_WHISPER = level;
			}
		}

		public static class TextProperties {
			public int getLength() {
				return MAX_CHAT_TEXT_LENGHT;
			}

			public void setLength(int length) {
				MAX_CHAT_TEXT_LENGHT = length;
			}
		}
	}

	@Getter
	public static class SearchProperties {
		private final FactionsProperties factions = new FactionsProperties();
		private final GmProperties gm = new GmProperties();

		public static class FactionsProperties {
			public boolean isMode() {
				return FACTIONS_SEARCH_MODE;
			}

			public void setMode(boolean mode) {
				FACTIONS_SEARCH_MODE = mode;
			}
		}

		public static class GmProperties {
			public boolean isList() {
				return SEARCH_GM_LIST;
			}

			public void setList(boolean list) {
				SEARCH_GM_LIST = list;
			}
		}
	}

	@Getter
	public static class CrossProperties {
		private final CrossFactionProperties faction = new CrossFactionProperties();

		public static class CrossFactionProperties {
			public boolean isBinding() {
				return ENABLE_CROSS_FACTION_BINDING;
			}

			public void setBinding(boolean binding) {
				ENABLE_CROSS_FACTION_BINDING = binding;
			}
		}
	}

	@Getter
	public static class SimpleProperties {
		private final SecondclassProperties secondclass = new SecondclassProperties();

		public static class SecondclassProperties {
			public boolean isEnable() {
				return ENABLE_SIMPLE_2NDCLASS;
			}

			public void setEnable(boolean enable) {
				ENABLE_SIMPLE_2NDCLASS = enable;
			}
		}
	}

	@Getter
	public static class SkillProperties {
		private final ChainProperties chain = new ChainProperties();

		public static class ChainProperties {
			public boolean isTriggerrate() {
				return SKILL_CHAIN_TRIGGERRATE;
			}

			public void setTriggerrate(boolean triggerrate) {
				SKILL_CHAIN_TRIGGERRATE = triggerrate;
			}
		}
	}

	@Getter
	public static class AdminCustomProperties {
		private final DyeProperties dye = new DyeProperties();

		public static class DyeProperties {
			public int getPrice() {
				return DYE_PRICE;
			}

			public void setPrice(int price) {
				DYE_PRICE = price;
			}
		}
	}

	@Getter
	public static class BaseProperties {
		private final RewardsProperties rewards = new RewardsProperties();

		public int getFlytime() {
			return BASE_FLYTIME;
		}

		public void setFlytime(int flytime) {
			BASE_FLYTIME = flytime;
		}

		public static class RewardsProperties {
			public boolean isEnable() {
				return ENABLE_BASE_REWARDS;
			}

			public void setEnable(boolean enable) {
				ENABLE_BASE_REWARDS = enable;
			}
		}
	}

	@Getter
	public static class OldnamesProperties {
		private final CouponProperties coupon = new CouponProperties();
		private final CommandProperties command = new CommandProperties();

		public static class CouponProperties {
			public boolean isDisable() {
				return OLD_NAMES_COUPON_DISABLED;
			}

			public void setDisable(boolean disable) {
				OLD_NAMES_COUPON_DISABLED = disable;
			}
		}

		public static class CommandProperties {
			public boolean isDisable() {
				return OLD_NAMES_COMMAND_DISABLED;
			}

			public void setDisable(boolean disable) {
				OLD_NAMES_COMMAND_DISABLED = disable;
			}
		}
	}

	public static class FriendlistProperties {
		public int getSize() {
			return FRIENDLIST_SIZE;
		}

		public void setSize(int size) {
			FRIENDLIST_SIZE = size;
		}
	}

	@Getter
	public static class BasicProperties {
		private final QuestsizeProperties questsize = new QuestsizeProperties();

		public static class QuestsizeProperties {
			public int getLimit() {
				return BASIC_QUEST_SIZE_LIMIT;
			}

			public void setLimit(int limit) {
				BASIC_QUEST_SIZE_LIMIT = limit;
			}
		}
	}

	@Getter
	public static class InstancesProperties {

		public boolean isEnable() {
			return ENABLE_INSTANCES;
		}

		public void setEnable(boolean enable) {
			ENABLE_INSTANCES = enable;
		}
	}

	@Getter
	public static class InstanceProperties {
		private final DuelProperties duel = new DuelProperties();
		private final InstanceRiftProperties rift = new InstanceRiftProperties();

		public static class DuelProperties {
			public boolean isEnable() {
				return INSTANCE_DUEL_ENABLE;
			}

			public void setEnable(boolean enable) {
				INSTANCE_DUEL_ENABLE = enable;
			}
		}

		public static class InstanceRiftProperties {
			public boolean isEnable() {
				return INSTANCE_RIFT_ENABLED;
			}

			public void setEnable(boolean enable) {
				INSTANCE_RIFT_ENABLED = enable;
			}

			public int getDuration() {
				return INSTANCE_RIFT_DURATION;
			}

			public void setDuration(int duration) {
				INSTANCE_RIFT_DURATION = duration;
			}
		}
	}

	@Getter
	public static class EnableProperties {
		private final KinahCapProperties kinah = new KinahCapProperties();
		private final LunaCapProperties luna = new LunaCapProperties();
		private final AutoProperties auto = new AutoProperties();
		private final ExpProperties exp = new ExpProperties();

		public static class KinahCapProperties {
			public boolean isCap() {
				return ENABLE_KINAH_CAP;
			}

			public void setCap(boolean cap) {
				ENABLE_KINAH_CAP = cap;
			}
		}

		public static class LunaCapProperties {
			public boolean isCap() {
				return ENABLE_LUNA_CAP;
			}

			public void setCap(boolean cap) {
				ENABLE_LUNA_CAP = cap;
			}
		}

		public static class AutoProperties {
			public boolean isPowershard() {
				return ENABLE_AUTO_POWERSHARD;
			}

			public void setPowershard(boolean powershard) {
				ENABLE_AUTO_POWERSHARD = powershard;
			}
		}

		@Getter
		public static class ExpProperties {
			private final ProgressiveProperties progressive = new ProgressiveProperties();

			@Getter
			public static class ProgressiveProperties {
				private final ApProperties ap = new ApProperties();
				private final GroupProperties group = new GroupProperties();

				public boolean isHunting() {
					return ENABLE_EXP_PROGRESSIVE_HUNTING;
				}

				public void setHunting(boolean hunting) {
					ENABLE_EXP_PROGRESSIVE_HUNTING = hunting;
				}

				public boolean isQuest() {
					return ENABLE_EXP_PROGRESSIVE_QUEST;
				}

				public void setQuest(boolean quest) {
					ENABLE_EXP_PROGRESSIVE_QUEST = quest;
				}

				public boolean isBook() {
					return ENABLE_EXP_PROGRESSIVE_BOOK;
				}

				public void setBook(boolean book) {
					ENABLE_EXP_PROGRESSIVE_BOOK = book;
				}

				public static class ApProperties {
					public boolean isPlayer() {
						return ENABLE_EXP_PROGRESSIVE_AP_PLAYER;
					}

					public void setPlayer(boolean player) {
						ENABLE_EXP_PROGRESSIVE_AP_PLAYER = player;
					}

					public boolean isNpc() {
						return ENABLE_EXP_PROGRESSIVE_AP_NPC;
					}

					public void setNpc(boolean npc) {
						ENABLE_EXP_PROGRESSIVE_AP_NPC = npc;
					}
				}

				public static class GroupProperties {
					public boolean isHunting() {
						return ENABLE_EXP_PROGRESSIVE_GROUP_HUNTING;
					}

					public void setHunting(boolean hunting) {
						ENABLE_EXP_PROGRESSIVE_GROUP_HUNTING = hunting;
					}
				}
			}
		}
	}

	@Getter
	public static class KinahProperties {
		private final CapProperties cap = new CapProperties();

		public static class CapProperties {
			public long getValue() {
				return KINAH_CAP_VALUE;
			}

			public void setValue(long value) {
				KINAH_CAP_VALUE = value;
			}
		}
	}

	@Getter
	public static class LunaProperties {
		private final CapProperties cap = new CapProperties();

		public static class CapProperties {
			public long getValue() {
				return LUNA_CAP_VALUE;
			}

			public void setValue(long value) {
				LUNA_CAP_VALUE = value;
			}
		}
	}

	@Getter
	public static class NoapProperties {
		private final MentorProperties mentor = new MentorProperties();

		public static class MentorProperties {
			public boolean isGroup() {
				return MENTOR_GROUP_AP;
			}

			public void setGroup(boolean group) {
				MENTOR_GROUP_AP = group;
			}
		}
	}

	@Getter
	public static class DialogProperties {
		private final ShowProperties show = new ShowProperties();

		public static class ShowProperties {
			public boolean isId() {
				return ENABLE_SHOW_DIALOG_ID;
			}

			public void setId(boolean id) {
				ENABLE_SHOW_DIALOG_ID = id;
			}
		}
	}

	@Getter
	public static class RewardProperties {
		private final ServiceProperties service = new ServiceProperties();

		public static class ServiceProperties {
			public boolean isEnable() {
				return ENABLE_REWARD_SERVICE;
			}

			public void setEnable(boolean enable) {
				ENABLE_REWARD_SERVICE = enable;
			}
		}
	}

	public static class LimitsProperties {
		public boolean isEnable() {
			return LIMITS_ENABLED;
		}

		public void setEnable(boolean enable) {
			LIMITS_ENABLED = enable;
		}

		public String getUpdate() {
			return LIMITS_UPDATE;
		}

		public void setUpdate(String update) {
			LIMITS_UPDATE = update;
		}

		public int getRate() {
			return LIMITS_RATE;
		}

		public void setRate(int rate) {
			LIMITS_RATE = rate;
		}
	}

	public static class AbyssxformProperties {
		public boolean isAfterlogout() {
			return ABYSSXFORM_LOGOUT;
		}

		public void setAfterlogout(boolean afterlogout) {
			ABYSSXFORM_LOGOUT = afterlogout;
		}
	}

	@Getter
	public static class RideProperties {
		private final RestrictionProperties restriction = new RestrictionProperties();

		public static class RestrictionProperties {
			public boolean isEnable() {
				return ENABLE_RIDE_RESTRICTION;
			}

			public void setEnable(boolean enable) {
				ENABLE_RIDE_RESTRICTION = enable;
			}
		}
	}

	@Getter
	public static class ChallengeProperties {
		private final TasksProperties tasks = new TasksProperties();

		public static class TasksProperties {
			public boolean isEnabled() {
				return CHALLENGE_TASKS_ENABLED;
			}

			public void setEnabled(boolean enabled) {
				CHALLENGE_TASKS_ENABLED = enabled;
			}
		}
	}

	@Getter
	public static class CommandsProperties {
		private final AdminDotProperties admin = new AdminDotProperties();

		@Getter
		public static class AdminDotProperties {
			private final DotProperties dot = new DotProperties();

			public static class DotProperties {
				public boolean isEnable() {
					return ENABLE_ADMIN_DOT_COMMANDS;
				}

				public void setEnable(boolean enable) {
					ENABLE_ADMIN_DOT_COMMANDS = enable;
				}
			}
		}
	}

	@Getter
	public static class ExpressProperties {
		private final MailProperties mail = new MailProperties();

		public static class MailProperties {
			public int getCooldown_seconds() {
				return EXPRESS_MAIL_COOLDOWN_SECONDS;
			}

			public void setCooldown_seconds(int cooldownSeconds) {
				EXPRESS_MAIL_COOLDOWN_SECONDS = cooldownSeconds;
			}
		}
	}

	public static class RiftProperties {
		public boolean isEnable() {
			return RIFT_ENABLED;
		}

		public void setEnable(boolean enable) {
			RIFT_ENABLED = enable;
		}

		public int getDuration() {
			return RIFT_DURATION;
		}

		public void setDuration(int duration) {
			RIFT_DURATION = duration;
		}
	}

	public static class VortexProperties {
		public boolean isEnable() {
			return VORTEX_ENABLED;
		}

		public void setEnable(boolean enable) {
			VORTEX_ENABLED = enable;
		}

		public int getDuration() {
			return VORTEX_DURATION;
		}

		public void setDuration(int duration) {
			VORTEX_DURATION = duration;
		}
	}

	@Getter
	public static class DisputeProperties {
		private final LandProperties land = new LandProperties();

		public static class LandProperties {
			public boolean isEnable() {
				return DISPUTE_LAND_ENABLED;
			}

			public void setEnable(boolean enable) {
				DISPUTE_LAND_ENABLED = enable;
			}

			public String getSchedule() {
				return DISPUTE_LAND_SCHEDULE;
			}

			public void setSchedule(String schedule) {
				DISPUTE_LAND_SCHEDULE = schedule;
			}

			public int getDuration() {
				return DISPUTE_LAND_DURATION;
			}

			public void setDuration(int duration) {
				DISPUTE_LAND_DURATION = duration;
			}
		}
	}

	public static class BeritraProperties {
		public boolean isEnable() {
			return BERITRA_ENABLED;
		}

		public void setEnable(boolean enable) {
			BERITRA_ENABLED = enable;
		}

		public int getDuration() {
			return BERITRA_DURATION;
		}

		public void setDuration(int duration) {
			BERITRA_DURATION = duration;
		}
	}

	public static class AgentProperties {
		public boolean isEnable() {
			return AGENT_ENABLED;
		}

		public void setEnable(boolean enable) {
			AGENT_ENABLED = enable;
		}

		public int getDuration() {
			return AGENT_DURATION;
		}

		public void setDuration(int duration) {
			AGENT_DURATION = duration;
		}
	}

	public static class AnohaProperties {
		public boolean isEnable() {
			return ANOHA_ENABLED;
		}

		public void setEnable(boolean enable) {
			ANOHA_ENABLED = enable;
		}

		public int getDuration() {
			return ANOHA_DURATION;
		}

		public void setDuration(int duration) {
			ANOHA_DURATION = duration;
		}
	}

	public static class SvsProperties {
		public boolean isEnable() {
			return SVS_ENABLED;
		}

		public void setEnable(boolean enable) {
			SVS_ENABLED = enable;
		}

		public int getDuration() {
			return SVS_DURATION;
		}

		public void setDuration(int duration) {
			SVS_DURATION = duration;
		}
	}

	public static class RvrProperties {
		public boolean isEnable() {
			return RVR_ENABLED;
		}

		public void setEnable(boolean enable) {
			RVR_ENABLED = enable;
		}

		public int getDuration() {
			return RVR_DURATION;
		}

		public void setDuration(int duration) {
			RVR_DURATION = duration;
		}
	}

	public static class MoltenusProperties {
		public boolean isEnable() {
			return MOLTENUS_ENABLED;
		}

		public void setEnable(boolean enable) {
			MOLTENUS_ENABLED = enable;
		}

		public int getDuration() {
			return MOLTENUS_DURATION;
		}

		public void setDuration(int duration) {
			MOLTENUS_DURATION = duration;
		}
	}

	@Getter
	public static class DynamicProperties {
		private final RiftSubProperties rift = new RiftSubProperties();

		@Getter
		public static class RiftSubProperties {
			private final DragonProperties dragon = new DragonProperties();
			private final IndratooProperties indratoo = new IndratooProperties();

			public boolean isEnable() {
				return DYNAMIC_RIFT_ENABLED;
			}

			public void setEnable(boolean enable) {
				DYNAMIC_RIFT_ENABLED = enable;
			}

			public int getDuration() {
				return DYNAMIC_RIFT_DURATION;
			}

			public void setDuration(int duration) {
				DYNAMIC_RIFT_DURATION = duration;
			}

			public static class DragonProperties {
				public String getSchedule() {
					return DYNAMIC_RIFT_DRAGON_SCHEDULE;
				}

				public void setSchedule(String schedule) {
					DYNAMIC_RIFT_DRAGON_SCHEDULE = schedule;
				}
			}

			public static class IndratooProperties {
				public String getSchedule() {
					return DYNAMIC_RIFT_INDRATOO_SCHEDULE;
				}

				public void setSchedule(String schedule) {
					DYNAMIC_RIFT_INDRATOO_SCHEDULE = schedule;
				}
			}
		}
	}

	@Getter
	public static class ShugoProperties {
		private final MerchantProperties merchant = new MerchantProperties();

		@Getter
		public static class MerchantProperties {
			private final LeagueProperties league = new LeagueProperties();

			public static class LeagueProperties {
				public String getSchedule() {
					return SHUGO_MERCHANT_LEAGUE_SCHEDULE;
				}

				public void setSchedule(String schedule) {
					SHUGO_MERCHANT_LEAGUE_SCHEDULE = schedule;
				}
			}
		}
	}

	@Getter
	public static class TowerProperties {
		private final OfProperties of = new OfProperties();

		@Getter
		public static class OfProperties {
			private final EternityProperties eternity = new EternityProperties();

			public static class EternityProperties {
				public boolean isEnable() {
					return TOWER_OF_ETERNITY_ENABLED;
				}

				public void setEnable(boolean enable) {
					TOWER_OF_ETERNITY_ENABLED = enable;
				}

				public String getSchedule() {
					return TOWER_OF_ETERNITY_SCHEDULE;
				}

				public void setSchedule(String schedule) {
					TOWER_OF_ETERNITY_SCHEDULE = schedule;
				}

				public int getDuration() {
					return TOWER_OF_ETERNITY_DURATION;
				}

				public void setDuration(int duration) {
					TOWER_OF_ETERNITY_DURATION = duration;
				}
			}
		}
	}

	@Getter
	public static class ZorshivProperties {
		private final DredgionProperties dredgion = new DredgionProperties();

		public static class DredgionProperties {
			public boolean isEnable() {
				return ZORSHIV_DREDGION_ENABLED;
			}

			public void setEnable(boolean enable) {
				ZORSHIV_DREDGION_ENABLED = enable;
			}

			public int getDuration() {
				return ZORSHIV_DREDGION_DURATION;
			}

			public void setDuration(int duration) {
				ZORSHIV_DREDGION_DURATION = duration;
			}
		}
	}

	public static class IuProperties {
		public boolean isEnable() {
			return IU_ENABLED;
		}

		public void setEnable(boolean enable) {
			IU_ENABLED = enable;
		}

		public String getSchedule() {
			return IU_SCHEDULE;
		}

		public void setSchedule(String schedule) {
			IU_SCHEDULE = schedule;
		}

		public int getDuration() {
			return IU_DURATION;
		}

		public void setDuration(int duration) {
			IU_DURATION = duration;
		}
	}

	@Getter
	public static class NightmareProperties {
		private final CircusProperties circus = new CircusProperties();

		public static class CircusProperties {
			public boolean isEnable() {
				return NIGHTMARE_CIRCUS_ENABLE;
			}

			public void setEnable(boolean enable) {
				NIGHTMARE_CIRCUS_ENABLE = enable;
			}

			public int getDuration() {
				return NIGHTMARE_CIRCUS_DURATION;
			}

			public void setDuration(int duration) {
				NIGHTMARE_CIRCUS_DURATION = duration;
			}
		}
	}

	public static class ConquestProperties {
		public boolean isEnable() {
			return CONQUEST_ENABLED;
		}

		public void setEnable(boolean enable) {
			CONQUEST_ENABLED = enable;
		}

		public int getDuration() {
			return CONQUEST_DURATION;
		}

		public void setDuration(int duration) {
			CONQUEST_DURATION = duration;
		}
	}

	@Getter
	public static class IdianProperties {
		private final DepthsProperties depths = new DepthsProperties();

		public static class DepthsProperties {
			public boolean isEnable() {
				return IDIAN_DEPTHS_ENABLED;
			}

			public void setEnable(boolean enable) {
				IDIAN_DEPTHS_ENABLED = enable;
			}

			public String getSchedule() {
				return IDIAN_DEPTHS_SCHEDULE;
			}

			public void setSchedule(String schedule) {
				IDIAN_DEPTHS_SCHEDULE = schedule;
			}

			public int getDuration() {
				return IDIAN_DEPTHS_DURATION;
			}

			public void setDuration(int duration) {
				IDIAN_DEPTHS_DURATION = duration;
			}
		}
	}

	@Getter
	public static class ReconnectProperties {
		private final ToProperties to = new ToProperties();

		@Getter
		public static class ToProperties {
			private final BindProperties bind = new BindProperties();

			public static class BindProperties {
				public boolean isPoint() {
					return ENABLE_RECONNECT_TO_BIND_POINT;
				}

				public void setPoint(boolean point) {
					ENABLE_RECONNECT_TO_BIND_POINT = point;
				}
			}
		}
	}

	@Getter
	public static class ProtectorProperties {
		private final ConquerorProperties conqueror = new ConquerorProperties();

		@Getter
		public static class ConquerorProperties {
			private final HandledProperties handled = new HandledProperties();
			private final KillsProperties kills = new KillsProperties();
			private final LevelProperties level = new LevelProperties();
			private final FirstRankProperties first = new FirstRankProperties();
			private final SecondRankProperties second = new SecondRankProperties();

			public boolean isEnable() {
				return PROTECTOR_CONQUEROR_ENABLE;
			}

			public void setEnable(boolean enable) {
				PROTECTOR_CONQUEROR_ENABLE = enable;
			}

			public FirstRankProperties get1st() {
				return first;
			}

			public SecondRankProperties get2nd() {
				return second;
			}

			public static class HandledProperties {
				public String getWorlds() {
					return PROTECTOR_CONQUEROR_WORLDS;
				}

				public void setWorlds(String worlds) {
					PROTECTOR_CONQUEROR_WORLDS = worlds;
				}
			}

			public static class KillsProperties {
				public int getRefresh() {
					return PROTECTOR_CONQUEROR_REFRESH;
				}

				public void setRefresh(int refresh) {
					PROTECTOR_CONQUEROR_REFRESH = refresh;
				}

				public int getDecrease() {
					return PROTECTOR_CONQUEROR_DECREASE;
				}

				public void setDecrease(int decrease) {
					PROTECTOR_CONQUEROR_DECREASE = decrease;
				}
			}

			public static class LevelProperties {
				public int getDiff() {
					return PROTECTOR_CONQUEROR_LEVEL_DIFF;
				}

				public void setDiff(int diff) {
					PROTECTOR_CONQUEROR_LEVEL_DIFF = diff;
				}
			}

			@Getter
			public static class FirstRankProperties {
				private final RankProperties rank = new RankProperties();

				public static class RankProperties {
					public int getKills() {
						return PROTECTOR_CONQUEROR_1ST_RANK_KILLS;
					}

					public void setKills(int kills) {
						PROTECTOR_CONQUEROR_1ST_RANK_KILLS = kills;
					}
				}
			}

			@Getter
			public static class SecondRankProperties {
				private final RankProperties rank = new RankProperties();

				public static class RankProperties {
					public int getKills() {
						return PROTECTOR_CONQUEROR_2ND_RANK_KILLS;
					}

					public void setKills(int kills) {
						PROTECTOR_CONQUEROR_2ND_RANK_KILLS = kills;
					}
				}
			}
		}
	}

	@Getter
	public static class RatesProperties {
		private final GodstoneProperties godstone = new GodstoneProperties();

		@Getter
		public static class GodstoneProperties {
			private final ActivationProperties activation = new ActivationProperties();
			private final EvaluationProperties evaluation = new EvaluationProperties();

			public static class ActivationProperties {
				public float getRate() {
					return GODSTONE_ACTIVATION_RATE;
				}

				public void setRate(float rate) {
					GODSTONE_ACTIVATION_RATE = rate;
				}
			}

			public static class EvaluationProperties {
				public int getCooldown_millis() {
					return GODSTONE_EVALUATION_COOLDOWN_MILLIS;
				}

				public void setCooldown_millis(int cooldownMillis) {
					GODSTONE_EVALUATION_COOLDOWN_MILLIS = cooldownMillis;
				}
			}
		}
	}

	@Getter
	public static class EnergyProperties {
		private final OfProperties of = new OfProperties();

		@Getter
		public static class OfProperties {
			private final ReposeProperties repose = new ReposeProperties();

			public static class ReposeProperties {
				public boolean isEnable() {
					return ENERGY_OF_REPOSE_ENABLE;
				}

				public void setEnable(boolean enable) {
					ENERGY_OF_REPOSE_ENABLE = enable;
				}
			}
		}
	}

	@Getter
	public static class ThievesProperties {
		private final GuildProperties guild = new GuildProperties();

		public static class GuildProperties {
			public boolean isEnable() {
				return THIEVES_ENABLE;
			}

			public void setEnable(boolean enable) {
				THIEVES_ENABLE = enable;
			}
		}
	}

	@Getter
	public static class DisableProperties {
		private final TeleportProperties teleport = new TeleportProperties();

		public static class TeleportProperties {
			public String getNpcs() {
				return DISABLE_TELEPORTER_NPCS;
			}

			public void setNpcs(String npcs) {
				DISABLE_TELEPORTER_NPCS = npcs;
			}
		}
	}

	public static class CubeProperties {
		public int getSize() {
			return CUBE_SIZE;
		}

		public void setSize(int size) {
			CUBE_SIZE = size;
		}
	}
}
