package com.aionemu.gameserver.ai.instance.aturamSkyFortress;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aturam Sky Fortress 副本 NPC AI：Ashunatal Shadowslip（@AIName "ashunatal_shadowslip"），继承 AggressiveNpcAI2。
 * Aturam Sky Fortress instance NPC AI: Ashunatal Shadowslip (@AIName "ashunatal_shadowslip"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("ashunatal_shadowslip")
public class AshunatalShadowslipAI2 extends AggressiveNpcAI2
{
	private boolean isSummoned;
	private boolean canThink = true;
	private AtomicBoolean isHome = new AtomicBoolean(true);
	
	@Override
	public boolean canThink() {
		return canThink;
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isHome.compareAndSet(true, false)) {
			getPosition().getWorldMapInstance().getDoors().get(2).setOpen(true);
			getPosition().getWorldMapInstance().getDoors().get(17).setOpen(true);
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 80 && !isSummoned) {
			isSummoned = true;
			GameEngineServices.skillEngine().getSkill(getOwner(), 19428, 1, getOwner()).useNoAnimationSkill();
			doSchedule();
		}
	}
	
	@Override
	protected void handleBackHome() {
		isHome.set(true);
		isSummoned = false;
		super.handleBackHome();
		getPosition().getWorldMapInstance().getDoors().get(17).setOpen(true);
		getPosition().getWorldMapInstance().getDoors().get(2).setOpen(false);
		Npc npc = getPosition().getWorldMapInstance().getNpc(219186);
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	/**
	 * 安全：若 Ashunatal 死亡。
	 * Security: if Ashunatal dies
	 */
	@Override
	protected void handleDied() {
		super.handleDied();
		getPosition().getWorldMapInstance().getDoors().get(17).setOpen(true);
	}
	
	private void despawn() {
		AI2Actions.deleteOwner(this);
	}
	
	private void doSchedule() {
		if (!isAlreadyDead()) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					if (!isAlreadyDead()) {
						// 阿舒纳塔尔已撤到另一房间。追击她！ / Ashunatal has retreated to another room. Hunt her down!
						GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1401391, 0);
						GameEngineServices.skillEngine().getSkill(getOwner(), 19417, 49, getOwner()).useNoAnimationSkill();
						GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							@Override
							public void run() {
								if (!isAlreadyDead()) {
									WorldPosition p = getPosition();
									spawn(219186, p.getX(), p.getY(), p.getZ(), p.getHeading());
									canThink = false;
									getSpawnTemplate().setWalkerId("3002400001");
									setStateIfNot(AIState.WALKING);
									think();
									getOwner().setState(1);
									PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getObjectId()));
									GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
										@Override
										public void run() {
											if (!isAlreadyDead()) {
												despawn();
											}
										}
									}, 4000);
								}
							}
						}, 3000);
					}
				}
			}, 2000);
		}
	}
}
