package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.handler.FollowEventHandler;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 快递/投递 NPC AI：由创建者派发并在超时后清理。
 * Delivery NPC AI spawned for a creator and cleaned up on timeout.
 *
 * @author Encom
 */
@AIName("deliveryman")
public class DeliveryManAI2 extends FollowingNpcAI2
{
	private Player owner;
	public static int EVENT_SET_CREATOR = 1;
	private static int SERVICE_TIME = 10 * 60 * 1000;
	private static int SPAWN_ACTION_DELAY = 1000;
	
	/**
	 * 处理生成完成事件。
	 * Handle post-spawn.
	 */
	@Override
	protected void handleSpawned() {
		GameThreadPoolServices.threadPoolManager().schedule(new DeleteDeliveryMan(), SERVICE_TIME);
		GameThreadPoolServices.threadPoolManager().schedule(new DeliveryManSpawnAction(), SPAWN_ACTION_DELAY);
		super.handleSpawned();
	}
	
	/**
	 * 处理消失事件。
	 * Handle despawn.
	 */
	@Override
	protected void handleDespawned() {
		sendMsg(390267, getObjectId(), false, 0);
		super.handleDespawned();
	}
	
	/**
	 * 玩家开始与本 NPC 对话/交互。
	 * Player starts dialog/interaction with this NPC.
	 *
	 * 玩家 / player
	 */
	@Override
	protected void handleDialogStart(Player player) {
		if (player.equals(owner)) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 18));
			player.getMailbox().sendMailList(true);
		}
	}
	
	/**
	 * 处理生物移动事件。
	 * Handle creature-moved.
	 *
	 * creature
	 */
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature == owner) {
			FollowEventHandler.creatureMoved(this, creature);
		}
	}
	
	/**
	 * 处理自定义事件。
	 * Handle custom event.
	 *
	 * event id
	 * @param args 附加参数 / extra args
	 */
	@Override
	protected void handleCustomEvent(int eventId, Object... args) {
		if (eventId == EVENT_SET_CREATOR) {
			owner = (Player) args[0];
		}
	}
	
	private final class DeleteDeliveryMan implements Runnable {
		@Override
		public void run() {
			AI2Actions.deleteOwner(DeliveryManAI2.this);
		}
	}
	
	private final class DeliveryManSpawnAction implements Runnable {
		@Override
		public void run() {
		    sendMsg(390266, getObjectId(), false, 2000);
			sendMsg(390268, getObjectId(), false, 5000);
			handleFollowMe(owner);
			handleCreatureMoved(owner);
		}
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
