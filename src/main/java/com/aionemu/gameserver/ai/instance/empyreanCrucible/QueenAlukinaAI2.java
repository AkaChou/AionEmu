package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Empyrean Crucible 副本 NPC AI：Queen Alukina（@AIName "alukina_emp"），继承 AggressiveNpcAI2。
 * Empyrean Crucible instance NPC AI: Queen Alukina (@AIName "alukina_emp"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("alukina_emp")
public class QueenAlukinaAI2 extends AggressiveNpcAI2
{
	private List<Integer> percents = new ArrayList<Integer>();
	private Future<?> task;
	
	@Override
	public void handleSpawned() {
		super.handleSpawned();
		addPercents();
	}
	
	@Override
	public void handleDespawned() {
		cancelTask();
		percents.clear();
		super.handleDespawned();
	}
	
	@Override
	public void handleDied() {
		cancelTask();
		super.handleDied();
	}
	
	@Override
	public void handleBackHome() {
		addPercents();
		cancelTask();
		super.handleBackHome();
	}
	
	@Override
	public void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void startEvent(int percent) {
		GameEngineServices.skillEngine().getSkill(getOwner(), 17899, 41, getTarget()).useNoAnimationSkill();
		switch (percent) {
			case 75:
				scheduleSkill(17900, 4500);
				GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 340487, getObjectId(), 0, 10000);
				scheduleSkill(17899, 14000);
				scheduleSkill(17900, 18000);
			break;
			case 50:
				scheduleSkill(17280, 4500);
				scheduleSkill(17902, 8000);
			break;
			case 25:
				task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					if (isAlreadyDead()) {
						cancelTask();
					} else {
						GameEngineServices.skillEngine().getSkill(getOwner(), 17901, 41, getTarget()).useNoAnimationSkill();
						scheduleSkill(17902, 5500);
						scheduleSkill(17902, 7500);
					}
				}
			}, 4500, 20000);
			break;
		}
	}
	
	private void cancelTask() {
		if (task != null && !task.isCancelled())
			task.cancel(true);
	}
	
	private void scheduleSkill(final int skillId , int delay) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead()) {
					GameEngineServices.skillEngine().getSkill(getOwner(), skillId, 41, getTarget()).useNoAnimationSkill();
				}
			}
		}, delay);
	}
	
	private void checkPercentage(int percentage) {
		for (Integer percent : percents) {
			if (percentage <= percent) {
				percents.remove(percent);
				startEvent(percent);
				break;
			}
		}
	}
	
	private void addPercents() {
		percents.clear();
		Collections.addAll(percents, new Integer[] {75, 50, 25});
	}
}
