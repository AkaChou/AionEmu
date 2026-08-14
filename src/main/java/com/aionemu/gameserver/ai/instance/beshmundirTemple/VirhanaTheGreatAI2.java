package com.aionemu.gameserver.ai.instance.beshmundirTemple;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Beshmundir Temple 副本 NPC AI：Virhana The Great（@AIName "virhana"），继承 AggressiveNpcAI2。
 * Beshmundir Temple instance NPC AI: Virhana The Great (@AIName "virhana"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("virhana")
public class VirhanaTheGreatAI2 extends AggressiveNpcAI2
{
	private int count;
	private boolean isStart;
	
	@Override
	public void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (!isStart){
			isStart = true;
			scheduleRage();
		}
	}
	
	@Override
	protected void handleBackHome() {
		super.handleBackHome();
		isStart = false;
	}
	
	private void scheduleRage() {
		if (isAlreadyDead() || !isStart) {
			return;
		}
		AI2Actions.useSkill(this, 19121); // 反射封印 / Seal Of Reflection.
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				startRage();
			}
		}, 70000);
	}
	
	private void startRage() {
		if (isAlreadyDead() || !isStart) {
			return;
		} if (count < 12) {
			AI2Actions.useSkill(this, 18897); // 大地惩戒 / Earthly Retribution.
			count++;
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					startRage();
				}
			}, 10000);
		} else {
			count = 0;
			scheduleRage();
		}
	}
}
