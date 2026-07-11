package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 逃脱效果：离开副本（若在其中）并传送回绑定点。
 * Escape effect: leaves the instance if inside and teleports to bind location.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EscapeEffect")
public class EscapeEffect extends EffectTemplate {
	/**
	 * 处理副本离开并传送回绑定点。
	 * Handles instance leave and teleports to bind location.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (effect.getEffected().isInInstance()) {
			InstanceService.onLeaveInstance((Player) effect.getEffector());
		}
		TeleportService2.moveToBindLocation((Player) effect.getEffector(), true);
	}

	/**
	 * 目标已生成时标记效果成功。
	 * Marks success when the target is spawned.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffected().isSpawned()) {
			effect.addSucessEffect(this);
		}
	}
}
