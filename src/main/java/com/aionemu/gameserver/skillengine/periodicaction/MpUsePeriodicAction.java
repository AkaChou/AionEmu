package com.aionemu.gameserver.skillengine.periodicaction;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 周期 MP 消耗：按固定值或最大 MP 百分比扣除受影响者 MP，不足则结束效果。
 * Periodic MP cost: reduces effected MP by a fixed value or max-MP percentage; ends effect if insufficient.
 *
 * @author antness
 */
public class MpUsePeriodicAction extends PeriodicAction {

	/**
	 * 消耗比例（最大 MP 的百分比）。
	 * Cost ratio as a percentage of max MP.
	 */
	@XmlAttribute(name = "value")
	protected int value;

	@XmlAttribute(name = "ratio")
	protected boolean ratio = true;

	/**
	 * 按最大 MP 百分比扣除 MP；不足时结束效果。
	 * Reduces MP by a percent of max MP; ends the effect if insufficient.
	 *
	 * related effect
	 */
	@Override
	public void act(Effect effect) {
		Creature effected = effect.getEffected();
		int requiredMp = requiredMp(effected.getGameStats().getMaxMp().getCurrent());
		if (effected.getLifeStats().getCurrentMp() < requiredMp) {
			effect.endEffect();
			return;
		}
		effected.getLifeStats().reduceMp(requiredMp);
	}

	int requiredMp(int maxMp) {
		return ratio ? (int) (maxMp * (value / 100f)) : value;
	}
}
