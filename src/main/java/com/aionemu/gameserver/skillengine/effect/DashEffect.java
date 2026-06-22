/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.skillengine.effect;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

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
import com.aionemu.gameserver.world.geo.GeoService;

/**
 * @author ATracer
 * @modified 修复冲刺技能位置重叠问题，玩家不会直接落在目标身上
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DashEffect")
public class DashEffect extends DamageEffect {

	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect);
		final Player effector = (Player) effect.getEffector();

		// Move Effector to Effected
		Skill skill = effect.getSkill();
		World.getInstance().updatePosition(effector, skill.getX(), skill.getY(), skill.getZ(), skill.getH());
	}

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
		// 直接重叠会导致客户端无法自动攻击，需要停在目标前方一定距离
		byte newHeading = MathUtil.estimateHeadingFrom(effector, effected);
		float boundRadius = effector.getCollision() + effected.getCollision();
		float x1 = effector.getX(), y1 = effector.getY(), z1 = effector.getZ(),
			  x2 = effected.getX(), y2 = effected.getY(), z2 = effected.getZ(),
			  distance = (float) MathUtil.getDistance(x1, y1, z1, x2, y2, z2),
			  vx = (x1 - x2) * (boundRadius/distance),
			  vy = (y1 - y2) * (boundRadius/distance),
			  vz = (z1 - z2) * (boundRadius/distance);
		Vector3f pos = GeoService.getInstance().getClosestCollision(effected, x2 + vx, y2 + vy, z2 + vz, false, CollisionIntention.PHYSICAL.getId());
		
		effect.getSkill().setTargetPosition(pos.x, pos.y, pos.z, newHeading);
	}
}
