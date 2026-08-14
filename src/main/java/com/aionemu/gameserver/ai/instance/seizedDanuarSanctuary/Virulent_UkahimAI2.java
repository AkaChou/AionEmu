package com.aionemu.gameserver.ai.instance.seizedDanuarSanctuary;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Seized Danuar Sanctuary 副本 NPC AI：Virulent Ukahim（@AIName "ukahim"），继承 AggressiveNpcAI2。
 * Seized Danuar Sanctuary instance NPC AI: Virulent Ukahim (@AIName "ukahim"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("ukahim")
public class Virulent_UkahimAI2 extends AggressiveNpcAI2
{
	private int stage = 0;
	private boolean isStart = false;
	private Future<?> enrageTask;
	
	@Override
	protected void handleCreatureAggro(Creature creature) {
		super.handleCreatureAggro(creature);
		wakeUp();
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
		wakeUp();
	}
	
	private void wakeUp() {
		isStart = true;
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 90 && stage < 1) {
			stage1();
			stage = 1;
		} if (hpPercentage <= 50 && stage < 2) {
			stage2();
			stage = 2;
		} if (hpPercentage <= 20 && stage < 3) {
			stage3();
			stage = 3;
		}
	}
	
	private void stage1() {
		int delay = 0;
		if (isAlreadyDead() || !isStart) {
			return;
		} else {
			GameEngineServices.skillEngine().getSkill(getOwner(), 21135, 60, getOwner()).useNoAnimationSkill(); // 贝里特拉的恩宠。 / Beritra's Favor.
		}
	}
	
	private void stage2() {
		int delay = 35000;
		if (isAlreadyDead() || !isStart) {
			return;
		} else {
		    skill();
			scheduleDelayStage2(delay);
		}
	}
	
	private void skill() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 18158, 100, getOwner()).useNoAnimationSkill(); // 狂怒毒爆。 / Wrathful Venom Burst.
		   GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			public void run() {
                GameEngineServices.skillEngine().getSkill(getOwner(), 18160, 100, getOwner()).useNoAnimationSkill(); // 剧毒。 / Virulence.
			}
		}, 4000);
	}
	
	private void scheduleDelayStage2(int delay) {
		if (!isStart && !isAlreadyDead()) {
			return;
		} else {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					stage2();
				}
			}, delay);
		}
	}
	
	private void stage3() {
		int delay = 15000;
		if (isAlreadyDead() || !isStart) {
			return;
		} else {
			scheduleDelayStage3(delay);
		}
	}
	
	private void scheduleDelayStage3(int delay) {
		if (!isStart && !isAlreadyDead()) {
			return;
		} else {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					getRandomTarget();
					stage3();
				}
			}, delay);
		}
	}
	
    private void getRandomTarget()  {
        List<Player> players = new ArrayList<Player>();
        for (Player player : getKnownList().getKnownPlayers().values()) {
            if (!PlayerActions.isAlreadyDead(player) && MathUtil.isIn3dRange(player, getOwner(), 16)) {
                players.add(player);
			}
        } if (players.isEmpty()) {
            return;
		}
        getAggroList().clear();
        getAggroList().startHate(players.get(Rnd.get(0, players.size() - 1)));
    }
	
	@Override
	protected void handleBackHome() {
        super.handleBackHome();
		isStart = false;
		stage = 0;
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		isStart = false;
		stage = 0;
	}
}
