package com.aionemu.gameserver.ai.worlds.enshar;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.manager.EmoteManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enshar 区域 NPC AI：Tatar Blaze（@AIName "tatar_blaze"），继承 AggressiveNpcAI2。
 * Enshar zone NPC AI: Tatar Blaze (@AIName "tatar_blaze"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("tatar_blaze")
public class Tatar_BlazeAI2 extends AggressiveNpcAI2
{
	private Future<?> phaseTask;
	private Future<?> thinkTask;
	private boolean think = true;
	private int curentPercent = 100;
	private Future<?> specialSkillTask;
	private List<Integer> percents = new ArrayList<Integer>();
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	public boolean canThink() {
		return think;
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		addPercent();
		darkLordBlessing();
	}
	
	private void darkLordBlessing() {
	    GameEngineServices.skillEngine().getSkill(getOwner(), 22664, 1, getOwner()).useNoAnimationSkill(); //Dark Lord's Blessing.
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			startSpecialSkillTask();
			sendMsg(1500499);
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void sendMsg(int msg) {
		GameFeatureServices.npcShoutsService().sendMsg(getOwner(), msg, getObjectId(),false, 0, 0);
	}
	
	private synchronized void checkPercentage(int hpPercentage) {
		curentPercent = hpPercentage;
		for (Integer percent: percents) {
			if (hpPercentage <= percent) {
				switch (percent) {
					case 90:
					case 70:
					case 44:
					case 23:
						cancelspecialSkillTask();
						think = false;
						EmoteManager.emoteStopAttacking(getOwner());
						GameEngineServices.skillEngine().getSkill(getOwner(), 20483, 60, getOwner()).useNoAnimationSkill();
						sendMsg(1500501);
						GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							@Override
							public void run() {
								if (!isAlreadyDead()) {
									GameEngineServices.skillEngine().getSkill(getOwner(), 20216, 60, getOwner()).useNoAnimationSkill();
									startThinkTask();
								}
							}
						}, 3500);
					break;
					case 84:
					case 79:
					case 75:
					case 72:
					case 67:
					case 63:
					case 59:
					case 53:
					case 47:
					case 43:
					case 39:
					case 35:
					case 30:
					case 26:
					case 21:
					case 16:
					case 11:
					case 6:
						startPhaseTask();
					break;
				}
				percents.remove(percent);
				break;
			}
		}
	}
	
	private void startThinkTask() {
		thinkTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead()) {
					think = true;
					Creature creature = getAggroList().getMostHated();
					if (creature == null || creature.getLifeStats().isAlreadyDead() || !getOwner().canSee(creature)) {
						setStateIfNot(AIState.FIGHT);
						think();
					} else {
						getMoveController().abortMove();
						getOwner().setTarget(creature);
						getOwner().getGameStats().renewLastAttackTime();
						getOwner().getGameStats().renewLastAttackedTime();
						getOwner().getGameStats().renewLastChangeTargetTime();
						getOwner().getGameStats().renewLastSkillTime();
						setStateIfNot(AIState.FIGHT);
						handleMoveValidate();
						cancelspecialSkillTask();
						startSpecialSkillTask();
					}
				}
			}
		}, 20000);
	}
	
	private void startPhaseTask() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 20481, 60, getOwner()).useNoAnimationSkill();
		sendMsg(1500500);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead()) {
					cancelspecialSkillTask();
					startSpecialSkillTask();
				}
			}
		}, 4000);
	}
	
	private void startSpecialSkillTask() {
		specialSkillTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead()) {
					GameEngineServices.skillEngine().getSkill(getOwner(), 20223, 60, getOwner()).useNoAnimationSkill();
					specialSkillTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							if (!isAlreadyDead()) {
								GameEngineServices.skillEngine().getSkill(getOwner(), 20224, 60, getOwner()).useNoAnimationSkill();
								specialSkillTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
									@Override
									public void run() {
										if (!isAlreadyDead()) {
											GameEngineServices.skillEngine().getSkill(getOwner(), 20224, 60, getOwner()).useNoAnimationSkill();
											if (curentPercent <= 63) {
												specialSkillTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
													@Override
													public void run() {
														if (!isAlreadyDead()) {
															GameEngineServices.skillEngine().getSkill(getOwner(), 20480, 60, getOwner()).useNoAnimationSkill();
															sendMsg(1500502);
														}
													}
												}, 21000);
											}
										}
									}
								}, 3500);
							}
						}
					}, 1500);
				}
			}
		}, 12000);
	}
	
	private void cancelspecialSkillTask() {
		if (specialSkillTask != null && !specialSkillTask.isDone()) {
			specialSkillTask.cancel(true);
		}
	}
	
	private void cancelPhaseTask() {
		if (phaseTask != null && !phaseTask.isDone()) {
			phaseTask.cancel(true);
		}
	}
	
	private void cancelThinkTask() {
		if (thinkTask != null && !thinkTask.isDone()) {
			thinkTask.cancel(true);
		}
	}
	
	private void addPercent() {
		percents.clear();
		Collections.addAll(percents, new Integer[]{90, 84, 79, 75, 72, 70, 67, 63, 59, 53, 47, 44, 43, 39, 35, 30, 26, 23, 21, 16, 11, 6});
	}
	
	@Override
	protected void handleDespawned() {
		cancelspecialSkillTask();
		cancelThinkTask();
		cancelPhaseTask();
		percents.clear();
		super.handleDespawned();
	}
	
	@Override
	protected void handleDied() {
		sendMsg(1500503);
		cancelspecialSkillTask();
		cancelThinkTask();
		cancelPhaseTask();
		percents.clear();
		super.handleDied();
	}
	
	@Override
	protected void handleBackHome() {
		think = true;
		cancelspecialSkillTask();
		cancelThinkTask();
		cancelPhaseTask();
		addPercent();
		curentPercent = 100;
		isAggred.set(false);
		super.handleBackHome();
	}
}
