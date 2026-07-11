package com.aionemu.gameserver.ai.instance.elementisForest;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Elementis Forest 副本 NPC AI：Canyon Mark（@AIName "canyonmark"），继承 AggressiveNpcAI2。
 * Elementis Forest instance NPC AI: Canyon Mark (@AIName "canyonmark"), extends AggressiveNpcAI2.
 *
 * @author Luzien
 */
@AIName("canyonmark")
public class CanyonMarkAI2 extends AggressiveNpcAI2 {
	
	private Creature target;
	
	@Override
	public void handleSpawned() {
		super.handleSpawned();
		markTarget();
	}

	private void markTarget() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			
			@Override
			public void run() {
				target = (Creature) getOwner().getTarget();
				if (target != null) {
					AI2Actions.useSkill(CanyonMarkAI2.this, 19504);

					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

						@Override
						public void run() {
							if (!isAlreadyDead()) {
								AI2Actions.targetCreature(CanyonMarkAI2.this, target);
								AI2Actions.useSkill(CanyonMarkAI2.this, 19505);
								AI2Actions.deleteOwner(CanyonMarkAI2.this);
							}
						}

					}, Rnd.get(5,10) * 1000);
					
				}
				else {
					AI2Actions.deleteOwner(CanyonMarkAI2.this);
				}
			}
		}, 5000);
	}
}
