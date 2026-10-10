package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 安全防护、反外挂与验证相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Security, anti-hack and validation related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver.security")
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

	private final GmauditProperties gmaudit = new GmauditProperties();
	private final AntihackProperties antihack = new AntihackProperties();
	private final NoAnimationProperties noanimation = new NoAnimationProperties();
	private final CaptchaProperties captcha = new CaptchaProperties();
	private final PasskeyProperties passkey = new PasskeyProperties();
	private final PingcheckProperties pingcheck = new PingcheckProperties();
	private final FloodProperties flood = new FloodProperties();
	private final PffProperties pff = new PffProperties();
	private final MotionProperties motion = new MotionProperties();
	private final BrokerSecurityProperties broker = new BrokerSecurityProperties();
	private final ValidationProperties validation = new ValidationProperties();
	private final SurveyProperties survey = new SurveyProperties();
	private final IntegrityProperties integrity = new IntegrityProperties();
	private final CheckProperties check = new CheckProperties();

	public GmauditProperties getGmaudit() {
		return gmaudit;
	}

	public AntihackProperties getAntihack() {
		return antihack;
	}

	public NoAnimationProperties getNoanimation() {
		return noanimation;
	}

	public void setNoanimation(NoAnimationProperties noanimation) {
	}

	public CaptchaProperties getCaptcha() {
		return captcha;
	}

	public PasskeyProperties getPasskey() {
		return passkey;
	}

	public PingcheckProperties getPingcheck() {
		return pingcheck;
	}

	public FloodProperties getFlood() {
		return flood;
	}

	public PffProperties getPff() {
		return pff;
	}

	public MotionProperties getMotion() {
		return motion;
	}

	public BrokerSecurityProperties getBroker() {
		return broker;
	}

	public ValidationProperties getValidation() {
		return validation;
	}

	public SurveyProperties getSurvey() {
		return survey;
	}

	public IntegrityProperties getIntegrity() {
		return integrity;
	}

	public CheckProperties getCheck() {
		return check;
	}

	public static class GmauditProperties {
		private final MessageProperties message = new MessageProperties();

		public MessageProperties getMessage() {
			return message;
		}

		public static class MessageProperties {
			public boolean isBroadcast() {
				return GM_AUDIT_MESSAGE_BROADCAST;
			}

			public void setBroadcast(boolean broadcast) {
				GM_AUDIT_MESSAGE_BROADCAST = broadcast;
			}
		}
	}

	public static class AntihackProperties {
		private final SpeedhackProperties speedhack = new SpeedhackProperties();
		private final AbnormalProperties abnormal = new AbnormalProperties();

		public boolean isInvis() {
			return INVIS;
		}

		public void setInvis(boolean invis) {
			INVIS = invis;
		}

		public boolean isTeleportation() {
			return TELEPORTATION;
		}

		public void setTeleportation(boolean teleportation) {
			TELEPORTATION = teleportation;
		}

		public SpeedhackProperties getSpeedhack() {
			return speedhack;
		}

		public void setSpeedhack(SpeedhackProperties speedhack) {
		}

		public AbnormalProperties getAbnormal() {
			return abnormal;
		}

		public void setAbnormal(AbnormalProperties abnormal) {
		}

		public int getPunish() {
			return PUNISH;
		}

		public void setPunish(int punish) {
			PUNISH = punish;
		}

		public static class SpeedhackProperties {
			public SpeedhackProperties() {}

			public static SpeedhackProperties valueOf(String value) {
				SPEEDHACK = Boolean.parseBoolean(value);
				return new SpeedhackProperties();
			}

			public boolean isEnable() {
				return SPEEDHACK;
			}

			public void setEnable(boolean enable) {
				SPEEDHACK = enable;
			}

			public int getCounter() {
				return SPEEDHACK_COUNTER;
			}

			public void setCounter(int counter) {
				SPEEDHACK_COUNTER = counter;
			}
		}

		public static class AbnormalProperties {
			public AbnormalProperties() {}

			public static AbnormalProperties valueOf(String value) {
				ABNORMAL = Boolean.parseBoolean(value);
				return new AbnormalProperties();
			}

			public boolean isEnable() {
				return ABNORMAL;
			}

			public void setEnable(boolean enable) {
				ABNORMAL = enable;
			}

			public int getCounter() {
				return ABNORMAL_COUNTER;
			}

			public void setCounter(int counter) {
				ABNORMAL_COUNTER = counter;
			}
		}
	}

	public static class NoAnimationProperties {
		public NoAnimationProperties() {}

		public static NoAnimationProperties valueOf(String value) {
			NO_ANIMATION = Boolean.parseBoolean(value);
			return new NoAnimationProperties();
		}

		public boolean isEnable() {
			return NO_ANIMATION;
		}

		public void setEnable(boolean enable) {
			NO_ANIMATION = enable;
		}

		public boolean isKick() {
			return NO_ANIMATION_KICK;
		}

		public void setKick(boolean kick) {
			NO_ANIMATION_KICK = kick;
		}

		public float getValue() {
			return NO_ANIMATION_VALUE;
		}

		public void setValue(float value) {
			NO_ANIMATION_VALUE = value;
		}
	}

	public static class MotionProperties {
		private final TimeProperties time = new TimeProperties();

		public TimeProperties getTime() {
			return time;
		}

		public static class TimeProperties {
			public boolean isEnable() {
				return MOTION_TIME;
			}

			public void setEnable(boolean enable) {
				MOTION_TIME = enable;
			}
		}
	}

	public static class CaptchaProperties {
		private final AppearProperties appear = new AppearProperties();

		public boolean isEnable() {
			return CAPTCHA_ENABLE;
		}

		public void setEnable(boolean enable) {
			CAPTCHA_ENABLE = enable;
		}

		public AppearProperties getAppear() {
			return appear;
		}

		public void setAppear(AppearProperties appear) {
		}

		public int getExtractionBanTime() {
			return CAPTCHA_EXTRACTION_BAN_TIME;
		}

		public void setExtractionBanTime(int extractionBanTime) {
			CAPTCHA_EXTRACTION_BAN_TIME = extractionBanTime;
		}

		public static class AppearProperties {
			public AppearProperties() {}

			public static AppearProperties valueOf(String value) {
				CAPTCHA_APPEAR = value;
				return new AppearProperties();
			}

			public String getValue() {
				return CAPTCHA_APPEAR;
			}

			public void setValue(String value) {
				CAPTCHA_APPEAR = value;
			}

			public int getRate() {
				return CAPTCHA_APPEAR_RATE;
			}

			public void setRate(int rate) {
				CAPTCHA_APPEAR_RATE = rate;
			}
		}
	}

	public static class PasskeyProperties {
		private final WrongProperties wrong = new WrongProperties();

		public boolean isEnable() {
			return PASSKEY_ENABLE;
		}

		public void setEnable(boolean enable) {
			PASSKEY_ENABLE = enable;
		}

		public WrongProperties getWrong() {
			return wrong;
		}

		public static class WrongProperties {
			public int getMaxcount() {
				return PASSKEY_WRONG_MAXCOUNT;
			}

			public void setMaxcount(int maxcount) {
				PASSKEY_WRONG_MAXCOUNT = maxcount;
			}
		}
	}

	public static class PingcheckProperties {
		public boolean isEnable() {
			return SECURITY_ENABLE;
		}

		public void setEnable(boolean enable) {
			SECURITY_ENABLE = enable;
		}

		public int getInterval() {
			return PING_INTERVAL;
		}

		public void setInterval(int interval) {
			PING_INTERVAL = interval;
		}
	}

	public static class FloodProperties {
		public int getDelay() {
			return FLOOD_DELAY;
		}

		public void setDelay(int delay) {
			FLOOD_DELAY = delay;
		}

		public int getMsg() {
			return FLOOD_MSG;
		}

		public void setMsg(int msg) {
			FLOOD_MSG = msg;
		}
	}

	public static class PffProperties {
		public boolean isEnable() {
			return PFF_ENABLE;
		}

		public void setEnable(boolean enable) {
			PFF_ENABLE = enable;
		}

		public int getLevel() {
			return PFF_LEVEL;
		}

		public void setLevel(int level) {
			PFF_LEVEL = level;
		}
	}

	public static class BrokerSecurityProperties {
		public boolean isPrebuy() {
			return BROKER_PREBUY_CHECK;
		}

		public void setPrebuy(boolean prebuy) {
			BROKER_PREBUY_CHECK = prebuy;
		}
	}

	public static class ValidationProperties {
		public boolean isFlypath() {
			return ENABLE_FLYPATH_VALIDATOR;
		}

		public void setFlypath(boolean flypath) {
			ENABLE_FLYPATH_VALIDATOR = flypath;
		}
	}

	public static class SurveyProperties {
		private final DelayProperties delay = new DelayProperties();

		public DelayProperties getDelay() {
			return delay;
		}

		public static class DelayProperties {
			public int getMinute() {
				return SURVEY_DELAY;
			}

			public void setMinute(int minute) {
				SURVEY_DELAY = minute;
			}
		}
	}

	public static class IntegrityProperties {
		public boolean isCheck() {
			return INTEGRITY_CHECK;
		}

		public void setCheck(boolean check) {
			INTEGRITY_CHECK = check;
		}
	}

	public static class CheckProperties {
		private final CreatureProperties creature = new CreatureProperties();

		public CreatureProperties getCreature() {
			return creature;
		}

		public static class CreatureProperties {
			public boolean isStats() {
				return STATS_CHECK;
			}

			public void setStats(boolean stats) {
				STATS_CHECK = stats;
			}
		}
	}
}
