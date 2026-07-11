package com.aionemu.gameserver.ai.instance.rentusBase;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

import java.util.concurrent.Future;

/**
 * Rentus Base 副本 NPC AI：Spilled Oil（@AIName "spilled_oil"），继承 AggressiveNpcAI2。
 * Rentus Base instance NPC AI: Spilled Oil (@AIName "spilled_oil"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("spilled_oil")
public class SpilledOilAI2 extends AggressiveNpcAI2
{
	private Future<?> attackOilSoakTask;
	
	@Override
	public void think() {
	}
	
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		attackOilSoak();
		startLifeTask();
    }
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(SpilledOilAI2.this);
			}
		}, 20000); //20 Secondes.
	}
	
	private void attackOilSoak() {
		attackOilSoakTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				AI2Actions.targetCreature(SpilledOilAI2.this, getPosition().getWorldMapInstance().getNpc(217311)); //Kuhara The Volatile.
				AI2Actions.targetCreature(SpilledOilAI2.this, getPosition().getWorldMapInstance().getNpc(236298)); //Kuhara The Volatile.
				AI2Actions.useSkill(SpilledOilAI2.this, 19658); //Oil Soak.
			}
		}, 3000, 8000);
	}
	
	private void delete() {
		AI2Actions.deleteOwner(this);
	}
}
