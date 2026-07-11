package com.aionemu.gameserver.ai.rvr.kenovikan;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RvR 相关 NPC AI：Armored Transport Delta（@AIName "Armored_Transport_Delta"），继承 GeneralNpcAI2。
 * RvR-related NPC AI: Armored Transport Delta (@AIName "Armored_Transport_Delta"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("Armored_Transport_Delta")
public class Armored_Transport_DeltaAI2 extends GeneralNpcAI2
{
    private boolean canThink = true;
	private String walkerId = "220110004";
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	private AtomicBoolean startedEvent = new AtomicBoolean(false);

	@Override
	public boolean canThink() {
		return canThink;
	}

	private void removeF6RewardTrans() {
		getOwner().getEffectController().removeEffect(17774);
	}

	private void F6RewardTrans() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 17774, 1, getOwner()).useNoAnimationSkill();
	}

	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 100) {
				if (startedEvent.compareAndSet(false, true)) {
					canThink = false;
					getSpawnTemplate().setWalkerId("220110004");
					WalkManager.startWalking(this);
					getOwner().setState(1);
					PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getObjectId()));
				}
			}
		}
	}

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 246466:
				    announceF6RaidSumAtta01Light();
				break;
			}
		}
	}

	private void announceF6RaidSumAtta01Light() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 敌人正在取回我们的碎片。 / The enemy is retrieving our fragment.
					// 摧毁敌方运输体，阻止其夺走碎片！ / Destroy the enemy's carrier and stop them from taking the fragment!
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1404263));
				}
			}
		});
	}
	private void announceF6RaidSumKill04DarkDie() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 装甲运输德尔塔被摧毁，部分玩家获得特殊效果。 / Armored Transport Delta was destroyed, and some users were given a special effect.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1404270));
				}
			}
		});
	}

	@Override
	protected void handleSpawned() {
		F6RewardTrans();
		super.handleSpawned();
	}

	@Override
	protected void handleBackHome() {
		F6RewardTrans();
		super.handleBackHome();
	}

	@Override
	protected void handleDied() {
		removeF6RewardTrans();
		announceF6RaidSumKill04DarkDie();
		super.handleDied();
	}
}
