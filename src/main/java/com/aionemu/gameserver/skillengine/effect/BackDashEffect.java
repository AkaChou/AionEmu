package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_UPDATE;
import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.model.DashStatus;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 后撤斩击效果：造成物理伤害并沿朝向后方位移。
 * Back-dash effect: deals physical damage and moves the caster backward.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BackDashEffect")
public class BackDashEffect extends DamageEffect {
	@XmlAttribute(name = "distance")
	private float distance;

	private float direction = 1;

	/**
	 * 应用伤害并更新施法者坐标。
	 * Applies damage and updates the effector position.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect);
		final Player effector = (Player) effect.getEffector();
		PacketSendUtility.sendPacket(effector, new SM_TARGET_UPDATE(effector));
		Skill skill = effect.getSkill();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(effector, skill.getX(), skill.getY(), skill.getZ(), skill.getH());
	}

	/**
	 * 计算物理伤害与后撤落点（含碰撞检测）。
	 * Calculates physical damage and back-dash landing point (with collision checks).
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		if (!super.calculate(effect, DamageType.PHYSICAL)) {
			return;
		}
		effect.setDashStatus(DashStatus.BACKDASH);
		final Player effector = (Player) effect.getEffector();
		double radian = Math.toRadians(MathUtil.convertHeadingToDegree(effector.getHeading()));
		float x1 = (float) (Math.cos(Math.PI * direction + radian) * distance);
		float y1 = (float) (Math.sin(Math.PI * direction + radian) * distance);
		byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
		Vector3f closestCollision = GameWorldServices.geoService().getClosestCollision(effector, effector.getX() + x1,
				effector.getY() + y1, effector.getZ(), false, intentions);
		effect.getSkill().setTargetPosition(closestCollision.getX(), closestCollision.getY(), closestCollision.getZ(),
				effector.getHeading());
	}
}
