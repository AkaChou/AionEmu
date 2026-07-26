package com.aionemu.gameserver.ai.instance.esoterrace;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Esoterrace 副本 NPC AI：Captain Murugan（@AIName "captain_murugan"），继承 AggressiveNpcAI2。
 * Esoterrace instance NPC AI: Captain Murugan (@AIName "captain_murugan"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("captain_murugan")
public class Captain_MuruganAI2 extends AggressiveNpcAI2
{
	private Future<?> task;
	private Future<?> specialSkillTask;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			getPosition().getWorldMapInstance().getInstanceHandler().setDoorState(70, false);
			startTaskEvent();
		}
	}
	
	private void startTaskEvent() {
		VisibleObject target = getTarget();
		if (target != null && target instanceof Player) {
			GameEngineServices.skillEngine().getSkill(getOwner(), 19324, 1, target).useNoAnimationSkill();
		}
		task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (isAlreadyDead()) {
					cancelTask();
				} else {
					// 我先除掉被诅咒的那些！ / I'll get rid of the cursed ones first!
					sendMsg(1500193, getObjectId(), false, 0);
					GameEngineServices.skillEngine().getSkill(getOwner(), 19325, 1, getOwner()).useNoAnimationSkill();
					if (getLifeStats().getHpPercentage() <= 50) {
						specialSkillTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							@Override
							public void run() {
								if (!isAlreadyDead()) {
									// 我先除掉被诅咒的那些！ / I'll get rid of the cursed ones first!
									sendMsg(1500193, getObjectId(), false, 0);
									VisibleObject target = getTarget();
									if (target != null && target instanceof Player) {
										GameEngineServices.skillEngine().getSkill(getOwner(), 19324, 1, target).useNoAnimationSkill();
									}
									specialSkillTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
										@Override
										public void run() {
											if (!isAlreadyDead()) {
												VisibleObject target = getTarget();
												if (target != null && target instanceof Player) {
													GameEngineServices.skillEngine().getSkill(getOwner(), 19324, 1, target).useNoAnimationSkill();
												}
											}
										}
									}, 4000);
								}
							}
						}, 10000);
					}
				}
			}
		}, 20000, 20000);
	}
	
	private void cancelTask() {
		if (task != null && !task.isDone()) {
			task.cancel(true);
		}
	}
	
	private void cancelSpecialSkillTask() {
		if (specialSkillTask != null && !specialSkillTask.isDone()) {
			specialSkillTask.cancel(true);
		}
	}
	
	@Override
	protected void handleBackHome() {
		cancelTask();
		cancelSpecialSkillTask();
		super.handleBackHome();
	}
	
	@Override
	protected void handleDespawned() {
		cancelTask();
		cancelSpecialSkillTask();
		super.handleDespawned();
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
	
	@Override
	protected void handleDied() {
		cancelTask();
		cancelSpecialSkillTask();
		// 力量在……溢出…… / Power is... overflowing...
		sendMsg(1500195, getObjectId(), false, 0);
		// 苏拉玛大人……我……很……抱歉。 / My lord Surama... I.. am... sorry.
		sendMsg(1500194, getObjectId(), false, 5000);
		super.handleDied();
	}
}
