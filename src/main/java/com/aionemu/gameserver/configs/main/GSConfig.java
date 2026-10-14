package com.aionemu.gameserver.configs.main;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 游戏服务器核心相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Game server core related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class GSConfig {
	/**
	 * 服务器国家代码。5 = 盛大/国服（默认），99 = 自动适配。
	 * Server country code. 5 = SDO/China (default), 99 = auto-detect.
	 */
	@Property(key = "gameserver.country.code", defaultValue = "5")
	public static int SERVER_COUNTRY_CODE;

	/**
	 * 解析国家码：99 按系统 Locale、时区与环境综合映射，其余原样返回。
	 * Resolve country code: 99 maps from system Locale, timezone and environment, otherwise unchanged.
	 */
	public static int resolveCountryCode(int countryCode) {
		if (countryCode != 99) {
			return countryCode;
		}
		Locale locale = Locale.getDefault();
		if (isChineseEnvironment(locale)) {
			return 5;
		}
		String tzId = TimeZone.getDefault().getID();
		if (tzId != null) {
			String lowerTz = tzId.toLowerCase(Locale.ROOT);
			if (lowerTz.contains("seoul") || lowerTz.contains("korea")) {
				return 0;
			}
			if (lowerTz.contains("tokyo") || lowerTz.contains("japan")) {
				return 4;
			}
			if (lowerTz.contains("moscow")) {
				return 7;
			}
		}
		return switch (locale.getLanguage()) {
			case "zh" -> 5;
			case "ja" -> 4;
			case "ko" -> 0;
			case "ru" -> 7;
			default -> switch (locale.getCountry()) {
				case "GB", "DE", "FR", "ES", "IT", "PL", "NL", "SE", "NO", "DK", "FI", "AT", "CH", "BE", "PT", "IE",
					 "CZ", "HU", "RO", "GR" -> 2;
				default -> 1;
			};
		};
	}

	/**
	 * 检查当前宿主环境是否为中文环境（综合判断 Locale、时区、系统编码与环境变量）。
	 * Checks if the current host environment is Chinese (evaluates Locale, timezone, encoding and env vars).
	 */
	private static boolean isChineseEnvironment(Locale locale) {
		if (locale != null) {
			String lang = locale.getLanguage();
			String country = locale.getCountry();
			if ("zh".equalsIgnoreCase(lang) || "CN".equalsIgnoreCase(country)
				|| "TW".equalsIgnoreCase(country) || "HK".equalsIgnoreCase(country) || "MO".equalsIgnoreCase(country)) {
				return true;
			}
		}
		for (Locale.Category category : Locale.Category.values()) {
			Locale catLocale = Locale.getDefault(category);
			if (catLocale != null) {
				if ("zh".equalsIgnoreCase(catLocale.getLanguage()) || "CN".equalsIgnoreCase(catLocale.getCountry())) {
					return true;
				}
			}
		}
		String userLang = System.getProperty("user.language");
		String userCountry = System.getProperty("user.country");
		String userRegion = System.getProperty("user.region");
		if ("zh".equalsIgnoreCase(userLang) || "CN".equalsIgnoreCase(userCountry) || "CN".equalsIgnoreCase(userRegion)) {
			return true;
		}
		for (String encodingKey : new String[] {"sun.jnu.encoding", "file.encoding", "native.encoding"}) {
			String enc = System.getProperty(encodingKey);
			if (enc != null && (enc.equalsIgnoreCase("GBK") || enc.equalsIgnoreCase("GB2312")
				|| enc.equalsIgnoreCase("GB18030") || enc.equalsIgnoreCase("MS936") || enc.equalsIgnoreCase("x-windows-936"))) {
				return true;
			}
		}
		for (String envKey : new String[] {"LANG", "LC_ALL", "LC_CTYPE", "LANGUAGE"}) {
			String val = System.getenv(envKey);
			if (val != null && (val.contains("zh") || val.contains("CN") || val.contains("GBK") || val.contains("gbk") || val.contains("18030"))) {
				return true;
			}
		}
		String tzId = TimeZone.getDefault().getID();
		if (tzId != null) {
			String lower = tzId.toLowerCase(Locale.ROOT);
			if (lower.contains("shanghai") || lower.contains("chongqing") || lower.contains("harbin")
				|| lower.contains("urumqi") || lower.contains("beijing") || lower.contains("china")
				|| "prc".equalsIgnoreCase(tzId) || "ctt".equalsIgnoreCase(tzId)) {
				return true;
			}
		}
		String userTz = System.getProperty("user.timezone");
		if (userTz != null) {
			String lower = userTz.toLowerCase(Locale.ROOT);
			if (lower.contains("shanghai") || lower.contains("chongqing") || lower.contains("harbin")
				|| lower.contains("urumqi") || lower.contains("beijing") || lower.contains("china")
				|| "prc".equalsIgnoreCase(userTz) || "ctt".equalsIgnoreCase(userTz)) {
				return true;
			}
		}
		return false;
	}

	/** 将 99 解析为具体地区码并写回字段。 / Resolve 99 into a concrete code in-place. */
	public static void resolveServerCountryCode() {
		SERVER_COUNTRY_CODE = resolveCountryCode(SERVER_COUNTRY_CODE);
	}
	/**
	 * 玩家最大等级。
	 * Player maximum level.
	 */
	@Property(key = "gameserver.players.max.level", defaultValue = "83")
	public static int PLAYER_MAX_LEVEL;
	/**
	 * 服务器时区 ID（空则使用系统时区）。
	 * Server timezone ID (empty uses system timezone).
	 */
	@Property(key = "gameserver.timezone", defaultValue = "")
	public static String TIME_ZONE_ID = Calendar.getInstance().getTimeZone().getID();
	/**
	 * 是否启用聊天服务器。
	 * Whether chat server is enabled.
	 */
	@Property(key = "gameserver.chatserver.enable", defaultValue = "false")
	public static boolean ENABLE_CHAT_SERVER;
	/**
	 * 是否启用启动进度输出。
	 * Whether startup progress output is enabled.
	 */
	@Property(key = "gameserver.startup.progress.enable", defaultValue = "true")
	public static boolean STARTUP_PROGRESS_ENABLE = true;
	/**
	 * 是否在静态数据加载进度中显示条目数。
	 * Whether static-data progress shows entry counts.
	 */
	@Property(key = "gameserver.static_data.progress.entry_counts.enable", defaultValue = "false")
	public static boolean STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE = false;
	/**
	 * 是否输出静态数据加载摘要日志（硬编码字段）。
	 * Whether static-data summary log is enabled (hardcoded field).
	 */
	public static boolean STATIC_DATA_SUMMARY_LOG;
	/**
	 * 角色创建模式。
	 * Character creation mode.
	 */
	@Property(key = "gameserver.character.creation.mode", defaultValue = "0")
	public static int CHARACTER_CREATION_MODE;
	/**
	 * 每账号角色数量上限。
	 * Maximum character count per account.
	 */
	@Property(key = "gameserver.character.limit.count", defaultValue = "8")
	public static int CHARACTER_LIMIT_COUNT;
	/**
	 * 角色删除倒计时（分钟）。
	 * Character deletion countdown in minutes.
	 */
	@Property(key = "gameserver.character.delete.delay.minutes", defaultValue = "10")
	public static int CHARACTER_DELETE_DELAY_MINUTES;
	/**
	 * 角色阵营限制模式。
	 * Character faction limitation mode.
	 */
	@Property(key = "gameserver.character.faction.limitation.mode", defaultValue = "0")
	public static int CHARACTER_FACTION_LIMITATION_MODE;
	/**
	 * 是否启用阵营人数比例限制。
	 * Whether faction ratio limitation is enabled.
	 */
	@Property(key = "gameserver.ratio.limitation.enable", defaultValue = "false")
	public static boolean ENABLE_RATIO_LIMITATION;
	/**
	 * 比例限制统计的最低角色等级。
	 * Minimum character level counted for ratio limitation.
	 */
	@Property(key = "gameserver.ratio.min.required.level", defaultValue = "10")
	public static int RATIO_MIN_REQUIRED_LEVEL;
	/**
	 * 比例限制生效所需的最少角色数。
	 * Minimum characters count required before ratio limitation applies.
	 */
	@Property(key = "gameserver.ratio.min.characters_count", defaultValue = "50")
	public static int RATIO_MIN_CHARACTERS_COUNT;
	/**
	 * 是否使用精简欧比斯排行缓存。
	 * Whether abyss ranking uses small cache.
	 */
	@Property(key = "gameserver.abyssranking.small.cache", defaultValue = "false")
	public static boolean ABYSSRANKING_SMALL_CACHE;
	/**
	 * 角色重进世界冷却时间（秒）。
	 * Character re-entry cooldown time in seconds.
	 */
	@Property(key = "gameserver.character.reentry.time", defaultValue = "20")
	public static int CHARACTER_REENTRY_TIME;
	/**
	 * 是否启用 YA 管理面板服务。
	 * Whether YA admin panel server is enabled.
	 */
	@Property(key = "gameserver.yaadminpanel.server.enable", defaultValue = "false")
	public static boolean SERVER_YAADMINPANEL_SWITCH_ON;

	/** 国家码父节点 / country parent node. */
	@Getter
	private final CountryProperties country = new CountryProperties();
	/** 玩家父节点 / players parent node. */
	@Getter
	private final PlayersProperties players = new PlayersProperties();
	/** 聊天服父节点 / chat-server parent node. */
	@Getter
	private final ChatserverProperties chatserver = new ChatserverProperties();
	/** 启动进度父节点 / startup parent node. */
	@Getter
	private final StartupProperties startup = new StartupProperties();
	/** 静态数据父节点 / static-data parent node. */
	@Getter
	private final StaticDataProperties staticData = new StaticDataProperties();
	/** 角色父节点 / character parent node. */
	@Getter
	private final CharacterProperties character = new CharacterProperties();
	/** 比例限制父节点 / ratio parent node. */
	@Getter
	private final RatioProperties ratio = new RatioProperties();
	/** 欧比斯排行父节点 / abyss-ranking parent node. */
	@Getter
	private final AbyssrankingProperties abyssranking = new AbyssrankingProperties();
	/** YA 管理面板父节点 / YA admin panel parent node. */
	@Getter
	private final YaadminpanelProperties yaadminpanel = new YaadminpanelProperties();

	public String getTimezone() {
		return TIME_ZONE_ID;
	}

	public void setTimezone(String timezone) {
		TIME_ZONE_ID = timezone;
	}

	/**
	 * 国家码节点：绑定 gameserver.country.code。
	 * Country node binding gameserver.country.code.
	 */
	@Getter
	public static class CountryProperties {
		public int getCode() {
			return SERVER_COUNTRY_CODE;
		}

		public void setCode(int code) {
			SERVER_COUNTRY_CODE = code;
		}
	}

	/**
	 * 聊天服父节点：对应配置元素 gameserver.chatserver。
	 * Chat-server parent node matching the gameserver.chatserver element.
	 */
	@Getter
	public static class ChatserverProperties {
		public boolean isEnable() {
			return ENABLE_CHAT_SERVER;
		}

		public void setEnable(boolean enable) {
			ENABLE_CHAT_SERVER = enable;
		}
	}

	/**
	 * 启动进度父节点：对应配置元素 gameserver.startup。
	 * Startup parent node matching the gameserver.startup element.
	 */
	@Getter
	public static class StartupProperties {
		private final StartupProgressProperties progress = new StartupProgressProperties();
	}

	/**
	 * 启动进度父节点：对应配置元素 gameserver.startup.progress。
	 * Startup-progress parent node matching the gameserver.startup.progress element.
	 */
	@Getter
	public static class StartupProgressProperties {
		public boolean isEnable() {
			return STARTUP_PROGRESS_ENABLE;
		}

		public void setEnable(boolean enable) {
			STARTUP_PROGRESS_ENABLE = enable;
		}
	}

	/**
	 * 静态数据父节点：对应配置元素 gameserver.static_data。
	 * Static-data parent node matching the gameserver.static_data element.
	 */
	@Getter
	public static class StaticDataProperties {
		private final StaticDataProgressProperties progress = new StaticDataProgressProperties();
	}

	/**
	 * 静态数据进度父节点：对应配置元素 gameserver.static_data.progress。
	 * Static-data progress parent node matching the gameserver.static_data.progress element.
	 */
	@Getter
	public static class StaticDataProgressProperties {
		private final EntryCountsProperties entryCounts = new EntryCountsProperties();
	}

	/**
	 * 静态数据条目数父节点：对应配置元素 gameserver.static_data.progress.entry_counts。
	 * Static-data entry-counts parent node matching the gameserver.static_data.progress.entry_counts element.
	 */
	@Getter
	public static class EntryCountsProperties {
		public boolean isEnable() {
			return STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE;
		}

		public void setEnable(boolean enable) {
			STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE = enable;
		}
	}

	/**
	 * 玩家父节点：对应配置元素 gameserver.players。
	 * Players parent node matching the gameserver.players element.
	 */
	@Getter
	public static class PlayersProperties {
		private final MaxProperties max = new MaxProperties();
	}

	/** 玩家等级上限父节点 / Player max-level parent node. */
	@Getter
	public static class MaxProperties {
		public int getLevel() {
			return PLAYER_MAX_LEVEL;
		}

		public void setLevel(int level) {
			PLAYER_MAX_LEVEL = level;
		}
	}

	/**
	 * 角色节点：绑定 gameserver.character.*。
	 * Character node binding gameserver.character.*.
	 */
	@Getter
	public static class CharacterProperties {
		private final CreationProperties creation = new CreationProperties();
		private final LimitProperties limit = new LimitProperties();
		private final DeleteProperties delete = new DeleteProperties();
		private final FactionProperties faction = new FactionProperties();
		private final ReentryProperties reentry = new ReentryProperties();
	}

	/** 角色创建父节点 / Character-creation parent node. */
	@Getter
	public static class CreationProperties {
		public int getMode() {
			return CHARACTER_CREATION_MODE;
		}

		public void setMode(int mode) {
			CHARACTER_CREATION_MODE = mode;
		}
	}

	/** 角色数量上限父节点 / Character-limit parent node. */
	@Getter
	public static class LimitProperties {
		public int getCount() {
			return CHARACTER_LIMIT_COUNT;
		}

		public void setCount(int count) {
			CHARACTER_LIMIT_COUNT = count;
		}
	}

	/** 角色删除父节点 / Character-delete parent node. */
	@Getter
	public static class DeleteProperties {
		private final DeleteDelayProperties delay = new DeleteDelayProperties();
	}

	/** 角色删除延迟父节点 / Character-delete-delay parent node. */
	@Getter
	public static class DeleteDelayProperties {
		public int getMinutes() {
			return CHARACTER_DELETE_DELAY_MINUTES;
		}

		public void setMinutes(int minutes) {
			CHARACTER_DELETE_DELAY_MINUTES = minutes;
		}
	}

	/** 角色阵营父节点 / Character-faction parent node. */
	@Getter
	public static class FactionProperties {
		private final FactionLimitationProperties limitation = new FactionLimitationProperties();
	}

	/** 角色阵营限制父节点 / Character-faction-limitation parent node. */
	@Getter
	public static class FactionLimitationProperties {
		public int getMode() {
			return CHARACTER_FACTION_LIMITATION_MODE;
		}

		public void setMode(int mode) {
			CHARACTER_FACTION_LIMITATION_MODE = mode;
		}
	}

	/** 角色重进父节点 / Character-reentry parent node. */
	@Getter
	public static class ReentryProperties {
		public int getTime() {
			return CHARACTER_REENTRY_TIME;
		}

		public void setTime(int time) {
			CHARACTER_REENTRY_TIME = time;
		}
	}

	/**
	 * 比例限制节点：绑定 gameserver.ratio.*。
	 * Ratio-limitation node binding gameserver.ratio.*.
	 */
	@Getter
	public static class RatioProperties {
		private final LimitationProperties limitation = new LimitationProperties();
		private final MinProperties min = new MinProperties();
	}

	/**
	 * 比例限制父节点：对应配置元素 gameserver.ratio.limitation。
	 * Ratio-limitation parent node matching the gameserver.ratio.limitation element.
	 */
	@Getter
	public static class LimitationProperties {
		public boolean isEnable() {
			return ENABLE_RATIO_LIMITATION;
		}

		public void setEnable(boolean enable) {
			ENABLE_RATIO_LIMITATION = enable;
		}
	}

	/**
	 * 比例最小值父节点：对应配置元素 gameserver.ratio.min。
	 * Ratio-minimum parent node matching the gameserver.ratio.min element.
	 */
	@Getter
	public static class MinProperties {
		private final RequiredProperties required = new RequiredProperties();

		public int getCharactersCount() {
			return RATIO_MIN_CHARACTERS_COUNT;
		}

		public void setCharactersCount(int charactersCount) {
			RATIO_MIN_CHARACTERS_COUNT = charactersCount;
		}
	}

	/**
	 * 比例最低等级父节点：对应配置元素 gameserver.ratio.min.required。
	 * Ratio-minimum-level parent node matching the gameserver.ratio.min.required element.
	 */
	@Getter
	public static class RequiredProperties {
		public int getLevel() {
			return RATIO_MIN_REQUIRED_LEVEL;
		}

		public void setLevel(int level) {
			RATIO_MIN_REQUIRED_LEVEL = level;
		}
	}

	/**
	 * 欧比斯排行节点：绑定 gameserver.abyssranking.*。
	 * Abyss-ranking node binding gameserver.abyssranking.*.
	 */
	@Getter
	public static class AbyssrankingProperties {
		private final SmallProperties small = new SmallProperties();
	}

	/** 欧比斯排行缓存父节点 / Abyss-ranking cache parent node. */
	@Getter
	public static class SmallProperties {
		public boolean isCache() {
			return ABYSSRANKING_SMALL_CACHE;
		}

		public void setCache(boolean cache) {
			ABYSSRANKING_SMALL_CACHE = cache;
		}
	}

	/**
	 * YA 管理面板节点：绑定 gameserver.yaadminpanel.*。
	 * YA admin panel node binding gameserver.yaadminpanel.*.
	 */
	@Getter
	public static class YaadminpanelProperties {
		private final ServerProperties server = new ServerProperties();
	}

	/** YA 管理面板服务器父节点 / YA admin panel server parent node. */
	@Getter
	public static class ServerProperties {
		public boolean isEnable() {
			return SERVER_YAADMINPANEL_SWITCH_ON;
		}

		public void setEnable(boolean enable) {
			SERVER_YAADMINPANEL_SWITCH_ON = enable;
		}
	}
}
