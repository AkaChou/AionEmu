package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 安全防护、反外挂与验证相关配置。
 * Security, anti-hack and validation related configuration.
 */
public class SecurityConfig {
	/**
	 * 是否广播 GM 审计消息。
	 * Whether GM audit messages are broadcast.
	 */
	@Property(key = "gameserver.security.gmaudit.message.broadcast", defaultValue = "false")
	public static boolean GM_AUDIT_MESSAGE_BROADCAST;
	/**
	 * 是否检测异常隐身。
	 * Whether anti-hack invisibility check is enabled.
	 */
	@Property(key = "gameserver.security.antihack.invis", defaultValue = "false")
	public static boolean INVIS;
	/**
	 * 是否检测异常瞬移。
	 * Whether anti-hack teleportation check is enabled.
	 */
	@Property(key = "gameserver.security.antihack.teleportation", defaultValue = "false")
	public static boolean TELEPORTATION;
	/**
	 * 是否检测加速外挂。
	 * Whether anti-hack speedhack check is enabled.
	 */
	@Property(key = "gameserver.security.antihack.speedhack", defaultValue = "false")
	public static boolean SPEEDHACK;
	/**
	 * 加速外挂触发计数阈值。
	 * Speedhack detection counter threshold.
	 */
	@Property(key = "gameserver.security.antihack.speedhack.counter", defaultValue = "1")
	public static int SPEEDHACK_COUNTER;
	/**
	 * 是否检测异常状态。
	 * Whether anti-hack abnormal state check is enabled.
	 */
	@Property(key = "gameserver.security.antihack.abnormal", defaultValue = "false")
	public static boolean ABNORMAL;
	/**
	 * 异常状态触发计数阈值。
	 * Abnormal state detection counter threshold.
	 */
	@Property(key = "gameserver.security.antihack.abnormal.counter", defaultValue = "1")
	public static int ABNORMAL_COUNTER;
	/**
	 * 反外挂惩罚类型。
	 * Anti-hack punishment type.
	 */
	@Property(key = "gameserver.security.antihack.punish", defaultValue = "0")
	public static int PUNISH;
	/**
	 * 是否检测无动画技能。
	 * Whether no-animation skill check is enabled.
	 */
	@Property(key = "gameserver.security.noanimation", defaultValue = "false")
	public static boolean NO_ANIMATION;
	/**
	 * 无动画检测是否踢下线。
	 * Whether no-animation detection kicks the player.
	 */
	@Property(key = "gameserver.security.noanimation.kick", defaultValue = "false")
	public static boolean NO_ANIMATION_KICK;
	/**
	 * 无动画检测阈值。
	 * No-animation detection threshold value.
	 */
	@Property(key = "gameserver.security.noanimation.value", defaultValue = "0.1")
	public static float NO_ANIMATION_VALUE;
	/**
	 * 是否启用动作时间校验。
	 * Whether motion time validation is enabled.
	 */
	@Property(key = "gameserver.security.motion.time.enable", defaultValue = "true")
	public static boolean MOTION_TIME;
	/**
	 * 是否启用验证码。
	 * Whether captcha is enabled.
	 */
	@Property(key = "gameserver.security.captcha.enable", defaultValue = "false")
	public static boolean CAPTCHA_ENABLE;
	/**
	 * 验证码出现场景。
	 * Captcha appear condition/scene.
	 */
	@Property(key = "gameserver.security.captcha.appear", defaultValue = "OD")
	public static String CAPTCHA_APPEAR;
	/**
	 * 验证码出现概率。
	 * Captcha appear rate.
	 */
	@Property(key = "gameserver.security.captcha.appear.rate", defaultValue = "5")
	public static int CAPTCHA_APPEAR_RATE;
	/**
	 * 验证码失败后的采集封禁时长。
	 * Extraction ban time after captcha failure.
	 */
	@Property(key = "gameserver.security.captcha.extraction.ban.time", defaultValue = "3000")
	public static int CAPTCHA_EXTRACTION_BAN_TIME;
	/**
	 * 验证码失败额外增加的封禁时长。
	 * Additional extraction ban time after captcha failure.
	 */
	@Property(key = "gameserver.security.captcha.extraction.ban.add.time", defaultValue = "600")
	public static int CAPTCHA_EXTRACTION_BAN_ADD_TIME;
	/**
	 * 验证码成功奖励的飞行点时间。
	 * Bonus flight point time for successful captcha.
	 */
	@Property(key = "gameserver.security.captcha.bonus.fp.time", defaultValue = "5")
	public static int CAPTCHA_BONUS_FP_TIME;
	/**
	 * 是否启用二级密码。
	 * Whether passkey is enabled.
	 */
	@Property(key = "gameserver.security.passkey.enable", defaultValue = "false")
	public static boolean PASSKEY_ENABLE;
	/**
	 * 二级密码最大错误次数。
	 * Maximum wrong passkey attempts.
	 */
	@Property(key = "gameserver.security.passkey.wrong.maxcount", defaultValue = "5")
	public static int PASSKEY_WRONG_MAXCOUNT;
	/**
	 * 是否启用 Ping 存活检测。
	 * Whether ping check is enabled.
	 */
	@Property(key = "gameserver.security.pingcheck.enable", defaultValue = "true")
	public static boolean SECURITY_ENABLE;
	/**
	 * Ping 检测间隔。
	 * Ping check interval.
	 */
	@Property(key = "gameserver.security.pingcheck.interval", defaultValue = "80")
	public static int PING_INTERVAL;
	/**
	 * 聊天洪水限制延迟（秒）。
	 * Chat flood delay in seconds.
	 */
	@Property(key = "gameserver.security.flood.delay", defaultValue = "1")
	public static int FLOOD_DELAY;
	/**
	 * 聊天洪水限制消息数。
	 * Chat flood message count limit.
	 */
	@Property(key = "gameserver.security.flood.msg", defaultValue = "6")
	public static int FLOOD_MSG;
	/**
	 * 是否启用数据包洪水过滤。
	 * Whether packet flood filter is enabled.
	 */
	@Property(key = "gameserver.security.pff.enable", defaultValue = "false")
	public static boolean PFF_ENABLE;
	/**
	 * 数据包洪水过滤级别。
	 * Packet flood filter level.
	 */
	@Property(key = "gameserver.security.pff.level", defaultValue = "1")
	public static int PFF_LEVEL;
	/**
	 * 是否启用交易所预购校验。
	 * Whether broker prebuy check is enabled.
	 */
	@Property(key = "gameserver.security.broker.prebuy", defaultValue = "true")
	public static boolean BROKER_PREBUY_CHECK;
	/**
	 * 是否启用飞行路径校验。
	 * Whether flypath validator is enabled.
	 */
	@Property(key = "gameserver.security.validation.flypath", defaultValue = "false")
	public static boolean ENABLE_FLYPATH_VALIDATOR;
	/**
	 * 问卷延迟（分钟）。
	 * Survey delay in minutes.
	 */
	@Property(key = "gameserver.security.survey.delay.minute", defaultValue = "20")
	public static int SURVEY_DELAY;
	/**
	 * 是否启用完整性检查。
	 * Whether integrity check is enabled.
	 */
	@Property(key = "gameserver.security.integrity.check", defaultValue = "true")
	public static boolean INTEGRITY_CHECK;
	/**
	 * 是否检查生物属性异常。
	 * Whether creature stats check is enabled.
	 */
	@Property(key = "gameserver.security.check.creature.stats", defaultValue = "false")
	public static boolean STATS_CHECK;
}
