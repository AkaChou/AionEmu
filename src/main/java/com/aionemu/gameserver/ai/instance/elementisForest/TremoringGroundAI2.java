package com.aionemu.gameserver.ai.instance.elementisForest;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.poll.AIAnswer;
import com.aionemu.gameserver.ai2.poll.AIAnswers;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elementis Forest 副本 NPC AI：Tremoring Ground（@AIName "tremorground"），继承 GeneralNpcAI2。
 * Elementis Forest instance NPC AI: Tremoring Ground (@AIName "tremorground"), extends GeneralNpcAI2.
 *
 * @author Luzien
 */
@AIName("tremorground")
public class TremoringGroundAI2 extends GeneralNpcAI2 {
	
	private AtomicBoolean isUsed = new AtomicBoolean(false);
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 16) {
				if (isUsed.compareAndSet(false, true)) {
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

						@Override
						public void run() {
							GameEngineServices.skillEngine().getSkill(getOwner(), 19442, 51, player).useNoAnimationSkill();
							AI2Actions.deleteOwner(TremoringGroundAI2.this);
						}

					}, 2000);
				}
			}
		}
	}
	
	@Override
	public AIAnswer ask(AIQuestion question) {
		switch (question) {
			case CAN_ATTACK_PLAYER:
				return AIAnswers.POSITIVE;
			default:
				return AIAnswers.NEGATIVE;
		}
	}
}
