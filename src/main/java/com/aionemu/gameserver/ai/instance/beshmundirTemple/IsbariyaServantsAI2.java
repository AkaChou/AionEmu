package com.aionemu.gameserver.ai.instance.beshmundirTemple;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Beshmundir Temple 副本 NPC AI：Isbariya Servants（@AIName "isbariyaServants"），继承 AggressiveNpcAI2。
 * Beshmundir Temple instance NPC AI: Isbariya Servants (@AIName "isbariyaServants"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("isbariyaServants")
public class IsbariyaServantsAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		int lifetime = (getNpcId() == 281659 ? 20000 : 10000);
		toDespawn(lifetime);
	}
	
	private void toDespawn(int delay) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(IsbariyaServantsAI2.this);
			}
		}, delay);
	}
}
