package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.model.DashStatus;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.World;

/**
 * 冲刺效果：对目标造成物理伤害，并将施法者位移到目标附近（避免完全重叠）。
 * Dash effect: deals physical damage and relocates the effector near the target (avoids full overlap).
 *
 * @author ATracer
 * @modified 修复冲刺技能位置重叠问题，玩家不会直接落在目标身上
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DashEffect")
public class DashEffect extends DamageEffect {

	/**
	 * 结算伤害并将施法者移动到技能目标坐标。
	 * Applies damage and moves the effector to the skill target position.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect);
		final Player effector = (Player) effect.getEffector();

		// 将施法者移至受影响者 / Move Effector to Effected
		Skill skill = effect.getSkill();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(effector, skill.getX(), skill.getY(), skill.getZ(), skill.getH());
	}

	/**
	 * 计算物理伤害与冲刺落点（按碰撞半径偏移，避免叠体）。
	 * Calculates physical damage and dash landing offset by collision radii.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffected() == null) {
			return;
		}
		if (!(effect.getEffector() instanceof Player)) {
			return;
		}

		if (!super.calculate(effect, DamageType.PHYSICAL)) {
			return;
		}
		
		Creature effector = effect.getEffector();
		Creature effected = effect.getEffected();
		effect.setDashStatus(DashStatus.DASH);
		
		// 修复：计算偏移位置，避免玩家直接落在目标身上
		// Fix: calculate the offset position so the player does not land directly on the target.
		// 直接重叠会导致客户端无法自动攻击，需要停在目标前方一定距离
		// Full overlap prevents the client from auto-attacking, so the player stops a short distance in front of the target.
		byte newHeading = MathUtil.estimateHeadingFrom(effector, effected);
		float boundRadius = effector.getCollision() + effected.getCollision();
		float x1 = effector.getX(), y1 = effector.getY(), z1 = effector.getZ(),
			  x2 = effected.getX(), y2 = effected.getY(), z2 = effected.getZ(),
			  distance = (float) MathUtil.getDistance(x1, y1, z1, x2, y2, z2),
			  vx = (x1 - x2) * (boundRadius/distance),
			  vy = (y1 - y2) * (boundRadius/distance),
			  vz = (z1 - z2) * (boundRadius/distance);
		Vector3f pos = GameWorldServices.geoService().getClosestCollision(effected, x2 + vx, y2 + vy, z2 + vz, false, CollisionIntention.PHYSICAL.getId());
		
		effect.getSkill().setTargetPosition(pos.x, pos.y, pos.z, newHeading);
	}
}
