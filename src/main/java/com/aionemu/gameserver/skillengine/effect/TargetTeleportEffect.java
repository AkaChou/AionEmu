package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 目标传送效果：将玩家目标传送到施法者前方。
 * Target teleport effect: teleports a player target in front of the effector.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TargetTeleportEffect")
public class TargetTeleportEffect extends EffectTemplate {

	@XmlAttribute(name = "same_map")
	protected boolean isSameMap;
	@XmlAttribute
	protected int distance;

	@Override
	public void applyEffect(Effect effect) {
		if (!isSameMap || !(effect.getEffected() instanceof Player)) {
			return; // Cross-map destinations are skill/instance-specific and must be handled by their scripts.
		}
		Creature effector = effect.getEffector();
		Player effected = (Player) effect.getEffected();
		double radian = Math.toRadians(MathUtil.convertHeadingToDegree(effector.getHeading()));
		float targetX = effector.getX() + (float) Math.cos(radian) * distance;
		float targetY = effector.getY() + (float) Math.sin(radian) * distance;
		byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
		Vector3f destination = GameWorldServices.geoService().getClosestCollision(effected, targetX, targetY,
				effector.getZ(), false, intentions);
		TeleportService2.teleportTo(effected, effected.getWorldId(), effected.getInstanceId(), destination.getX(),
				destination.getY(), destination.getZ(), effected.getHeading(), TeleportAnimation.NO_ANIMATION);
	}
}
