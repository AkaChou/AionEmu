package com.aionemu.gameserver.ai.instance.transidiumAnnex;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transidium Annex 副本 NPC AI：Assult Pod4（@AIName "assult_pod_4"），继承 AggressiveNpcAI2。
 * Transidium Annex instance NPC AI: Assult Pod4 (@AIName "assult_pod_4"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("assult_pod_4")
public class AssultPod4AI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 5) {
				if (startedEvent.compareAndSet(false, true)) {
					GameEngineServices.skillEngine().getSkill(getOwner(), 19358, 60, getOwner()).useNoAnimationSkill();
					GameEngineServices.skillEngine().getSkill(getOwner(), 19922, 60, getOwner()).useNoAnimationSkill();
					AI2Actions.deleteOwner(AssultPod4AI2.this);
					spawn(297188, 636.82513f, 396.8799f, 688.8357f, (byte) 104);
					spawn(297188, 623.8942f, 384.79068f, 688.8357f, (byte) 107);
					spawn(297188, 621.5461f, 401.1501f, 688.86523f, (byte) 105);
					// 烟雾特效 / FXMon_Smoke
					spawn(297352, 379.51096f, 395.966f, 688.8357f, (byte) 78);
					spawn(297352, 394.50833f, 385.5321f, 688.8357f, (byte) 70);
					spawn(297352, 397.13196f, 401.5456f, 688.86523f, (byte) 75);
					spawn(297309, 645.18121f, 376.97357f, 688.78943f, (byte) 78, 165); // 迪西隆进阶走廊护盾 / Disillon Advance Corridor Shield
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					    @Override
					    public void run() {
							spawn(297193, 639.1037f, 393.87778f, 688.8357f, (byte) 103);
				        }
			        }, 1000);
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					    @Override
					    public void run() {
							spawn(297193, 626.69946f, 382.24298f, 688.8357f, (byte) 107);
				        }
			        }, 3000);
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					    @Override
					    public void run() {
							spawn(297192, 624.11786f, 398.21442f, 688.869f, (byte) 105);
							spawn(297193, 647.3834f, 381.9655f, 688.94727f, (byte) 82);
							spawn(297193, 639.4842f, 377.7366f, 688.9912f, (byte) 118);
				        }
			        }, 5000);
				}
			}
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
