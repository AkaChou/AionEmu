package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillMoveType;
import com.aionemu.gameserver.skillengine.model.SpellStatus;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

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
	 * 目标已有特定异常时失败，否则按踉跄抗性结算。
	 * Fails if certain abnormals are present; otherwise uses stagger resistance.
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffected().getEffectController().hasAbnormalEffect(8224)
				|| effect.getEffected().getEffectController().hasAbnormalEffect(8678)) {
			return;
		}
		super.calculate(effect, StatEnum.STAGGER_RESISTANCE, null);
	}

	/**
	 * 计算击退落点，更新位置并广播强制移动。
	 * Computes knockback landing, updates position, and broadcasts forced move.
	 */
	@Override
	public void startEffect(final Effect effect) {
		final Creature effected = effect.getEffected();
		final Creature effector = effect.getEffector();
		byte heading = effect.getEffector().getHeading();
		effect.setSpellStatus(SpellStatus.NONE);
		effect.setSkillMoveType(SkillMoveType.KNOCKBACK);
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.KNOCKBACK.getId());
		effect.setAbnormal(AbnormalState.KNOCKBACK.getId());
		double radian = Math.toRadians(MathUtil.convertHeadingToDegree(heading));
		float x1 = (float) (Math.cos(radian) * 0.7f);
		float y1 = (float) (Math.sin(radian) * 0.7f);
		float z = effected.getZ();
		byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
		Vector3f closestCollision = GameWorldServices.geoService().getClosestCollision(effected, effector.getX() + x1,
				effector.getY() + y1, effected.getZ() - 0.4f, false, intentions);
		x1 = closestCollision.x;
		y1 = closestCollision.y;
		z = closestCollision.z;
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(effected, x1, y1, z, heading, false);
		PacketSendUtility.broadcastPacketAndReceive(effected,
				new SM_FORCED_MOVE(effect.getEffector(), effected.getObjectId(), x1, y1, z));
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
