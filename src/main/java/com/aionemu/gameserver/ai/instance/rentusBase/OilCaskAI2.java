package com.aionemu.gameserver.ai.instance.rentusBase;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Rentus Base 副本 NPC AI：Oil Cask（@AIName "oil_cask"），继承 AggressiveNpcAI2。
 * Rentus Base instance NPC AI: Oil Cask (@AIName "oil_cask"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("oil_cask")
public class OilCaskAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
	}
	
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		startLifeTask();
    }
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(OilCaskAI2.this);
			}
		}, 20000); // 20 秒后 / 20 Secondes.
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
