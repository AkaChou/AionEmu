package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 自定义功能与世界事件相关配置。
 * Custom features and world event related configuration.
 */
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
	 * 启用怪物仇恨的副本地图 ID 列表。
	 * Instance map IDs with mob aggro enabled.
	 */
	@Property(key = "gameserver.instances.mob.aggro", defaultValue = "300030000,300040000,300050000,300060000,300070000,300080000,300090000,300100000,300110000,300120000,300130000,300140000,300150000,300160000,300170000,300190000,300200000,300210000,300220000,300230000,300240000,300250000,300270000,300280000,300300000,300310000,300320000,300380000,300440000,300460000,300470000,300510000,300520000,300530000,300540000,300560000,300580000,300590000,300600000,300700000,300800000,300900000,301010000,301020000,301030000,301040000,301050000,301110000,301120000,301130000,301140000,301170000,301180000,301190000,301210000,310050000,310080000,310090000,310100000,310110000,320050000,320080000,320090000,320100000,320110000,320120000,320130000,320150000")
	public static String INSTANCES_MOB_AGGRO;
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

	@Property(key = "gameserver.rates.godstone.activation.rate", defaultValue = "1.0")
	public static float GODSTONE_ACTIVATION_RATE;

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
	@Property(key = "gameserver.pk.tag", defaultValue = "\u2620 %s")
	public static String TAG_PK;
	/**
	 * 自定义 PvE 标签格式。
	 * Custom PvE tag format.
	 */
	@Property(key = "gameserver.pve.tag", defaultValue = "\u26E8 %s")
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
}
