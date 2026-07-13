package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HitType;
import com.aionemu.gameserver.skillengine.model.ProvokeTarget;
import com.aionemu.gameserver.utils.PositionUtil;

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
		Creature caster = effect.getEffected();
		int probability = getHitTypeProbability(effect.getSkillLevel());
		int triggeredSkillLevel = getTriggeredSkillLevel(effect.getSkillLevel());
		ActionObserver observer;
		switch (this.hitType) {
		case NMLATK, BACKATK:
			observer = new ActionObserver(getTriggerObserverType()) {
				@Override
				public void attack(Creature opponent, int sourceSkillId) {
					if (hitType == HitType.BACKATK
							&& !PositionUtil.isBehindTarget(caster, opponent)) {
						return;
					}
					if (sourceSkillId > 0 && DataManager.SKILL_DATA.getSkillTemplate(sourceSkillId) != null
							&& DataManager.SKILL_DATA.getSkillTemplate(sourceSkillId).isProvoked()) {
						return;
					}
					trigger(caster, opponent, probability, triggeredSkillLevel);
				}
			};
			break;
		case EVERYHIT, PHHIT, MAHIT:
			observer = new ActionObserver(getTriggerObserverType()) {
				@Override
				public void attacked(Creature opponent, boolean magical) {
					if (!acceptsAttackedType(magical)) {
						return;
					}
					trigger(caster, opponent, probability, triggeredSkillLevel);
				}
			};
			break;
		default:
			return;
		}
		effect.setActionObserver(observer, position);
		effect.getEffected().getObserveController().addObserver(observer);
	}

	private void trigger(Creature caster, Creature opponent, int probability, int triggeredSkillLevel) {
		if (!matchesOpponent(caster, opponent) || probability <= 0 || Rnd.get(1000) >= probability) {
			return;
		}
		Creature target = getProvokeTarget(provokeTarget, caster, opponent);
		GameEngineServices.skillEngine().applyEffectDirectly(skillId, caster, target, 0, triggeredSkillLevel);
	}

	private boolean matchesOpponent(Creature caster, Creature opponent) {
		if (opponent == null || caster.getWorldId() != opponent.getWorldId()
				|| caster.getInstanceId() != opponent.getInstanceId() || condrace != null && opponent.getRace() != condrace) {
			return false;
		}
		float dx = opponent.getX() - caster.getX();
		float dy = opponent.getY() - caster.getY();
		float dz = opponent.getZ() - caster.getZ();
		int squaredDistance = (int) (dx * dx + dy * dy + dz * dz);
		return (minradius <= 0 || squaredDistance >= minradius * minradius)
			&& (radius <= 0 || squaredDistance <= radius * radius);
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

	public int getTriggeredSkillLevel(int effectSkillLevel) {
		return delta * effectSkillLevel + value;
	}

	ObserverType getTriggerObserverType() {
		return hitType == HitType.NMLATK || hitType == HitType.BACKATK ? ObserverType.ATTACK : ObserverType.ATTACKED;
	}

	boolean acceptsAttackedType(boolean magical) {
		return hitType == HitType.EVERYHIT || hitType == HitType.MAHIT && magical || hitType == HitType.PHHIT && !magical;
	}

	public int getTriggeredSkillId() {
		return skillId;
	}

	public ProvokeTarget getProvokeTarget() {
		return provokeTarget;
	}

	public Race getTriggerRace() {
		return condrace;
	}

	public int getMinRadius() {
		return minradius;
	}

	public int getRadius() {
		return radius;
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
