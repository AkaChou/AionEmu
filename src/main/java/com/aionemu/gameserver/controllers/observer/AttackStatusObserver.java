package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.controllers.attack.AttackStatus;

/**
 * 攻击状态观察者基类，携带数值与目标攻击状态。
 * Base observer for attack status, holding a value and target attack status.
 *
 * @author ATracer
 */
public class AttackStatusObserver extends AttackCalcObserver {

	/** 关联数值（概率、倍率等） / Associated value (chance, multiplier, etc.) */
	protected int value;
	/** 目标攻击状态 / Target attack status */
	protected AttackStatus status;

	/**
	 * @param value 关联数值 / associated value
	 * @param status 攻击状态 / attack status
	 */
	public AttackStatusObserver(int value, AttackStatus status) {
		this.value = value;
		this.status = status;
	}
}
