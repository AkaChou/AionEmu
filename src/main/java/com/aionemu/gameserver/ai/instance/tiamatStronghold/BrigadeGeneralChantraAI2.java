package com.aionemu.gameserver.ai.instance.tiamatStronghold;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tiamat Stronghold 副本 NPC AI：Brigade General Chantra（@AIName "brigadegeneralchantra"），继承 AggressiveNpcAI2。
 * Tiamat Stronghold instance NPC AI: Brigade General Chantra (@AIName "brigadegeneralchantra"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("brigadegeneralchantra")
public class BrigadeGeneralChantraAI2 extends AggressiveNpcAI2
{
	private Future<?> trapTask;
	private boolean isFinalBuff;
	private AtomicBoolean isHome = new AtomicBoolean(true);
	
	@Override
	protected void handleAttack(Creature creature){
		super.handleAttack(creature);
		if (isHome.compareAndSet(true, false)) {
			startSkillTask();
		} if (!isFinalBuff && getOwner().getLifeStats().getHpPercentage() <= 25) {
			isFinalBuff = true;
			AI2Actions.useSkill(this, 20942);
		}
	}
	
	private void startSkillTask()	{
		trapTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run()	{
				if (isAlreadyDead()) {
					cancelTask();
				} else {
					startTrapEvent();
				}
			}
		}, 5000, 40000);
	}
	
	private void cancelTask() {
		if (trapTask != null && !trapTask.isCancelled()) {
			trapTask.cancel(true);
		}
	}
	
	private void startTrapEvent() {
		int [] trapNpc = {283092, 283094};
		final int trap = trapNpc[Rnd.get(0, trapNpc.length -1)]; 
		if (getPosition().getWorldMapInstance().getNpc(trap) == null) {
			spawn(trap, 1031.1f, 466.38f, 445.45f, (byte) 0);
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
	  		    @Override
	  		    public void run() {
	  			    Npc ring = getPosition().getWorldMapInstance().getNpc(trap);
	  			    if (trap == 283092) {
	  				    spawn(283171, 1031.1f, 466.38f, 445.45f, (byte) 0);
					} else {
	  				    spawn(283172, 1031.1f, 466.38f, 445.45f, (byte) 0);
					}
	  			    ring.getController().onDelete();
	  		    }
	  	    }, 5000);
	    }
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		cancelTask();
	}
	
	@Override
	protected void handleDespawned() {
		super.handleDespawned();
		cancelTask();
	}
	
	@Override
	protected void handleBackHome() {
		super.handleBackHome();
		cancelTask();
		isFinalBuff = false;
		isHome.set(true);
	}
}
