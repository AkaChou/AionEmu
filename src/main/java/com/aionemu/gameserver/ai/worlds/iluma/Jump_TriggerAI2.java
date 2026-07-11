package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Iluma 区域 NPC AI：Jump Trigger（@AIName "jump_trigger"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Jump Trigger (@AIName "jump_trigger"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("jump_trigger")
public class Jump_TriggerAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			PlayerEffectController effectController = player.getEffectController();
			if (MathUtil.getDistance(getOwner(), player) <= 5) {
				if (startedEvent.compareAndSet(false, true)) {
					switch (Rnd.get(1, 5)) {
					    case 1:
						    effectController.removeEffect(22883);
				            effectController.removeEffect(22884);
				            effectController.removeEffect(22885);
							effectController.removeEffect(22886);
                            GameEngineServices.skillEngine().getSkill(player, 22882, 1, player).useNoAnimationSkill(); //Boost Attack Power.
						break;
						case 2:
						    effectController.removeEffect(22884);
				            effectController.removeEffect(22885);
				            effectController.removeEffect(22886);
							effectController.removeEffect(22882);
                            GameEngineServices.skillEngine().getSkill(player, 22883, 1, player).useNoAnimationSkill(); //Movement Speed Increase.
						break;
						case 3:
						    effectController.removeEffect(22885);
				            effectController.removeEffect(22886);
				            effectController.removeEffect(22882);
							effectController.removeEffect(22883);
                            GameEngineServices.skillEngine().getSkill(player, 22884, 1, player).useNoAnimationSkill(); //Attack Speed Increased.
						break;
						case 4:
						    effectController.removeEffect(22886);
				            effectController.removeEffect(22882);
				            effectController.removeEffect(22883);
							effectController.removeEffect(22884);
                            GameEngineServices.skillEngine().getSkill(player, 22885, 1, player).useNoAnimationSkill(); //Boost Defense.
						break;
						case 5:
						    effectController.removeEffect(22882);
				            effectController.removeEffect(22883);
				            effectController.removeEffect(22884);
							effectController.removeEffect(22885);
                            GameEngineServices.skillEngine().getSkill(player, 22886, 1, player).useNoAnimationSkill(); //Casting Time Reduced.
						break;
					}
					AI2Actions.deleteOwner(Jump_TriggerAI2.this);
					AI2Actions.scheduleRespawn(this);
				}
			}
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
