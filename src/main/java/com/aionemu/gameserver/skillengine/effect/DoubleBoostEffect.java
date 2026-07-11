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
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRANSFORM;
import com.aionemu.gameserver.skillengine.model.DashStatus;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 双重推进位移效果：按方向/距离计算冲刺落点并更新施法者位置。
 * Double-boost dash effect: computes a directional dash landing and updates effector position.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DoubleBoostEffect")
public class DoubleBoostEffect extends EffectTemplate {
	@XmlAttribute(name = "distance")
	private float distance;
	@XmlAttribute(name = "direction")
	private float direction;

	/**
	 * 同步目标更新包并将施法者移动到技能坐标。
	 * Sends target-update and moves the effector to the skill position.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		final Player effector = (Player) effect.getEffector();
		PacketSendUtility.sendPacket(effector, new SM_TARGET_UPDATE(effector));
		Skill skill = effect.getSkill();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(effector, skill.getX(), skill.getY(), skill.getZ(), skill.getH());
	}

	/**
	 * 标记冲刺成功并按朝向/距离/碰撞计算落点。
	 * Marks dash success and computes landing by heading, distance, and collision.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
		effect.setDashStatus(DashStatus.DASH);
		final Player effector = (Player) effect.getEffector();
		double radian = Math.toRadians(MathUtil.convertHeadingToDegree(effector.getHeading()));
		float x1 = (float) (Math.cos(Math.PI * direction + radian) * distance);
		float y1 = (float) (Math.sin(Math.PI * direction + radian) * distance);
		effector.getEffectController().updatePlayerEffectIcons();
		PacketSendUtility.broadcastPacketAndReceive(effector, new SM_TRANSFORM(effector, true));
		PacketSendUtility.broadcastPacketAndReceive(effector,
				new SM_TRANSFORM(effector, effector.getTransformedModelId(), true, effector.getTransformedItemId()));
		byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
		Vector3f closestCollision = GameWorldServices.geoService().getClosestCollision(effector, effector.getX() + x1,
				effector.getY() + y1, effector.getZ(), false, intentions);
		effect.getSkill().setTargetPosition(closestCollision.getX(), closestCollision.getY(), closestCollision.getZ(),
				effector.getHeading());
	}
}
