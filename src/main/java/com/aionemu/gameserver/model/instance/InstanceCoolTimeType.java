package com.aionemu.gameserver.model.instance;

/**
 * 副本 CoolTime 类型枚举。
 * Instance Cool Time Type enumeration.
 */

public enum InstanceCoolTimeType {
	/** 相对 / Relative. */
	RELATIVE, WEEKLY, DAILY;

	/**
	 * @return Whether relative
	 */
	public boolean isRelative() {
		return this.equals(InstanceCoolTimeType.RELATIVE);
	}

	/**
	 * @return Whether weekly
	 */
	public boolean isWeekly() {
		return this.equals(InstanceCoolTimeType.WEEKLY);
	}

	/**
	 * @return 是否为每日类型。 / Whether daily
	  */
	public boolean isDaily() {
		return this.equals(InstanceCoolTimeType.DAILY);
	}
}
