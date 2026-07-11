package com.aionemu.gameserver.ai.instance.sauroSupplyBase;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

import java.util.concurrent.Future;

/**
 * Sauro Supply Base 副本 NPC AI：Corrupted Danuar（@AIName "corrupted_danuar"），继承 AggressiveNpcAI2。
 * Sauro Supply Base instance NPC AI: Corrupted Danuar (@AIName "corrupted_danuar"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("corrupted_danuar")
public class Corrupted_DanuarAI2 extends AggressiveNpcAI2
{
    private Future<?> skillTask;
	
    @Override
    protected void handleSpawned() {
        super.handleSpawned();
        startpower();
    }
	
    private void startpower() {
        skillTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
		        AI2Actions.targetSelf(Corrupted_DanuarAI2.this);
                AI2Actions.useSkill(Corrupted_DanuarAI2.this, 21185); //Curse Of The Rune.
            }
        }, 3000, 5000);
    }
	
    private void cancelskillTask() {
        if (skillTask != null && !skillTask.isCancelled()) {
            skillTask.cancel(true);
        }
    }
	
    @Override
    protected void handleDied() {
        cancelskillTask();
        super.handleDied();
    }
	
    @Override
    protected void handleDespawned() {
        cancelskillTask();
        super.handleDespawned();
    }
}
