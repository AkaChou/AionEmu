package com.aionemu.gameserver.ai.instance.dredgionDefense;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dredgion Defense 副本 NPC AI：Defence Tower Of Sanctum 1（@AIName "Defence_Tower_Of_Sanctum_1"），继承 ActionItemNpcAI2。
 * Dredgion Defense instance NPC AI: Defence Tower Of Sanctum 1 (@AIName "Defence_Tower_Of_Sanctum_1"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("Defence_Tower_Of_Sanctum_1")
public class Defence_Tower_Of_Sanctum_1AI2 extends ActionItemNpcAI2
{
	private Future<?> dreadgionDrakanATKTask;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleDialogStart(Player player) {
		super.handleDialogStart(player);
	}
	
	@Override
	protected void handleUseItemFinish(Player player) {
		// 圣所防御炮塔冷却剂。 / Sanctum Defense Turret Coolant.
		if (!player.getInventory().decreaseByItemId(185000284, 1)) {
			// 你没有冷却剂。 / You don’t have coolant.
            PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403692));
			return;
        }
		dreadgionDrakanATK();
		startDreadgionOverheatTask();
		// 防御炮塔已冷却到可使用！ / The defense turret is now cool enough to use!
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403951));
	}
	
	@Override
	protected void handleSpawned() {
		dreadgionDrakanATK();
		startDreadgionOverheatTask();
		super.handleSpawned();
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 220816: //Defence Tower Of Sanctum 1.
				    announceDefenceSanctumA();
				break;
			}
		}
	}
	
	private void dreadgionDrakanATK() {
		dreadgionDrakanATKTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				AI2Actions.targetCreature(Defence_Tower_Of_Sanctum_1AI2.this, getPosition().getWorldMapInstance().getNpc(220966));
                AI2Actions.useSkill(Defence_Tower_Of_Sanctum_1AI2.this, 18311);
			}
		}, 4000, 10000);
	}
	
	private void startDreadgionOverheatTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				cancelATKTask();
				announceDreadgionOverheat();
				//AI2Actions.useSkill(Defence_Tower_Of_Sanctum_1AI2.this, 18310); //Dreadgion Overheat.
			}
		}, 300000);
	}
	
	private void announceDefenceSanctumA() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 防御炮塔正遭受攻击！ / The defense turret is being attacked!
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403709));
				}
			}
		});
	}
	private void announceDefenceSanctumDieA() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 防御炮塔已被摧毁。 / The defense turret has been destroyed.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403710));
				}
			}
		});
	}
	private void announceDreadgionOverheat() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 防御炮塔过热。去找冷却剂！ / The defense turret is overheating. Find some coolant!
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403947));
				}
			}
		});
	}
	
	@Override
	protected void handleDied() {
		announceDefenceSanctumDieA();
		super.handleDied();
	}
	
	private void cancelATKTask() {
		if (dreadgionDrakanATKTask != null && !dreadgionDrakanATKTask.isDone()) {
			dreadgionDrakanATKTask.cancel(true);
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
