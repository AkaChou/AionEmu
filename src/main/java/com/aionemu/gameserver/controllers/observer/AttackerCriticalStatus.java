package com.aionemu.gameserver.controllers.observer;

import lombok.Data;

/**
 * 攻击方暴击状态数据：结果、剩余次数、数值与是否百分比。
 * Attacker critical status data: result, remaining count, value and percent flag.
 * @author kecimis
 */
@Data
public class AttackerCriticalStatus {
	/** 是否判定成功 / Whether the check succeeded */
	private boolean result = false;
	/** 剩余次数 / Remaining count */
	private int count;
	/** 暴击数值 / Critical value */
	private int value;
	/** 数值是否为百分比 / Whether value is percent-based */
	private boolean isPercent;

	/**
	 * 仅携带判定结果的构造。
	 * Constructor with result only.
	 * @param result 判定结果 / check result
	 */
	public AttackerCriticalStatus(boolean result) {
		this.result = result;
	}

	/**
	 * 携带次数与数值的构造。
	 * Constructor with count and value.
	 * @param count 剩余次数 / remaining count
	 * @param value 暴击数值 / critical value
	 * @param isPercent 是否百分比 / whether percent-based
	 */
	public AttackerCriticalStatus(int count, int value, boolean isPercent) {
		this.count = count;
		this.value = value;
		this.isPercent = isPercent;
	}
}
