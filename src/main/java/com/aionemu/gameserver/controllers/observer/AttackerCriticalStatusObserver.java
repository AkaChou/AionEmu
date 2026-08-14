package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.controllers.attack.AttackStatus;

/**
 * 攻击方暴击状态观察者，跟踪剩余次数与暴击配置。
 * Attacker critical-status observer tracking remaining count and critical config.
 */
public class AttackerCriticalStatusObserver extends AttackCalcObserver {

	/** 攻击方暴击状态数据 / Attacker critical status data */
	protected AttackerCriticalStatus acStatus = null;
	/** 关联攻击状态 / Associated attack status */
	protected AttackStatus status;

	/**
	 * @param status 攻击状态 / attack status
	 * @param count 剩余次数 / remaining count
	 * @param value 暴击数值 / critical value
	 * @param isPercent 是否百分比 / whether percent-based
	 */
	public AttackerCriticalStatusObserver(AttackStatus status, int count, int value, boolean isPercent) {
		this.status = status;
		this.acStatus = new AttackerCriticalStatus(count, value, isPercent);
	}

	/**
	 * 获取剩余次数。
	 * Get remaining count.
	 *
	 * @return 剩余次数 / remaining count
	 */
	public int getCount() {
		return acStatus.getCount();
	}

	/**
	 * 剩余次数减一。
	 * Decrease remaining count by one.
	 */
	public void decreaseCount() {
		acStatus.setCount((acStatus.getCount() - 1));
	}
}
