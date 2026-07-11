package com.aionemu.gameserver.ai.worlds.morheim;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Morheim 区域 NPC AI：Chieftain Muhamurru（@AIName "chieftain_muhamurru"），继承 AggressiveNpcAI2。
 * Morheim zone NPC AI: Chieftain Muhamurru (@AIName "chieftain_muhamurru"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("chieftain_muhamurru")
public class ChieftainMuhamurruAI2 extends AggressiveNpcAI2
{
	private Future<?> hideTask;
	private AtomicBoolean isHome = new AtomicBoolean(true);
	
	@Override
	public void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isHome.compareAndSet(true, false)) {
			sendMsg(1500397);
			startHideTask();
		}
	}
	
	private void cancelPhaseTask() {
		if (hideTask != null && !hideTask.isDone()) {
			hideTask.cancel(true);
		}
	}
	
	private void startHideTask() {
		hideTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (isAlreadyDead()) {
					cancelPhaseTask();
				} else {
					GameEngineServices.skillEngine().getSkill(getOwner(), 19660, 60, getOwner()).useNoAnimationSkill();
					sendMsg(1500398);
					startEvent(2000, 1500399, 19661);
					startEvent(6000, 1500399, 19661);
					startEvent(8000, 1500400, 19662);
				}
			}
		}, 14000, 14000);
	}
	
	private void startEvent(int time, final int msg, final int skill) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead() && !isHome.get()) {
					Creature target = getOwner();
					if (skill == 19661) {
						VisibleObject npcTarget = target.getTarget();
						if (npcTarget != null && npcTarget instanceof Creature) {
							target = (Creature) npcTarget;
						}
					} if (target != null && isInRange(target, 5)) {
						GameEngineServices.skillEngine().getSkill(getOwner(), skill, 60, target).useNoAnimationSkill();
					}
					getEffectController().removeEffect(19660);
					sendMsg(msg);
				}
			}
		}, time);
	}
	
	private void sendMsg(int msg) {
		GameFeatureServices.npcShoutsService().sendMsg(getOwner(), msg, getObjectId(), 0, 0);
	}
	
	@Override
	protected void handleDied() {
		cancelPhaseTask();
		sendMsg(1500401);
		super.handleDied();
	}
	
	@Override
	protected void handleDespawned() {
		cancelPhaseTask();
		super.handleDespawned();
	}
	
	@Override
	protected void handleBackHome() {
		getEffectController().removeEffect(19660);
		cancelPhaseTask();
		isHome.set(true);
		super.handleBackHome();
	}
}
