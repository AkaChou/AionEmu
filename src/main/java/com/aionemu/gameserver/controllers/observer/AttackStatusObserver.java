package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.controllers.attack.AttackStatus;
import lombok.AllArgsConstructor;

/**
 * 攻击状态观察者基类，携带数值与目标攻击状态。
 * Base observer for attack status, holding a value and target attack status.
 * @author ATracer
 */
@AllArgsConstructor
public class AttackStatusObserver extends AttackCalcObserver {

	/** 关联数值（概率、倍率等） / Associated value (chance, multiplier, etc.) */
	protected int value;
	/** 目标攻击状态 / Target attack status */
	protected AttackStatus status;
}
