package com.aionemu.gameserver.ai.instance.infernalDanuarReliquary;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

import java.util.concurrent.Future;

/**
 * Infernal Danuar Reliquary 副本 NPC AI：Venge Full Orb（@AIName "venge_full_orb"），继承 NpcAI2。
 * Infernal Danuar Reliquary instance NPC AI: Venge Full Orb (@AIName "venge_full_orb"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("venge_full_orb")
public class Venge_Full_OrbAI2 extends NpcAI2
{
	private Future<?> task;
	
    @Override
    protected void handleSpawned() {
  	    super.handleSpawned();
		final int skill;
		switch (getNpcId()) {
			case 284443: // 魔道女王莫多尔 / Sorcerer Queen Modor.
				skill = 21178;
		    break;
			default:
				skill = 0;
		}
		if (skill == 0)
			return;
		task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				AI2Actions.useSkill(Venge_Full_OrbAI2.this, skill);
			}
		},0, 2000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Venge_Full_OrbAI2.this);
			}
		}, 1000);
	}
	
	@Override
	public void handleDespawned() {
		task.cancel(true);
		super.handleDespawned();
	}
}
