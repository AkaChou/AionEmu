package com.aionemu.gameserver.skillengine.periodicaction;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 周期 HP 消耗：效果持续期间按间隔扣除受影响者 HP，不足则结束效果。
 * Periodic HP cost: reduces effected HP each tick; ends the effect if HP is too low.
 *
 * @author antness
 */
public class HpUsePeriodicAction extends PeriodicAction {

	/**
	 * 每次扣除的 HP 值。
	 * HP amount consumed per tick.
	 */
	@XmlAttribute(name = "value")
	protected int value;

	/**
	 * 等级相关增量（模板字段，当前实现未使用）。
	 * Level delta from template (unused by current implementation).
	 */
	@XmlAttribute(name = "delta")
	protected int delta;

	/**
	 * 扣除受影响者 HP；当前 HP 低于 value 时结束效果。
	 * Reduces effected HP; ends the effect if current HP is below value.
	 *
	 * related effect
	 */
	@Override
	public void act(Effect effect) {
		Creature effected = effect.getEffected();
		if (effected.getLifeStats().getCurrentHp() < value) {
			effect.endEffect();
			return;
		}
		effected.getLifeStats().reduceHp(value, effected);
	}
}
