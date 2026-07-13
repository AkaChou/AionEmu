package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 技能消耗降低效果：注册技能使用观察者，对后续技能设置消耗加成值。
 * Skill-cost boost effect: registers a skill-use observer that applies a cost boost value.
 *
 * @author Rama and Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BoostSkillCostEffect")
public class BoostSkillCostEffect extends BuffEffect {

	@XmlAttribute
	protected boolean percent;

	/**
	 * 启动效果并挂载技能使用观察者。
	 * Starts the effect and attaches a skill-use observer.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		super.startEffect(effect);

		ActionObserver observer = new ActionObserver(ObserverType.SKILLUSE) {

			@Override
			public void skilluse(Skill skill) {
				skill.setBoostSkillCost(value + delta * effect.getSkillLevel());
			}
		};
		effect.getEffected().getObserveController().addObserver(observer);
		effect.setActionObserver(observer, position);
	}

	/**
	 * 结束效果并移除技能使用观察者。
	 * Ends the effect and removes the skill-use observer.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect);
		ActionObserver observer = effect.getActionObserver(position);
		effect.getEffected().getObserveController().removeObserver(observer);
	}
}
