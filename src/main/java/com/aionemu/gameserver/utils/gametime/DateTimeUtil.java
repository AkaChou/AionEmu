package com.aionemu.gameserver.utils.gametime;

import com.aionemu.boot.i18n.I18n;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.aionemu.gameserver.configs.main.GSConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 Java 8 Time API 的日期时间工具，应用 GSConfig 时区并尽量保留日历字段。
 * Date/time utility using the Java 8 Time API; applies GSConfig zone while retaining calendar fields.
 *
 * @author Rolandas
 */
@Slf4j
public final class DateTimeUtil {

	/**
	 * 配置中的时区。
	 * Configured zone id.
	 */
	private static ZoneId configuredZoneId;
	/**
	 * 是否使用配置时区。
	 * Whether the configured zone is active.
	 */
	private static boolean useConfiguredZone = false;

	/**
	 * 工具类私有构造。
	 * Private constructor for utility class.
	 */
	private DateTimeUtil() {
		// 工具类的私有构造方法 / Private constructor for utility class
	}

	/**
	 * 初始化：从 GSConfig 加载时区。
	 * Initialize: load time zone from GSConfig.
	 */
	public static void init() {
		String zoneConfig = GSConfig.TIME_ZONE_ID;

		if (zoneConfig != null && !zoneConfig.trim().isEmpty()) {
			try {
				configuredZoneId = ZoneId.of(zoneConfig.trim());
				useConfiguredZone = true;
				log.info(I18n.get("log.530e4ccc70d1", configuredZoneId.getId()));
			} catch (Exception e) {
				log.error(I18n.get("log.2e8994c20b6c", zoneConfig, e));
				configuredZoneId = ZoneId.systemDefault();
				useConfiguredZone = false;
			}
		} else {
			// 配置未指定时使用系统时区（默认已在 GSConfig.TIME_ZONE_ID）。 / If not specified in config, use system zone (which is already in GSConfig.TIME_ZONE_ID by default)
			String systemZoneId = Calendar.getInstance().getTimeZone().getID();
			configuredZoneId = ZoneId.of(systemZoneId);
			useConfiguredZone = false;
			log.info(I18n.get("log.177ac71e1842", systemZoneId));
		}
	}

	/**
	 * 返回配置时区下的当前日期时间。
	 * Current date/time in the configured zone.
	 *
	 * Current ZonedDateTime
	 */
	public static ZonedDateTime now() {
		return ZonedDateTime.now(getZoneWithFallback());
	}

	/**
	 * 从 ISO 字符串创建 ZonedDateTime（保留字段后应用配置时区）。
	 * Create ZonedDateTime from an ISO string (retain fields, apply configured zone).
	 *
	 * @param isoDateTime ISO 日期时间字符串 / ISO date-time string
	 * @return ZonedDateTime
	 */
	public static ZonedDateTime fromIsoString(String isoDateTime) {
		try {
			LocalDateTime localDateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			return applyZoneRetainFields(localDateTime);
		} catch (DateTimeParseException e) {
			log.error(I18n.get("log.2d6fa0340d1f", isoDateTime, e));
			return now();
		}
	}

	/**
	 * 按指定格式从字符串创建 ZonedDateTime。
	 * Create ZonedDateTime from a string with the given formatter.
	 *
	 * @param dateTimeString 日期时间字符串 / Date-time string
	 * Formatter
	 * @return ZonedDateTime
	 */
	public static ZonedDateTime fromString(String dateTimeString, DateTimeFormatter formatter) {
		try {
			LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, formatter);
			return applyZoneRetainFields(localDateTime);
		} catch (DateTimeParseException e) {
			log.error(I18n.get("log.177d6d05cfac", dateTimeString, formatter, e));
			return now();
		}
	}

	/**
	 * 从 Calendar 创建 ZonedDateTime。
	 * Create ZonedDateTime from a Calendar.
	 *
	 * Calendar
	 * @return ZonedDateTime
	 */
	public static ZonedDateTime fromCalendar(Calendar calendar) {
		if (calendar == null) {
			return now();
		}
		return calendar.toInstant().atZone(getZoneWithFallback());
	}

	/**
	 * 从 epoch 毫秒创建 ZonedDateTime。
	 * Create ZonedDateTime from epoch millis.
	 *
	 * @param millisSinceEpoch 自 1970-01-01 起的毫秒 / Millis since epoch
	 * @return ZonedDateTime
	 */
	public static ZonedDateTime fromMillis(long millisSinceEpoch) {
		return Instant.ofEpochMilli(millisSinceEpoch).atZone(getZoneWithFallback());
	}

	/**
	 * 从 Instant 创建 ZonedDateTime。
	 * Create ZonedDateTime from an Instant.
	 *
	 * @param instant Instant
	 * @return ZonedDateTime
	 */
	public static ZonedDateTime fromInstant(Instant instant) {
		if (instant == null) {
			return now();
		}
		return instant.atZone(getZoneWithFallback());
	}

	/**
	 * 从游戏时间分钟数创建 ZonedDateTime（近似）。
	 * Create ZonedDateTime from game-time minutes (approximation).
	 *
	 * @param gameTimeMinutes 游戏时间分钟 / Game-time minutes
	 * @return 配置时区下的 ZonedDateTime / ZonedDateTime in configured zone
	 */
	public static ZonedDateTime fromGameTime(int gameTimeMinutes) {
		// GameTime 从 0000-01-01 计分钟，Java Time 从 1970 起 / GameTime counts minutes from 01.01.0000, Java Time from 1970
		// 这是用于演示的粗略近似 / This is a rough approximation for demonstration
		long gameTimeMillis = (long) gameTimeMinutes * 60 * 1000;
		return fromMillis(gameTimeMillis);
	}

	/**
	 * 将 LocalDateTime 应用配置时区并保留字段值。
	 * Apply configured zone to LocalDateTime while retaining field values.
	 *
	 * @param localDateTime 本地日期时间 / Local date-time
	 * @return ZonedDateTime
	 */
	public static ZonedDateTime applyZoneRetainFields(LocalDateTime localDateTime) {
		if (localDateTime == null) {
			return now();
		}
		return localDateTime.atZone(getZoneWithFallback());
	}

	/**
	 * 将 ZonedDateTime 字段保留后改用配置时区。
	 * Re-apply configured zone to a ZonedDateTime while retaining local fields.
	 *
	 * Source zoned date-time
	 * @return ZonedDateTime
	 */
	public static ZonedDateTime applyZoneRetainFields(ZonedDateTime zonedDateTime) {
		if (zonedDateTime == null) {
			return now();
		}
		if (!useConfiguredZone) {
			return zonedDateTime;
		}

		// 提取本地字段（年、月、日、时、分等） / Extract local fields (year, month, day, hour, minute, etc.)
		LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();

		// 应用到已配置区域 / Apply them to configured zone
		return localDateTime.atZone(configuredZoneId);
	}

	/**
	 * 是否正在使用配置时区。
	 * Whether the configured zone is in use.
	 *
	 * @return 使用配置时区则为 true / True if configured zone is used
	 */
	public static boolean isConfiguredZoneUsed() {
		return useConfiguredZone;
	}

	/**
	 * 返回当前配置时区（未启用时回退系统默认）。
	 * Current configured zone, or system default if disabled.
	 *
	 * @return ZoneId
	 */
	public static ZoneId getZone() {
		return getZoneWithFallback();
	}

	/**
	 * 按指定格式格式化 ZonedDateTime。
	 * Format a ZonedDateTime with the given formatter.
	 *
	 * Date-time
	 * Formatter
	 * @return 格式化字符串 / Formatted string
	 */
	public static String format(ZonedDateTime dateTime, DateTimeFormatter formatter) {
		if (dateTime == null) {
			return "";
		}
		return dateTime.format(formatter);
	}

	/**
	 * 转为 GregorianCalendar。
	 * Convert to GregorianCalendar.
	 *
	 * Date-time
	 * GregorianCalendar or null
	 */
	public static GregorianCalendar toGregorianCalendar(ZonedDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return GregorianCalendar.from(dateTime);
	}

	/**
	 * 转为 java.util.Calendar（时区来自 dateTime）。
	 * Convert to java.util.Calendar (zone taken from dateTime).
	 *
	 * Date-time
	 * Calendar or null
	 */
	public static Calendar toCalendar(ZonedDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return Calendar.getInstance(TimeZone.getTimeZone(dateTime.getZone()));
	}

	/**
	 * 返回 epoch 毫秒。
	 * Epoch millis for the given ZonedDateTime.
	 *
	 * Date-time
	 * Millis, or 0 if null
	 */
	public static long toMillis(ZonedDateTime dateTime) {
		if (dateTime == null) {
			return 0;
		}
		return dateTime.toInstant().toEpochMilli();
	}

	/**
	 * 返回 ISO 本地日期时间字符串。
	 * ISO local date-time string representation.
	 *
	 * Date-time
	 * ISO string
	 */
	public static String toIsoString(ZonedDateTime dateTime) {
		if (dateTime == null) {
			return "";
		}
		return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}

	/**
	 * 获取时区，未启用配置时回退系统默认。
	 * Resolve zone with fallback to system default.
	 *
	 * @return ZoneId
	 */
	private static ZoneId getZoneWithFallback() {
		if (useConfiguredZone && configuredZoneId != null) {
			return configuredZoneId;
		}
		return ZoneId.systemDefault();
	}
}
