package com.aionemu.gameserver.ai.instance.tiamatStronghold;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tiamat Stronghold 副本 NPC AI：Adjutant Anuhart（@AIName "adjutantanuhart"），继承 AggressiveNpcAI2。
 * Tiamat Stronghold instance NPC AI: Adjutant Anuhart (@AIName "adjutantanuhart"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("adjutantanuhart")
public class AdjutantAnuhartAI2 extends AggressiveNpcAI2
{
	
	private Future<?> bladeStormTask;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	private List<Integer> percents = new ArrayList<Integer>();
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			startBladeStormTask();
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void startBladeStormTask() {
		bladeStormTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (isAlreadyDead()) {
					cancelTask();
				} else {
					startBladeStormEvent();
				}
			}
		}, 5000, 40000);
	}
	
	private void startBladeStormEvent() {
		shield();
		GameEngineServices.skillEngine().getSkill(getOwner(), 20747, 1, getOwner()).useNoAnimationSkill();
		spawn(283099, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0); //Blade Storm.
	}
	
	private void cancelTask() {
		if (bladeStormTask != null && !bladeStormTask.isCancelled()) {
			bladeStormTask.cancel(true);
		}
	}
	
	private void checkPercentage(int hpPercentage) {
		for (Integer percent : percents) {
			if (hpPercentage <= percent) {
				switch (percent) {
					case 90:
						swiftAttack(20940);
						startBladeStormTask();
					break;
					case 70:
						swiftAttack(20939);
					break;
					case 50:
						swiftAttack(20938);
						startBladeStormTask();
					break;
					case 30:
						swiftAttack(20940);
					break;
					case 20:
						swiftAttack(20939);
						startBladeStormTask();
					break;
					case 10:
						swiftAttack(20938);
					break;
				}
				percents.remove(percent);
				break;
			}
		}
	}
	
	private void swiftAttack(int buff) {
		AI2Actions.targetSelf(this);
		AI2Actions.useSkill(this, buff);
	}
	
	private void shield() {
		AI2Actions.targetSelf(this);
		AI2Actions.useSkill(this, 20749);
	}
	
	private void addPercent() {
		percents.clear();
		Collections.addAll(percents, new Integer[]{90, 70, 50, 30, 20, 10});
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		addPercent();
	}
	
	@Override
	protected void handleBackHome() {
		addPercent();
		super.handleBackHome();
		cancelTask();
	}
	
	@Override
	protected void handleDespawned() {
		super.handleDespawned();
		cancelTask();
	}
	
	@Override
	protected void handleDied() {
		cancelTask();
		percents.clear();
		super.handleDied();
	}
	
	private void deleteNpcs(List<Npc> npcs) {
		for (Npc npc: npcs) {
			if (npc != null) {
				npc.getController().onDelete();
			}
		}
	}
}
