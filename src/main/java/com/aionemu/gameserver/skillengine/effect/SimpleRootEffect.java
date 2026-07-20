package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillMoveType;
import com.aionemu.gameserver.skillengine.model.SpellStatus;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.PositionUtil;

/**
 * 简易击退/定身效果：将目标小幅击退并施加击退异常。
 * Simple root/knockback effect: short knockback with KNOCKBACK abnormal.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SimpleRootEffect")
public class SimpleRootEffect extends EffectTemplate {
	/**
	 * 将效果加入目标的效果控制器。
	 * Adds this effect to the target effect controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 目标已有特定异常时失败，否则按简易击退抗性结算并计算后退落点。
	 * Fails if certain abnormals are present; otherwise checks simple-root resistance and calculates a move-away landing point.
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffected().getEffectController().hasAbnormalEffect(8224)
				|| effect.getEffected().getEffectController().hasAbnormalEffect(8678)) {
			return;
		}
		if (!super.calculate(effect, StatEnum.SIMPLE_ROOT_RESISTANCE, null)) {
			return;
		}
		effect.setSkillMoveType(SkillMoveType.KNOCKBACK);
		final Creature effector = effect.getEffector();
		final Creature effected = effect.getEffected();
		byte moveAwayHeading = PositionUtil.getMoveAwayHeading(effector, effected);
		Vector3f closestCollision = GameWorldServices.geoService().findMovementCollision(effected,
				MathUtil.convertHeadingToDegree(moveAwayHeading), 0.7f);
		effect.setTargetLoc(closestCollision.x, closestCollision.y, closestCollision.z);
	}

	/**
	 * 更新至预先计算的击退落点并广播强制移动。
	 * Moves to the precomputed knockback landing point and broadcasts forced movement.
	 */
	@Override
	public void startEffect(final Effect effect) {
		final Creature effected = effect.getEffected();
		effect.setSpellStatus(SpellStatus.NONE);
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.KNOCKBACK.getId());
		effect.setAbnormal(AbnormalState.KNOCKBACK.getId());
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(effected, effect.getTargetX(), effect.getTargetY(),
				effect.getTargetZ(), effected.getHeading(), false);
		PacketSendUtility.broadcastPacketAndReceive(effected,
				new SM_FORCED_MOVE(effect.getEffector(), effected.getObjectId(), effect.getTargetX(), effect.getTargetY(), effect.getTargetZ()));
	}

	/**
	 * 清除 KNOCKBACK 异常。
	 * Clears the KNOCKBACK abnormal.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.KNOCKBACK.getId());
	}
}
