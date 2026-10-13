package com.aionemu.gameserver.ai.instance.rentusBase;


import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

import java.util.concurrent.Future;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Rentus Base 副本 NPC AI：Spilled Oil（@AIName "spilled_oil"），继承 AggressiveNpcAI2。
 * Rentus Base instance NPC AI: Spilled Oil (@AIName "spilled_oil"), extends AggressiveNpcAI2.
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
		ThreadPoolManager.getInstance().schedule(() -> AI2Actions.deleteOwner(SpilledOilAI2.this), 20000); // 20 秒后 / 20 Secondes.
	}

	private void attackOilSoak() {
		attackOilSoakTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(() -> {
			AI2Actions.targetCreature(SpilledOilAI2.this, getPosition().getWorldMapInstance().getNpc(217311)); // 狂暴的库哈拉 / Kuhara The Volatile.
			AI2Actions.targetCreature(SpilledOilAI2.this, getPosition().getWorldMapInstance().getNpc(236298)); // 狂暴的库哈拉 / Kuhara The Volatile.
			AI2Actions.useSkill(SpilledOilAI2.this, 19658); // 油浸 / Oil Soak.
		}, 3000, 8000);
	}

}
