package com.aionemu.gameserver.ai.instance.dragonLordRefuge;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

import java.util.concurrent.Future;

/**
 * Dragon Lord Refuge 副本 NPC AI：Calindi Summons（@AIName "calindisummon"），继承 AggressiveNpcAI2。
 * Dragon Lord Refuge instance NPC AI: Calindi Summons (@AIName "calindisummon"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("calindisummon")
public class CalindiSummonsAI2 extends AggressiveNpcAI2
{
	private Future<?> task;
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		final int skill = getOwner().getNpcId() == 283132 ? 20914 : 20916;
		int delay = getNpcId() == 283132 ? 500 : 2000;
		task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				AI2Actions.useSkill(CalindiSummonsAI2.this, skill);
			}
		}, delay, delay);
		despawn();
	}
	
	private void despawn() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				getOwner().getController().onDelete();
			}
		}, 15000);
	}
	
	@Override
	public void handleDespawned() {
		task.cancel(true);
		super.handleDespawned();
	}
}
