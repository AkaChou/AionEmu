package com.aionemu.gameserver.utils;

import java.util.Date;

/**
 * 时间判断与格式化工具。
 * Time checking and formatting utility.
 *
 * @author ATracer
 */
public class TimeUtil {

	/**
	 * 判断给定毫秒时间戳是否已过期。
	 * Checks whether the supplied time in milliseconds has expired.
	 *
	 * @param time 毫秒时间戳 / Timestamp in milliseconds
	 * @return 是否已过期 / Whether expired
	 */
	public static final boolean isExpired(long time) {
		return time < System.currentTimeMillis();
	}

	/**
	 * 将秒级时间戳格式化为本地日期时间字符串。
	 * Formats a second-based timestamp as a locale date-time string.
	 *
	 * @param time 秒级时间戳 / Timestamp in seconds
	 * @return 本地化日期时间字符串 / Locale date-time string
	 */
	@SuppressWarnings("deprecation")
	public static String getTimeData(long time) {
		Date d = new Date(time * 1000);
		String localDate = d.toLocaleString();
		return localDate;
	}
}
