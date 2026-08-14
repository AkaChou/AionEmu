package com.aionemu.gameserver.network;

import java.sql.Timestamp;

/**
 * MAC 地址封禁条目，记录封禁地址、截止时间与备注。
 * MAC ban entry holding the banned address, end time and details.
 *
 * @author KID
 */
public class BannedMacEntry {
	private String mac, details;
	private Timestamp timeEnd;

	/**
	 * 以地址与截止时间（毫秒）构造条目。
	 * Creates an entry from address and end time in milliseconds.
	 *
	 * @param address 封禁的 MAC 地址 / banned MAC address
	 * @param newTime 截止时间戳（毫秒） / end timestamp in ms
	 */
	public BannedMacEntry(String address, long newTime) {
		this.mac = address;
		this.updateTime(newTime);
	}

	/**
	 * 以地址、截止时间与备注构造条目。
	 * Creates an entry from address, end timestamp and details.
	 *
	 * @param address 封禁的 MAC 地址 / banned MAC address
	 * @param time 截止时间 / end time
	 * @param details 封禁备注 / ban details
	 */
	public BannedMacEntry(String address, Timestamp time, String details) {
		this.mac = address;
		this.timeEnd = time;
		this.details = details;
	}

	/**
	 * 设置封禁备注。
	 * Sets ban details.
	 *
	 * @param details 封禁备注 / ban details
	 */
	public final void setDetails(String details) {
		this.details = details;
	}

	/**
	 * 更新封禁截止时间。
	 * Updates ban end time.
	 *
	 * @param newTime 截止时间戳（毫秒） / end timestamp in ms
	 */
	public final void updateTime(long newTime) {
		this.timeEnd = new Timestamp(newTime);
	}

	/**
	 * 获取封禁 MAC。
	 * Returns the banned MAC.
	 *
	 * @return 封禁的 MAC 地址 / banned MAC address
	 */
	public final String getMac() {
		return mac;
	}

	/**
	 * 获取封禁截止时间。
	 * Returns ban end time.
	 *
	 * @return 封禁截止时间 / ban end time
	 */
	public final Timestamp getTime() {
		return timeEnd;
	}

	/**
	 * 判断当前是否仍在封禁有效期内。
	 * Whether the ban is still active now.
	 *
	 * @return 封禁生效时为 {@code true} / {@code true} if active
	 */
	public final boolean isActive() {
		return timeEnd != null && timeEnd.getTime() > System.currentTimeMillis();
	}

	/**
	 * 判断在指定时间点是否仍处于封禁中。
	 * Whether the ban is still active at the given time.
	 *
	 * @param time 时间戳（毫秒） / timestamp in ms
	 * @return 封禁生效时为 {@code true} / {@code true} if active
	 */
	public final boolean isActiveTill(long time) {
		return timeEnd != null && timeEnd.getTime() > time;
	}

	/**
	 * 获取封禁备注。
	 * Returns ban details.
	 *
	 * @return 封禁备注 / ban details
	 */
	public final String getDetails() {
		return details;
	}
}
