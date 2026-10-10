package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 日志开关相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Logging switch related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class LoggingConfig {

	/**
	 * 是否启用高级日志。
	 * Whether advanced logging is enabled.
	 */
	@Property(key = "gameserver.enable.advanced.logging", defaultValue = "false")
	public static boolean ENABLE_ADVANCED_LOGGING;

	/**
	 * 是否记录审计日志。
	 * Whether audit logging is enabled.
	 */
	@Property(key = "gameserver.log.audit", defaultValue = "true")
	public static boolean LOG_AUDIT;

	/**
	 * 是否记录聊天日志。
	 * Whether chat logging is enabled.
	 */
	@Property(key = "gameserver.log.chat", defaultValue = "true")
	public static boolean LOG_CHAT;

	/**
	 * 是否记录阵营日志。
	 * Whether faction logging is enabled.
	 */
	@Property(key = "gameserver.log.faction", defaultValue = "false")
	public static boolean LOG_FACTION;

	/**
	 * 是否记录 GM 审计日志。
	 * Whether GM audit logging is enabled.
	 */
	@Property(key = "gameserver.log.gmaudit", defaultValue = "true")
	public static boolean LOG_GMAUDIT;

	/**
	 * 是否记录道具日志。
	 * Whether item logging is enabled.
	 */
	@Property(key = "gameserver.log.item", defaultValue = "true")
	public static boolean LOG_ITEM;

	/**
	 * 是否记录击杀日志。
	 * Whether kill logging is enabled.
	 */
	@Property(key = "gameserver.log.kill", defaultValue = "false")
	public static boolean LOG_KILL;

	/**
	 * 是否记录玩家日志。
	 * Whether player logging is enabled.
	 */
	@Property(key = "gameserver.log.pl", defaultValue = "false")
	public static boolean LOG_PL;

	/**
	 * 是否记录要塞战日志。
	 * Whether siege logging is enabled.
	 */
	@Property(key = "gameserver.log.siege", defaultValue = "false")
	public static boolean LOG_SIEGE;

	/**
	 * 是否记录房屋拍卖日志。
	 * Whether house auction logging is enabled.
	 */
	@Property(key = "gameserver.log.auction", defaultValue = "true")
	public static boolean LOG_HOUSE_AUCTION;

	/**
	 * 是否显示任务对话追踪调试日志。
	 * Whether quest dialog interaction trace logging is enabled.
	 */
	@Property(key = "gameserver.log.questtrace", defaultValue = "false")
	public static boolean LOG_QUEST_TRACE = false;

	@Getter
	private final EnableProperties enable = new EnableProperties();
	@Getter
	private final LogProperties log = new LogProperties();

	@Getter
	public static class EnableProperties {
		private final AdvancedProperties advanced = new AdvancedProperties();

		public static class AdvancedProperties {
			public boolean isLogging() {
				return ENABLE_ADVANCED_LOGGING;
			}

			public void setLogging(boolean logging) {
				ENABLE_ADVANCED_LOGGING = logging;
			}
		}
	}

	@Getter
	public static class LogProperties {
		public boolean isAudit() {
			return LOG_AUDIT;
		}

		public void setAudit(boolean audit) {
			LOG_AUDIT = audit;
		}

		public boolean isChat() {
			return LOG_CHAT;
		}

		public void setChat(boolean chat) {
			LOG_CHAT = chat;
		}

		public boolean isFaction() {
			return LOG_FACTION;
		}

		public void setFaction(boolean faction) {
			LOG_FACTION = faction;
		}

		public boolean isGmaudit() {
			return LOG_GMAUDIT;
		}

		public void setGmaudit(boolean gmaudit) {
			LOG_GMAUDIT = gmaudit;
		}

		public boolean isItem() {
			return LOG_ITEM;
		}

		public void setItem(boolean item) {
			LOG_ITEM = item;
		}

		public boolean isKill() {
			return LOG_KILL;
		}

		public void setKill(boolean kill) {
			LOG_KILL = kill;
		}

		public boolean isPl() {
			return LOG_PL;
		}

		public void setPl(boolean pl) {
			LOG_PL = pl;
		}

		public boolean isSiege() {
			return LOG_SIEGE;
		}

		public void setSiege(boolean siege) {
			LOG_SIEGE = siege;
		}

		public boolean isAuction() {
			return LOG_HOUSE_AUCTION;
		}

		public void setAuction(boolean auction) {
			LOG_HOUSE_AUCTION = auction;
		}

		public boolean isQuesttrace() {
			return LOG_QUEST_TRACE;
		}

		public void setQuesttrace(boolean questtrace) {
			LOG_QUEST_TRACE = questtrace;
		}
	}
}
