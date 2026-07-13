package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillSubType;
import com.aionemu.gameserver.skillengine.model.SkillType;

/**
 * 魔法反制效果：目标施法时对攻击者造成反击伤害（有上限）。
 * Magic counter-attack: damages the attacker when the target uses a skill (capped).
 *
 * @author ViAl
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MagicCounterAtkEffect")
public class MagicCounterAtkEffect extends EffectTemplate {

	@XmlAttribute
	protected int maxdmg;

	/**
	 * 将魔法反制加入目标控制器。
	 * Attaches magic counter-attack to the controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 注册技能使用观察以执行反击。
	 * Registers skill-use observer to perform the counter.
	 */
	@Override
	public void startEffect(final Effect effect) {
		final Creature effector = effect.getEffector();
		final Creature effected = effect.getEffected();
		final CreatureLifeStats<? extends Creature> cls = effect.getEffected().getLifeStats();
		final int percent = value + delta * effect.getSkillLevel();

		ActionObserver observer = new ActionObserver(ObserverType.SKILLUSE) {

			public void skilluse(final Skill skill) {
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

					@Override
					public void run() {

						if (skill.getSkillTemplate().getType() == SkillType.MAGICAL
								&& skill.getSkillTemplate().getSubType() == SkillSubType.ATTACK) {
							if ((int) (cls.getMaxHp() / 100f * percent) <= maxdmg) {
								effected.getController().onAttack(effector, effect.getSkillId(), TYPE.DAMAGE,
										(int) (cls.getMaxHp() / 100f * percent), true, LOG.REGULAR);
							} else {
								effected.getController().onAttack(effector, maxdmg, true);
							}
						}
					}
				}, 0);
			}
		};

		effect.setActionObserver(observer, position);
		effected.getObserveController().addObserver(observer);
	}

	/**
	 * 移除魔法反制观察者。
	 * Removes the magic counter-attack observer.
	 */
	@Override
	public void endEffect(Effect effect) {
		ActionObserver observer = effect.getActionObserver(position);
		if (observer != null) {
			effect.getEffected().getObserveController().removeObserver(observer);
		}
	}
}
