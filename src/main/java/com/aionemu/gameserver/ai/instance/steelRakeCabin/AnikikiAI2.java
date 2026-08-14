package com.aionemu.gameserver.ai.instance.steelRakeCabin;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Steel Rake Cabin 副本 NPC AI：Anikiki（@AIName "anikiki"），继承 AggressiveNpcAI2。
 * Steel Rake Cabin instance NPC AI: Anikiki (@AIName "anikiki"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("anikiki")
public class AnikikiAI2 extends AggressiveNpcAI2 {
	private AtomicBoolean isStartedWalkEvent = new AtomicBoolean(false);
	
	@Override
	public void think() {
	}
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
		    if (MathUtil.getDistance(getOwner(), player) <= 8) {
			    // 玩家靠近 8 码内时启动一次行走事件并生成巡逻怪。 / When a player comes within 8 yards, start the walk event once and spawn patrol mobs.
			    if (isStartedWalkEvent.compareAndSet(false, true)) {
				    getSpawnTemplate().setWalkerId("3004600001");
				    WalkManager.startWalking(this);
				    getOwner().setState(1);
				    PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getObjectId()));
				    spawn(700553, 611f, 481f, 936f, (byte) 90);
				    spawn(700553, 657f, 482f, 936f, (byte) 60);
				    spawn(700553, 626f, 540f, 936f, (byte) 1);
				    spawn(700553, 645f, 534f, 936f, (byte) 75);
				    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 900));
				    GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1400262, 3000);
				}
			}
		}
	}
	
	@Override
	protected void handleMoveArrived() {
		int point = getOwner().getMoveController().getCurrentPoint();
		super.handleMoveArrived();
		if (getNpcId() == 219040) {
			if (point == 8) {
				// 到达 8 号点播放表情。 / Play emote at point 8.
				getOwner().setState(64);
				PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getObjectId()));
			} if (point == 12) {
				// 到达 12 号点停止行走并删除自身。 / Stop walking and delete self at point 12.
				getSpawnTemplate().setWalkerId(null);
				WalkManager.stopWalking(this);
				AI2Actions.deleteOwner(this);
/* 				spawn(219037, 736.2967f, 510.07104f, 941.4781f, (byte) 72); //Tamer Anikiki. */
			}
		}
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		if (getNpcId() != 219040) {
			// 生成的巡逻怪 5 秒后获得增益并回满血。 / Spawned patrol mobs get the buff and full HP after 5 seconds.
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					GameEngineServices.skillEngine().getSkill(getOwner(), 18189, 20, getOwner()).useNoAnimationSkill();
					getLifeStats().setCurrentHp(getLifeStats().getMaxHp());
				}
			}, 5000);
		}
	}
}
