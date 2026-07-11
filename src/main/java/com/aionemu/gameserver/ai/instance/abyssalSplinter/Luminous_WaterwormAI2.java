package com.aionemu.gameserver.ai.instance.abyssalSplinter;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Abyssal Splinter 副本 NPC AI：Luminous Waterworm（@AIName "Luminous_Waterworm"），继承 AggressiveNpcAI2。
 * Abyssal Splinter instance NPC AI: Luminous Waterworm (@AIName "Luminous_Waterworm"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Luminous_Waterworm")
public class Luminous_WaterwormAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
    @Override
    protected void handleSpawned() {
        super.handleSpawned();
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                AI2Actions.targetCreature(Luminous_WaterwormAI2.this, getPosition().getWorldMapInstance().getNpc(216951)); //Pazuzu.
				AI2Actions.targetCreature(Luminous_WaterwormAI2.this, getPosition().getWorldMapInstance().getNpc(219554)); //Unstable Pazuzu.
                AI2Actions.useSkill(Luminous_WaterwormAI2.this, 19291); //Replenishment.
            }
        }, 3000);
    }
	
	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
