package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillMoveType;
import com.aionemu.gameserver.skillengine.model.SpellStatus;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 踉跄失衡效果：将目标击退一段距离并施加 STAGGER 物理控制。
 * Stagger effect: knocks the target back and applies STAGGER physical control.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StaggerEffect")
public class StaggerEffect extends EffectTemplate {
	/**
	 * 非不可移动时加入控制器，结束滑翔，强制位移到计算落点。
	 * If movable, adds effect, ends glide, and forced-moves to the calculated landing.
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (!effect.getEffected().getEffectController().isAbnormalSet(AbnormalState.CANNOT_MOVE)) {
			effect.addToEffectedController();
			effect.setIsPhysicalState(true);
			final Creature effected = effect.getEffected();
			if (effected instanceof Player && effected.isInState(CreatureState.GLIDING)) {
				((Player) effected).getFlyController().endFly(true);
			}
			effected.getController().cancelCurrentSkill();
			effected.getEffectController().removeParalyzeEffects();
			effected.getMoveController().abortMove();
			PacketSendUtility.broadcastPacketAndReceive(effect.getEffected(), new SM_FORCED_MOVE(effect.getEffector(),
					effect.getEffected().getObjectId(), effect.getTargetX(), effect.getTargetY(), effect.getTargetZ()));
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(effected, effect.getTargetX(), effect.getTargetY(), effect.getTargetZ(),
					effected.getHeading());
		}
	}

	/**
	 * 施加 STAGGER 异常。
	 * Applies the STAGGER abnormal.
	 */
	@Override
	public void startEffect(Effect effect) {
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.STAGGER.getId());
		effect.setAbnormal(AbnormalState.STAGGER.getId());
	}

	/**
	 * 已有物理状态时失败；否则按踉跄抗性结算并计算击退坐标。
	 * Fails if a physical state exists; otherwise resolves stagger resistance and target location.
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffected().getEffectController().hasPhysicalStateEffect()) {
			return;
		}
		if (!super.calculate(effect, StatEnum.STAGGER_RESISTANCE, SpellStatus.STAGGER)) {
			return;
		}
		effect.setSkillMoveType(SkillMoveType.STAGGER);
		final Creature effector = effect.getEffector();
		final Creature effected = effect.getEffected();
		effected.getController().cancelCurrentSkill();
		double radian = Math.toRadians(MathUtil.convertHeadingToDegree(effector.getHeading()));
		float x1 = (float) (Math.cos(radian) * 3);
		float y1 = (float) (Math.sin(radian) * 3);
		float z = effected.getZ();
		byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
		Vector3f closestCollision = GameWorldServices.geoService().getClosestCollision(effected, effected.getX() + x1,
				effected.getY() + y1, effected.getZ(), true, intentions);
		x1 = closestCollision.x;
		y1 = closestCollision.y;
		z = closestCollision.z;
		effect.setTargetLoc(x1, y1, z);
	}

	/**
	 * 清除物理状态标记与 STAGGER 异常。
	 * Clears physical-state flag and STAGGER abnormal.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.setIsPhysicalState(false);
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.STAGGER.getId());
	}
}
