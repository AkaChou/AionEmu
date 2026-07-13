package com.aionemu.gameserver.skillengine.effect;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CaseStatUpEffect")
public class CaseStatUpEffect extends EffectTemplate {

	@XmlAttribute(name = "hp_min")
	private int hpMin;
	@XmlAttribute(name = "hp_max")
	private int hpMax;
	@XmlElement(name = "case_change")
	private List<CaseStatChange> changes;

	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	@Override
	public void startEffect(Effect effect) {
		if (changes == null || changes.isEmpty()) {
			return;
		}
		List<IStatFunction> modifiers = new ArrayList<>(changes.size());
		for (CaseStatChange change : changes) {
			modifiers.add(new HpScaledStatFunction(change, hpMin, hpMax));
		}
		effect.getEffected().getGameStats().addEffect(effect, modifiers);
	}

	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getGameStats().endEffect(effect);
	}

	static int interpolate(int currentHp, int maxHp, int hpMin, int hpMax, int valueLowHp, int valueHighHp) {
		if (maxHp <= 0) {
			return valueHighHp;
		}
		float hpPercent = currentHp * 100f / maxHp;
		float scale = hpPercent < hpMin ? 1 : hpPercent > hpMax ? 0 : 1 - (hpPercent - hpMin) / (hpMax - hpMin);
		return (int) ((valueLowHp - valueHighHp) * scale + valueHighHp);
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class CaseStatChange {

		@XmlAttribute(required = true)
		private StatEnum stat;
		@XmlAttribute(name = "value_low_hp", required = true)
		private int valueLowHp;
		@XmlAttribute(name = "value_high_hp", required = true)
		private int valueHighHp;
		@XmlAttribute
		private boolean percent;
	}

	private static class HpScaledStatFunction extends StatFunction {

		private final int valueLowHp;
		private final int valueHighHp;
		private final int hpMin;
		private final int hpMax;
		private final boolean percent;

		HpScaledStatFunction(CaseStatChange change, int hpMin, int hpMax) {
			super(change.stat, 0, true);
			this.valueLowHp = change.valueLowHp;
			this.valueHighHp = change.valueHighHp;
			this.hpMin = hpMin;
			this.hpMax = hpMax;
			this.percent = change.percent;
		}

		@Override
		public void apply(Stat2 stat) {
			Creature owner = stat.getOwner();
			int value = interpolate(owner.getLifeStats().getCurrentHp(), owner.getLifeStats().getMaxHp(),
				hpMin, hpMax, valueLowHp, valueHighHp);
			stat.addToBonus(percent ? stat.getBase() * value / 100f : value);
		}

		@Override
		public int getPriority() {
			return percent ? 40 : 50;
		}
	}
}
