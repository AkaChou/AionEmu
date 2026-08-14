package com.aionemu.gameserver.ai.instance.archivesOfEternity;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Archives Of Eternity 副本 NPC AI：Tech Golem（@AIName "techgolem"），继承 AggressiveNpcAI2。
 * Archives Of Eternity instance NPC AI: Tech Golem (@AIName "techgolem"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("techgolem")
public class TechGolemAI2 extends AggressiveNpcAI2
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
				AI2Actions.deleteOwner(TechGolemAI2.this);
			}
		}, 120000); // 2 分钟后自毁 / Self-destruct after 2 minutes
	}
}
