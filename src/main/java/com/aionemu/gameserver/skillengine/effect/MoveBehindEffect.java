package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_UPDATE;
import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.model.DashStatus;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillMoveType;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 绕后位移伤害效果：将施法者移动到目标身后并结算伤害。
 * Move-behind damage effect: relocates the caster behind the target and deals damage.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MoveBehindEffect")
public class MoveBehindEffect extends DamageEffect {
	/**
	 * 应用绕后伤害结算。
	 * Applies move-behind damage settlement.
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect);
	}

	/**
	 * 计算绕后位移坐标与伤害。
	 * Calculates behind-target position and damage.
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffected() == null) {
			return;
		}
		if (!(effect.getEffector() instanceof Player)) {
			return;
		}
		final Player effector = (Player) effect.getEffector();
		if (effect.getSkill().getSkillId() == 11333) { // 쇠갈고리 [Mirash Sanctuary]
			effect.setDashStatus(DashStatus.MOVEBEHIND);
			effect.setSkillMoveType(SkillMoveType.MOVEBEHIND);
		}
		final Creature effected = effect.getEffected();
		double radian = Math.toRadians(MathUtil.convertHeadingToDegree(effected.getHeading()));
		float x1 = (float) (Math.cos(Math.PI + radian) * 1.3F);
		float y1 = (float) (Math.sin(Math.PI + radian) * 1.3F);
		float z = GameWorldServices.geoService().getZAfterMoveBehind(effected.getWorldId(), effected.getX() + x1,
				effected.getY() + y1, effected.getZ(), effected.getInstanceId());
		byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
		Vector3f closestCollision = GameWorldServices.geoService().getClosestCollision(effector, effected.getX() + x1,
				effected.getY() + y1, z, false, intentions);
		effected.getMoveController().abortMove();
		PacketSendUtility.sendPacket(effector, new SM_TARGET_UPDATE(effector));
		effect.setDashStatus(DashStatus.MOVEBEHIND);
		effect.setSkillMoveType(SkillMoveType.MOVEBEHIND);
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(effector, closestCollision.getX(), closestCollision.getY(),
				closestCollision.getZ(), effected.getHeading());
		effect.getSkill().setTargetPosition(closestCollision.getX(), closestCollision.getY(), closestCollision.getZ(),
				effected.getHeading());
		if (!super.calculate(effect, DamageType.PHYSICAL)) {
			return;
		}
	}
}
