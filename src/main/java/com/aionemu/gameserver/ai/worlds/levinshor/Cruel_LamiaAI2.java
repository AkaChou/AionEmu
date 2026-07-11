package com.aionemu.gameserver.ai.worlds.levinshor;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Levinshor 区域 NPC AI：Cruel Lamia（@AIName "cruel_lamia"），继承 AggressiveNpcAI2。
 * Levinshor zone NPC AI: Cruel Lamia (@AIName "cruel_lamia"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("cruel_lamia")
public class Cruel_LamiaAI2 extends AggressiveNpcAI2
{
	private Future<?> lamiaTask;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	private AtomicBoolean isStartedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			sendMsg(1500229);
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 95) {
			if (isStartedEvent.compareAndSet(false, true)) {
				startPhaseTask();
			}
		} if (hpPercentage <= 75) {
			if (isStartedEvent.compareAndSet(false, true)) {
				startPhaseTask();
			}
		} if (hpPercentage <= 55) {
			if (isStartedEvent.compareAndSet(false, true)) {
				startPhaseTask();
			}
		} if (hpPercentage <= 35) {
			if (isStartedEvent.compareAndSet(false, true)) {
				startPhaseTask();
			}
		} if (hpPercentage <= 15) {
			if (isStartedEvent.compareAndSet(false, true)) {
				startPhaseTask();
			}
		}
	}
	
	private void startPhaseTask() {
		lamiaTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (isAlreadyDead()) {
					cancelPhaseTask();
				} else {
					getOwner().getController().cancelCurrentSkill();
					GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1500230, getObjectId(), 0, 0);
					GameEngineServices.skillEngine().getSkill(getOwner(), 19551, 60, getOwner()).useNoAnimationSkill();
				}
			}
		}, 3000, 15000);
	}
	
	private void sendMsg(int msg) {
		GameFeatureServices.npcShoutsService().sendMsg(getOwner(), msg, getObjectId(), false, 0, 0);
	}
	
	@Override
	protected void handleDespawned() {
		cancelPhaseTask();
		super.handleDespawned();
	}
	
	@Override
	public void handleDied() {
		cancelPhaseTask();
		sendMsg(1500231);
		super.handleDied();
	}
	
	@Override
	protected void handleBackHome() {
		cancelPhaseTask();
		isStartedEvent.set(false);
		isAggred.set(false);
		super.handleBackHome();
	}
	
	private void cancelPhaseTask() {
		if (lamiaTask != null && !lamiaTask.isDone()) {
			lamiaTask.cancel(true);
		}
	}
}
