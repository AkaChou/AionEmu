package com.aionemu.gameserver.ai.siege;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 攻城战相关 NPC AI：Quartermaster（@AIName "quartermaster"），继承 GeneralNpcAI2。
 * Siege-related NPC AI: Quartermaster (@AIName "quartermaster"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("quartermaster")
public class QuartermasterAI2 extends GeneralNpcAI2
{
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 5) {
				if (startedEvent.compareAndSet(false, true)) {
					GameEngineServices.skillEngine().getSkill(player, 18145, 1, player).useNoAnimationSkill(); //Power Of Wind.
				}
			}
		}
	}
}
