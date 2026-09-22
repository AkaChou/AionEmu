package com.aionemu.gameserver.skillengine.model;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.condition.Conditions;

/**
 * 运行时效果的终止观察者域。
 * Termination-observer domain for a runtime effect.
 * <p>负责在装备条件失效或受到伤害时结束 {@link Effect}，并在效果结束时注销观察者。
 * Owns the observers that end an {@link Effect} when equipment conditions fail or damage is received,
 * and unregisters them when the effect ends.</p>
 */
final class EffectTerminationObservers {

	private final Effect effect;
	private ActionObserver equipmentObserver;
	private ActionObserver attackedObserver;
	private ActionObserver dotAttackedObserver;

	/**
	 * 绑定宿主效果。
	 * Binds the hosting effect.
	 * @param effect 宿主效果 / hosting effect
	 */
	EffectTerminationObservers(Effect effect) {
		this.effect = effect;
	}

	/**
	 * 安装全部终止观察者。
	 * Attaches all termination observers.
	 */
	void attach() {
		attachEquipmentObserver();
		attachCancelOnDamageObservers();
	}

	/**
	 * 注销全部终止观察者，并合并清理异常。
	 * Unregisters all termination observers and merges cleanup failures.
	 * @param failure 既有异常 / existing failure
	 * @return 合并后的异常 / merged failure
	 */
	Throwable clear(Throwable failure) {
		ActionObserver equipment = equipmentObserver;
		ActionObserver attacked = attackedObserver;
		ActionObserver dotAttacked = dotAttackedObserver;
		equipmentObserver = null;
		attackedObserver = null;
		dotAttackedObserver = null;
		for (ActionObserver observer : new ActionObserver[] { equipment, attacked, dotAttacked }) {
			try {
				effect.getEffected().getObserveController().removeObserver(observer);
			} catch (RuntimeException | Error observerFailure) {
				failure = collectFailure(failure, observerFailure);
			}
		}
		return failure;
	}

	private void attachEquipmentObserver() {
		Conditions useEquipConditions = effect.getSkillTemplate().getUseEquipmentconditions();
		if (useEquipConditions != null && !useEquipConditions.getConditions().isEmpty()) {
			equipmentObserver = new ActionObserver(ObserverType.UNEQUIP) {

				@Override
				public void unequip(Item item, Player owner) {
					if (!useEquipmentConditionsCheck()) {
						effect.endEffect();
					}
				}
			};
			effect.getEffected().getObserveController().addObserver(equipmentObserver);
		}
	}

	private void attachCancelOnDamageObservers() {
		if (!effect.isCancelOnDmg()) {
			return;
		}
		attackedObserver = new ActionObserver(ObserverType.ATTACKED) {

			@Override
			public void attacked(Creature creature) {
				effect.getEffected().getEffectController().removeEffect(effect.getSkillId());
			}
		};
		effect.getEffected().getObserveController().attach(attackedObserver);

		dotAttackedObserver = new ActionObserver(ObserverType.DOT_ATTACKED) {

			@Override
			public void dotattacked(Creature creature, Effect dotEffect) {
				effect.getEffected().getEffectController().removeEffect(effect.getSkillId());
			}
		};
		effect.getEffected().getObserveController().attach(dotAttackedObserver);
	}

	private boolean useEquipmentConditionsCheck() {
		Conditions useEquipConditions = effect.getSkillTemplate().getUseEquipmentconditions();
		return useEquipConditions == null || useEquipConditions.validate(effect);
	}

	private static Throwable collectFailure(Throwable failure, Throwable nextFailure) {
		if (failure == null) {
			return nextFailure;
		}
		if (failure != nextFailure) {
			failure.addSuppressed(nextFailure);
		}
		return failure;
	}
}
