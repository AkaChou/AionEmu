package com.aionemu.gameserver.ai.instance.fallenPoeta;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

import java.util.concurrent.Future;

/**
 * Fallen Poeta 副本 NPC AI：Balaur Explosives Stockpile（@AIName "Balaur_Explosives_Stockpile"），继承 AggressiveNpcAI2。
 * Fallen Poeta instance NPC AI: Balaur Explosives Stockpile (@AIName "Balaur_Explosives_Stockpile"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Balaur_Explosives_Stockpile")
public class Balaur_Explosives_StockpileAI2 extends AggressiveNpcAI2
{
	// 攻击增益定时任务：周期性对副官阿努哈尔特施放技能。 / Attack boost task: periodically uses a skill on Lieutenant Anuhart.
	private Future<?> attackBoostTask;
	
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		attackBoost();
		super.handleSpawned();
	}
	
	private void attackBoost() {
		attackBoostTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				AI2Actions.targetCreature(Balaur_Explosives_StockpileAI2.this, getPosition().getWorldMapInstance().getNpc(243682)); //副官阿努哈尔特。 / Lieutenant Anuhart.
				AI2Actions.useSkill(Balaur_Explosives_StockpileAI2.this, 0);
			}
		}, 3000, 8000);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
