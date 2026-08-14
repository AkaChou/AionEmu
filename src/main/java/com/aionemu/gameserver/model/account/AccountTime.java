package com.aionemu.gameserver.model.account;

/**
 * 账号时间模型。
 * Account Time model.
 *
 * @author EvilSpirit
 */
public class AccountTime {

	/**
	 * 累计在线时间（毫秒）。
	 * Accumulated online time in millis
	 */
	private long accumulatedOnlineTime;

	/**
	 * 累计休息（离线）时间（毫秒）。
	 * Accumulated rest(offline) time in millis
	 */
	private long accumulatedRestTime;

	/**
	 * 获取 dailyaccumulatedonline 时间 millis。
	 * get daily accumulated online time in millis
	 *
	 * @return 毫秒数 / time in millis
	 */
	public long getAccumulatedOnlineTime() {
		return accumulatedOnlineTime;
	}

	/**
	 * 获取 dailyaccumulatedonline 时间 millis。
	 * get daily accumulated online time in millis
	 *
	 * @param accumulatedOnlineTime 累计在线毫秒数 / time in millis
	 */
	public void setAccumulatedOnlineTime(long accumulatedOnlineTime) {
		this.accumulatedOnlineTime = accumulatedOnlineTime;
	}

	/**
	 * 获取 dailyaccumulatedrestoffline 时间 sincelogin。
	 * get daily accumulated rest (offline) time since first login
	 *
	 * @return 毫秒数 / time in millis
	 */
	public long getAccumulatedRestTime() {
		return accumulatedRestTime;
	}

	/**
	 * 获取 dailyaccumulatedrestoffline 时间 sincelogin。
	 * get daily accumulated rest (offline) time since first login
	 *
	 * @param accumulatedRestTime 累计休息毫秒数 / time in millis
	 */
	public void setAccumulatedRestTime(long accumulatedRestTime) {
		this.accumulatedRestTime = accumulatedRestTime;
	}

	/**
	 * 返回小时部分（向下取整），例如 1 小时 32 分钟返回 1 小时。
	 * Returns hour part rounded down.<br> For instance if time is 1 hr 32 min - it will return 1 hr
	 *
	 * @return 累计在线时间的小时部分 / hours part of accumulated online time
	 */
	public int getAccumulatedOnlineHours() {
		return toHours(accumulatedOnlineTime);
	}

	/**
	 * 返回分钟部分，例如 1 小时 32 分钟返回 32 分钟。
	 * Returns minutes part.<br> For instance: if time is 1 hr 32 min - it will return 32 min
	 *
	 * @return 累计在线时间的分钟部分 / minutes part of accumulated online time
	 */
	public int getAccumulatedOnlineMinutes() {
		return toMinutes(accumulatedOnlineTime);
	}

	/**
	 * 返回小时部分（向下取整），例如 1 小时 32 分钟返回 1 小时。
	 * Returns hour part rounded down.<br> For instance if time is 1 hr 32 min - it will return 1 hr
	 *
	 * @return 累计休息时间的小时部分 / hours part of accumulated rest time
	 */
	public int getAccumulatedRestHours() {
		return toHours(accumulatedRestTime);
	}

	/**
	 * 返回分钟部分，例如 1 小时 32 分钟返回 32 分钟。
	 * Returns minutes part.<br> For instance: if time is 1 hr 32 min - it will return 32 min
	 *
	 * @return 累计休息时间的分钟部分 / minutes part of accumulated rest time
	 */
	public int getAccumulatedRestMinutes() {
		return toMinutes(accumulatedRestTime);
	}

	/**
	 * 将毫秒转换为小时。
	 * Converts milliseconds to hours.<br> For instance if millis = 1 hr 32 min, 1 hour will be returned.
	 */
	private static int toHours(long millis) {
		return (int) (millis / 1000) / 3600;
	}

	/**
	 * 将毫秒转换为分钟。
	 * Converts milliseconds to minutes.<br> For instance if millis = 1 hr 32 min, 32 min will be returned.
	 */
	private static int toMinutes(long millis) {
		return (int) ((millis / 1000) % 3600) / 60;
	}
}
