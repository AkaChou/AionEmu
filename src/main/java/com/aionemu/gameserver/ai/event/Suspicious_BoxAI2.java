package com.aionemu.gameserver.ai.event;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_SETTINGS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 活动事件 NPC AI：Suspicious Box（@AIName "suspicious_box"），继承 NpcAI2。
 * Event NPC AI: Suspicious Box (@AIName "suspicious_box"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("suspicious_box")
public class Suspicious_BoxAI2 extends NpcAI2
{
	private Future<?> suspiciousChestTask;
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleDied() {
		suspiciousChestTask.cancel(true);
		super.handleDied();
	}
	
	@Override
    protected void handleCreatureSee(Creature creature) {
        checkDistance(this, creature);
    }
	
    @Override
    protected void handleCreatureMoved(Creature creature) {
        checkDistance(this, creature);
    }
	
	private void checkDistance(NpcAI2 ai, Creature creature) {
        if (creature instanceof Player && !creature.getLifeStats().isAlreadyDead()) {
			final Player player = (Player) creature;
			if (player.isInGroup2() && MathUtil.isIn3dRange(getOwner(), creature, 10)) {
        		AI2Actions.deleteOwner(Suspicious_BoxAI2.this);
				AI2Actions.scheduleRespawn(Suspicious_BoxAI2.this);
				// 一个古代宝箱不见了。 / One of the Ancient Treasure Boxes is missing.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_TREASUREBOX_DESPAWN_ONE, 0);
			} else if (MathUtil.isIn3dRange(getOwner(), creature, 10)) {
				if (startedEvent.compareAndSet(false, true)) {
					suspiciousChestTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					    @Override
						public void run() {
						    eventChestStart();
						}
					}, 1000);
					suspiciousChestTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					    @Override
						public void run() {
						    getOwner().setNpcType(NpcType.ATTACKABLE);
							PacketSendUtility.sendPacket(player, new SM_CUSTOM_SETTINGS(getOwner().getObjectId(), 0, NpcType.ATTACKABLE.getId(), 0));
						}
					}, 3000);
					suspiciousChestTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					    @Override
					    public void run() {
							eventChestFail();
							AI2Actions.deleteOwner(Suspicious_BoxAI2.this);
							AI2Actions.scheduleRespawn(Suspicious_BoxAI2.this);
				        }
			        }, 180000);
				}
			}
        }
    }
	
	private void eventChestStart() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendSys3Message(player, "\uE005", "You have <3 Minutes> for remove a curse on chest");
			}
		});
	}
	private void eventChestFail() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 未在时限内消灭怪物，宝箱已消失。 / The treasure chest has disappeared because you failed to destroy the monsters within the time limit.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDABRECORE_OOPS_REWARD_IS_GONE);
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	@Override
	public int modifyOwnerDamage(int damage) {
		return 1;
	}
	
	@Override
	public int modifyDamage(int damage) {
		return 1;
	}
}
