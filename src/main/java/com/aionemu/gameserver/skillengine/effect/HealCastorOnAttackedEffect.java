package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 受击回施法者效果：目标被攻击时按配置治疗施法者。
 * Heal castor on attacked: heals the caster when the target is hit.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HealCastorOnAttackedEffect")
public class HealCastorOnAttackedEffect extends EffectTemplate {

	@XmlAttribute
	protected HealType type;
	@XmlAttribute
	protected float range;

	/**
	 * 将受击回血效果加入控制器。
	 * Attaches heal-on-attacked to the controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 计算受击回施法者效果。
	 * Calculates heal-castor-on-attacked.
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffected() instanceof Player) {
			super.calculate(effect, null, null);
		}
	}

	/**
	 * 注册受击观察以治疗施法者。
	 * Registers attacked observer to heal the caster.
	 */
	@Override
	public void startEffect(final Effect effect) {
		super.startEffect(effect);

		final Player player = (Player) effect.getEffector();
		final int valueWithDelta = value + delta * effect.getSkillLevel();

		ActionObserver observer = new ActionObserver(ObserverType.ATTACKED) {

			@Override
			public void attacked(Creature creature) {
				if (player.getPlayerGroup2() != null) {
					for (Player p : player.getPlayerGroup2().getMembers()) {
						if (MathUtil.isIn3dRange(effect.getEffected(), p, range)) {
							p.getController().onRestore(type, valueWithDelta);
						}
					}
				} else if (player.isInAlliance2()) {
					for (Player p : player.getPlayerAllianceGroup2().getMembers()) {
						if (!p.isOnline()) {
							continue;
						}
						if (MathUtil.isIn3dRange(effect.getEffected(), p, range)) {
							p.getController().onRestore(type, valueWithDelta);
						}
					}
				} else {
					if (MathUtil.isIn3dRange(effect.getEffected(), player, range)) {
						player.getController().onRestore(type, valueWithDelta);
					}
				}
			}
		};
		effect.getEffected().getObserveController().addObserver(observer);
		effect.setActionObserver(observer, position);
	}

	/**
	 * 移除受击回血观察者。
	 * Removes the heal-on-attacked observer.
	 */
	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect);
		ActionObserver observer = effect.getActionObserver(position);
		if (observer != null) {
			effect.getEffected().getObserveController().removeObserver(observer);
		}
	}
}
