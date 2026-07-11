package com.aionemu.gameserver.ai.instance.beshmundirTemple;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Beshmundir Temple 副本 NPC AI：Magic Artifact（@AIName "magicartifact"），继承 AggressiveNpcAI2。
 * Beshmundir Temple instance NPC AI: Magic Artifact (@AIName "magicartifact"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("magicartifact")
public class MagicArtifactAI2 extends AggressiveNpcAI2
{
	private boolean cooldown = false;
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (!cooldown) {
			AI2Actions.useSkill(this, 18916);
			setCD();
		}
	}
	
	private void setCD() {
		cooldown = true;
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				cooldown = false;
			}
		}, 1000);
	}
}
