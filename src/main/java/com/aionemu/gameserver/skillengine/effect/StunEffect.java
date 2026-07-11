package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_IMMOBILIZE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 晕眩效果：使目标无法行动，打断技能并中止移动。
 * Stun effect: disables the target, cancelling skill and movement.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StunEffect")
public class StunEffect extends EffectTemplate {
	/**
	 * 目标非不可移动状态时加入效果控制器。
	 * Adds the effect when the target is not already cannot-move.
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (!effect.getEffected().getEffectController().isAbnormalSet(AbnormalState.CANNOT_MOVE)) {
			effect.addToEffectedController();
		}
	}

	/**
	 * 按晕眩抗性结算是否命中。
	 * Resolves hit chance against stun resistance.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.STUN_RESISTANCE, null);
	}

	/**
	 * 打断技能、中止移动，施加 STUN 并广播定身。
	 * Cancels skill, aborts move, applies STUN, and broadcasts immobilize.
	 */
	@Override
	public void startEffect(Effect effect) {
		final Creature effected = effect.getEffected();
		effected.getController().cancelCurrentSkill();
		effected.getMoveController().abortMove();
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.STUN.getId());
		effect.setAbnormal(AbnormalState.STUN.getId());
		PacketSendUtility.broadcastPacketAndReceive(effect.getEffected(),
				new SM_TARGET_IMMOBILIZE(effect.getEffected()));
	}

	/**
	 * 清除 STUN 异常。
	 * Clears the STUN abnormal.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.STUN.getId());
	}
}
