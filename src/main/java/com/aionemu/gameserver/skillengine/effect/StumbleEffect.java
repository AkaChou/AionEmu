package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
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
 * 踉跄效果：短距击退并施加 STUMBLE 物理控制。
 * Stumble effect: short knockback with STUMBLE physical control.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StumbleEffect")
public class StumbleEffect extends EffectTemplate {
	/**
	 * 非浮空/不可移动时加入控制器，结束滑翔并强制位移。
	 * If not open-aerial/immobile, adds effect, ends glide, and forced-moves.
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (!effect.getEffected().getEffectController().isAbnormalSet(AbnormalState.OPENAERIAL)
				&& !effect.getEffected().getEffectController().isAbnormalSet(AbnormalState.CANNOT_MOVE)) {
			effect.addToEffectedController();
			effect.setIsPhysicalState(true);
			final Creature effected = effect.getEffected();
			if (effected instanceof Player && effected.isInState(CreatureState.GLIDING)) {
				((Player) effected).getFlyController().endFly(true);
			}
			effected.getController().cancelCurrentSkill();
			effected.getEffectController().removeParalyzeEffects();
			if (!(effected instanceof Npc)) {
				effected.getMoveController().abortMove();
			}
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(effected, effect.getTargetX(), effect.getTargetY(), effect.getTargetZ(),
					effected.getHeading());
			PacketSendUtility.broadcastPacketAndReceive(effect.getEffected(), new SM_FORCED_MOVE(effect.getEffector(),
					effect.getEffected().getObjectId(), effect.getTargetX(), effect.getTargetY(), effect.getTargetZ()));
		}
	}

	/**
	 * 清除浮空异常并施加 STUMBLE。
	 * Clears open-aerial if set and applies STUMBLE abnormal.
	 */
	@Override
	public void startEffect(Effect effect) {
		if (effect.getEffected().getEffectController().isAbnormalSet(AbnormalState.OPENAERIAL)) {
			effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.OPENAERIAL.getId());
		}
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.STUMBLE.getId());
		effect.setAbnormal(AbnormalState.STUMBLE.getId());
	}

	/**
	 * 已有物理状态时失败；否则按踉跄抗性结算并计算击退坐标。
	 * Fails if a physical state exists; otherwise resolves stumble resistance and target location.
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffected().getEffectController().hasPhysicalStateEffect()) {
			return;
		}
		if (!super.calculate(effect, StatEnum.STUMBLE_RESISTANCE, SpellStatus.STUMBLE)) {
			return;
		}
		effect.setSkillMoveType(SkillMoveType.STUMBLE);
		final Creature effector = effect.getEffector();
		final Creature effected = effect.getEffected();
		effected.getController().cancelCurrentSkill();
		float direction = effected instanceof Player ? 1.5f : 0.5f;
		double radian = Math.toRadians(MathUtil.convertHeadingToDegree(effector.getHeading()));
		float x1 = (float) (Math.cos(radian) * direction);
		float y1 = (float) (Math.sin(radian) * direction);
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
	 * 清除物理状态标记与 STUMBLE 异常。
	 * Clears physical-state flag and STUMBLE abnormal.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.setIsPhysicalState(false);
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.STUMBLE.getId());
	}
}
