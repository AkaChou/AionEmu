package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.ProvokeTarget;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 挑衅/反制触发效果：在攻击或受击时按目标类型触发指定技能。
 * Provoker effect: on attack or being attacked, triggers a configured skill by target type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProvokerEffect")
public class ProvokerEffect extends ShieldEffect {

	@XmlAttribute(name = "provoke_target")
	protected ProvokeTarget provokeTarget;
	@XmlAttribute(name = "skill_id")
	protected int skillId;

	/**
	 * 将挑衅/反制效果加入控制器。
	 * Attaches the provoker effect to the controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 按配置注册攻击/受击触发观察者。
	 * Registers attack/attacked observers per configuration.
	 */
	@Override
	public void startEffect(Effect effect) {
		ActionObserver observer = null;
		final Creature effector = effect.getEffector();
		final int prob2 = this.hitTypeProb / 10;
		final int radius = this.radius;
		switch (this.hitType) {
		case NMLATK:
			observer = new ActionObserver(ObserverType.ATTACK) {
				@Override
				public void attack(Creature creature) {
					if (Rnd.get(0, 100) <= prob2) {
						Creature target = getProvokeTarget(provokeTarget, effector, creature);
						createProvokedEffect(effector, target);
					}
				}
			};
			break;
		case EVERYHIT:
			observer = new ActionObserver(ObserverType.ATTACKED) {
				@Override
				public void attacked(Creature creature) {
					if (radius > 0) {
						if (!MathUtil.isIn3dRange(effector, creature, radius)) {
							return;
						}
					}
					if (Rnd.get(0, 100) <= prob2) {
						Creature target = getProvokeTarget(provokeTarget, effector, creature);
						createProvokedEffect(effector, target);
					}
				}
			};
			break;
		case BACKATK:
			observer = new ActionObserver(ObserverType.ATTACKED) {
				@Override
				public void attacked(Creature creature) {
					if (Rnd.get(0, 100) <= prob2) {
						Creature target = getProvokeTarget(provokeTarget, effector, creature);
						createProvokedEffect(effector, target);
					}
				}
			};
			break;
		case PHHIT:
			observer = new ActionObserver(ObserverType.ATTACKED) {
				@Override
				public void attacked(Creature creature) {
					int physical = creature.getGameStats().getMainHandPAttack().getBase();
					int magical = creature.getGameStats().getMainHandMAttack().getBase();
					if (Rnd.get(0, 100) <= prob2) {
						if (physical > magical) {
							Creature target = getProvokeTarget(provokeTarget, effector, creature);
							createProvokedEffect(effector, target);
						}
					}
				}
			};
			break;
		case MAHIT:
			observer = new ActionObserver(ObserverType.ATTACKED) {
				@Override
				public void attacked(Creature creature) {
					int physical = creature.getGameStats().getMainHandPAttack().getBase();
					int magical = creature.getGameStats().getMainHandMAttack().getBase();
					if (Rnd.get(0, 100) <= prob2) {
						if (physical < magical) {
							Creature target = getProvokeTarget(provokeTarget, effector, creature);
							createProvokedEffect(effector, target);
						}
					}
				}
			};
			break;
		}
		if (observer == null) {
			return;
		}
		effect.setActionObserver(observer, position);
		effect.getEffected().getObserveController().addObserver(observer);
	}

	/**
	 * 创建被触发的技能效果实例。
	 * Creates the provoked skill effect instance.
	 */
	private void createProvokedEffect(Creature effector, Creature target) {
		GameEngineServices.skillEngine().applyEffectDirectly(skillId, effector, target, 0);
	}

	/**
	 * 解析挑衅技能的目标选择。
	 * Resolves the provoke skill target selection.
	 */
	private Creature getProvokeTarget(ProvokeTarget provokeTarget, Creature effector, Creature target) {
		switch (provokeTarget) {
		case ME:
			return effector;
		case OPPONENT:
			return target;
		}
		throw new IllegalArgumentException("Provoker target is invalid " + provokeTarget);
	}

	/**
	 * 移除挑衅触发观察者。
	 * Removes provoker trigger observers.
	 */
	@Override
	public void endEffect(Effect effect) {
		ActionObserver observer = effect.getActionObserver(position);
		if (observer != null) {
			effect.getEffected().getObserveController().removeObserver(observer);
		}
	}
}
