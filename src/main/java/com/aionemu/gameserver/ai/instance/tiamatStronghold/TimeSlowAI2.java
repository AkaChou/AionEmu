package com.aionemu.gameserver.ai.instance.tiamatStronghold;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Tiamat Stronghold 副本 NPC AI：Time Slow（@AIName "timeslow"），继承 AggressiveNpcAI2。
 * Tiamat Stronghold instance NPC AI: Time Slow (@AIName "timeslow"), extends AggressiveNpcAI2.
 *
 * @author Ranastic (Encom)
 */
@AIName("timeslow")
public class TimeSlowAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		int lifetime = (getNpcId() == 283088 ? 20000 : 10000); //Time Slow.
		toDespawn(lifetime);
	}
	
	private void toDespawn(int delay) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(TimeSlowAI2.this);
			}
		}, delay);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
