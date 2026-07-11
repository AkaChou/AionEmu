package com.aionemu.gameserver.ai.classAi;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * 职业技能召唤物/陷阱 AI：Spike Bite Trap（@AIName "spike_bite_trap"），继承 AggressiveNpcAI2。
 * Class-skill summon/trap AI: Spike Bite Trap (@AIName "spike_bite_trap"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("spike_bite_trap")
public class Spike_Bite_TrapAI2 extends AggressiveNpcAI2
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
				startLifeTask();
				AI2Actions.useSkill(Spike_Bite_TrapAI2.this, 17511);
			}
		}, 1000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Spike_Bite_TrapAI2.this);
			}
		}, 5000);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
