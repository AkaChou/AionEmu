package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_IMMOBILIZE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 定身效果：禁止目标移动，普通受伤时解除，持续伤害是否解除由技能配置控制。
 * Root effect: immobilizes the target; direct damage breaks it, while periodic damage is configurable.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RootEffect")
public class RootEffect extends EffectTemplate {
	@XmlAttribute
	protected int resistchance = 100;

	/**
	 * 将效果加入目标的效果控制器。
	 * Adds this effect to the target effect controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 按定身抗性结算是否命中。
	 * Resolves hit chance against root resistance.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.ROOT_RESISTANCE, null);
	}

	/**
	 * 中止移动与当前技能，施加 ROOT 异常，并注册伤害解除观察者。
	 *
	 * @param effect Aborts move / skill, applies ROOT abnormal, and registers the damage observer.
	 */
	@Override
	public void startEffect(final Effect effect) {
		final Creature effected = effect.getEffected();
		if (effected.isInState(CreatureState.RESTING)) {
			effected.unsetState(CreatureState.RESTING);
		}
		if (!(effected instanceof Npc)) {
			effected.getMoveController().abortMove();
		}
		effected.getController().cancelCurrentSkill();
		effected.getEffectController().setAbnormal(AbnormalState.ROOT.getId());
		effect.setAbnormal(AbnormalState.ROOT.getId());
		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_TARGET_IMMOBILIZE(effected));
		ActionObserver observer = new ActionObserver(
				SkillConfig.ROOT_BREAK_ON_DOT ? ObserverType.ATTACKED_OR_DOT : ObserverType.ATTACKED) {
			private void removeRoot() {
				effected.getEffectController().removeEffect(effect.getSkillId());
			}

			@Override
			public void attacked(Creature creature) {
				removeRoot();
			}

			@Override
			public void dotattacked(Creature creature, Effect dotEffect) {
				removeRoot();
			}
		};
		effected.getObserveController().addObserver(observer);
		effect.setActionObserver(observer, position);
	}

	/**
	 * 清除 ROOT 异常并移除受击观察者。
	 * Clears the ROOT abnormal and removes the attacked observer.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.ROOT.getId());
		ActionObserver observer = effect.getActionObserver(position);
		if (observer != null) {
			effect.getEffected().getObserveController().removeObserver(observer);
		}
	}
}
