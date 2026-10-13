package com.aionemu.gameserver.ai.instance.tiamatStronghold;


import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

import java.util.concurrent.Future;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Tiamat Stronghold 副本 NPC AI：Blade Storm（@AIName "bladestorm"），继承 AggressiveNpcAI2。
 * Tiamat Stronghold instance NPC AI: Blade Storm (@AIName "bladestorm"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("bladestorm")
public class BladeStormAI2 extends AggressiveNpcAI2
{
	private Future<?> stormBladeTask;

	@Override
	public void think() {
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		stormBlade();
		startLifeTask();
	}

	private void stormBlade() {
		stormBladeTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(() -> {
			AI2Actions.targetCreature(BladeStormAI2.this, getPosition().getWorldMapInstance().getNpc(219357)); //Adjudant Anuhart.
			AI2Actions.targetCreature(BladeStormAI2.this, getPosition().getWorldMapInstance().getNpc(247717)); //F4_Raid_Drakan_Boss_55_Ah.
			AI2Actions.useSkill(BladeStormAI2.this, 20748); //Storm Blade.
		}, 3000, 8000);
	}

    private void startLifeTask() {
		ThreadPoolManager.getInstance().schedule(() -> AI2Actions.deleteOwner(BladeStormAI2.this), 10000);
	}

    @Override
	public boolean isMoveSupported() {
		return false;
	}
}
