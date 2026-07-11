package com.aionemu.gameserver.skillengine.periodicaction;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 周期动作基类：效果持续期间按间隔执行的动作。
 * Base periodic action: action executed on an interval while an effect lasts.
 *
 * @author antness
 */
public abstract class PeriodicAction {

	/**
	 * 在效果上执行一次周期动作。
	 * Executes one periodic tick for the effect.
	 *
	 * related effect
	 */
	public abstract void act(Effect effect);
}
