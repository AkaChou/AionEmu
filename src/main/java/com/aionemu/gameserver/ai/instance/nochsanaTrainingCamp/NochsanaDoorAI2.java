package com.aionemu.gameserver.ai.instance.nochsanaTrainingCamp;

import com.aionemu.gameserver.ai.GeneralNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Nochsana Training Camp 副本 NPC AI：Nochsana Door（@AIName "nochsanadoor"），继承 GeneralNpcAI2。
 * Nochsana Training Camp instance NPC AI: Nochsana Door (@AIName "nochsanadoor"), extends GeneralNpcAI2.
 */
@AIName("nochsanadoor")
public class NochsanaDoorAI2 extends GeneralNpcAI2 {

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
	}

	@Override
	protected void handleAttack(Creature creature) {
	}

	@Override
	protected void handleDied() {
		super.handleDied();
		getOwner().getController().onDelete();
	}
}
