package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 回城效果：将施法者传送至绑定点（灵魂绑定位置）。
 * Return effect: teleports the effector to their bind location.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReturnEffect")

public class ReturnEffect extends EffectTemplate {
	/**
	 * 若在副本中则先触发离本逻辑，再传送到绑定点。
	 * Leaves the instance if needed, then moves the player to bind location.
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (effect.getEffected().isInInstance()) {
			InstanceService.onLeaveInstance((Player) effect.getEffector());
		}
		TeleportService2.moveToBindLocation((Player) effect.getEffector(), true);
	}

	/**
	 * 目标已生成时标记本效果成功。
	 * Marks this effect successful when the target is spawned.
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffected().isSpawned()) {
			effect.addSucessEffect(this);
		}
	}
}
