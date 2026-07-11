package com.aionemu.gameserver.ai.instance.tiamatStronghold;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

import java.util.concurrent.Future;

/**
 * Tiamat Stronghold 副本 NPC AI：Electrocute（@AIName "electrocute"），继承 NpcAI2。
 * Tiamat Stronghold instance NPC AI: Electrocute (@AIName "electrocute"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("electrocute")
public class ElectrocuteAI2 extends NpcAI2
{
	private Future<?> task;
	
	@Override
	public void think() {
	}
	
    @Override
    protected void handleSpawned() {
	    super.handleSpawned();
		task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				AI2Actions.useSkill(ElectrocuteAI2.this, 20757);
			}
		},0, 2000);
	    despawn();
    }
	
    private void despawn() {
	    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
		    @Override
		    public void run() {
			    getOwner().getController().onDelete();
		    }
	    }, 10000);
    }
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	@Override
	public void handleDespawned() {
		task.cancel(true);
		super.handleDespawned();
	}
}
