package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 条件技能效果：当目标生命值（HP/MP）降至阈值以下且概率命中时，对目标施放指定技能（仅触发一次）。
 * Conditional skill effect: casts the specified skill on the target when its HP/MP drops below the threshold and the probability check succeeds (triggers only once).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CaseSkillEffect")
public class CaseSkillEffect extends EffectTemplate {

	@XmlAttribute(name = "skill_id")
	private int skillId;
	@XmlAttribute
	private HealType type;
	@XmlAttribute
	private int threshold;
	@XmlAttribute
	private int probability = 100;

	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	@Override
	public void startEffect(Effect effect) {
		if (type != HealType.HP && type != HealType.MP) {
			return;
		}
		ActionObserver observer = new ActionObserver(ObserverType.LIFE_CHANGED) {
			private boolean triggered;

			@Override
			public synchronized void lifeChanged(HealType changedType, int currentValue) {
				if (triggered || changedType != type) {
					return;
				}
				int maxValue = type == HealType.HP ? effect.getEffected().getLifeStats().getMaxHp()
					: effect.getEffected().getLifeStats().getMaxMp();
				if (currentValue <= maxValue * threshold / 100f && Rnd.chance(probability)) {
					triggered = true;
					GameEngineServices.skillEngine().applyEffect(skillId, effect.getEffected(), effect.getEffected());
				}
			}
		};
		effect.getEffected().getObserveController().addObserver(observer);
		effect.setActionObserver(observer, position);
		int currentValue = type == HealType.HP ? effect.getEffected().getLifeStats().getCurrentHp()
			: effect.getEffected().getLifeStats().getCurrentMp();
		observer.lifeChanged(type, currentValue);
	}

	@Override
	public void endEffect(Effect effect) {
		ActionObserver observer = effect.getActionObserver(position);
		if (observer != null) {
			effect.getEffected().getObserveController().removeObserver(observer);
		}
	}
}
