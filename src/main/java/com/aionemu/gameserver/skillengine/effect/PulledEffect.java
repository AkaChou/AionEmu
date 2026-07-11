package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillMoveType;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 拉拽效果：将目标拉向施法者并限制其移动。
 * Pull effect: drags the target toward the caster and restricts movement.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PulledEffect")
public class PulledEffect extends EffectTemplate {

	/**
	 * 将效果应用到目标（加入控制器或立即结算）。
	 * Applies the effect to the target (controller attach or immediate settlement).
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
		final Creature effected = effect.getEffected();
		effected.setPulledMulti(0);
		effected.getController().cancelCurrentSkill();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(effected, effect.getTargetX(), effect.getTargetY(), effect.getTargetZ(),
				effected.getHeading());
		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_FORCED_MOVE(effect.getEffector(),
				effected.getObjectId(), effect.getTargetX(), effect.getTargetY(), effect.getTargetZ()));
	}

	/**
	 * 计算本效果是否成功命中/生效，并写入效果上下文。
	 * Calculates whether this effect succeeds and writes into the effect context.
	 */
	@Override
	public void calculate(Effect effect) {
		if (!super.calculate(effect, StatEnum.PULLED_RESISTANCE, null)) {
			return;
		}
		effect.setSkillMoveType(SkillMoveType.PULL);
		final Creature effector = effect.getEffector();
		double radian = Math.toRadians(MathUtil.convertHeadingToDegree(effector.getHeading()));
		final float x1 = (float) Math.cos(radian);
		final float y1 = (float) Math.sin(radian);
		effect.setTragetLoc(effector.getX() + x1, effector.getY() + y1, effector.getZ() + 0.25F);
	}

	/**
	 * 执行拉拽位移并限制目标移动。
	 * Performs the pull relocation and restricts target movement.
	 */
	@Override
	public void startEffect(Effect effect) {
		final Creature effected = effect.getEffected();
		effected.getEffectController().setAbnormal(AbnormalState.CANNOT_MOVE.getId());
		effect.setAbnormal(AbnormalState.CANNOT_MOVE.getId());
	}

	/**
	 * 结束拉拽并恢复可移动状态。
	 * Ends the pull and restores mobility.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().setPulledMulti(1);
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.CANNOT_MOVE.getId());
	}
}
