package com.aionemu.gameserver.ai.rvr;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AI2Request;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_OBJECT;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * RvR 相关 NPC AI：Legion Advance Corridor（@AIName "direct_portal"），继承 NpcAI2。
 * RvR-related NPC AI: Legion Advance Corridor (@AIName "direct_portal"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("direct_portal")
public class Legion_Advance_CorridorAI2 extends NpcAI2
{
	protected int startBarAnimation = 1;
	protected int cancelBarAnimation = 2;
	private final int CANCEL_DIALOG_METERS = 10;
	
	@Override
	protected void handleDialogStart(Player player) {
		handleUseItemStart(player);
	}
	
	protected void handleUseItemStart(final Player player) {
		final int delay = getTalkDelay();
		if (delay != 0) {
			final ItemUseObserver observer = new ItemUseObserver() {
				@Override
				public void abort() {
					player.getController().cancelTask(TaskId.ACTION_ITEM_NPC);
					PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.END_QUESTLOOT, 0, getObjectId()), true);
					PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), 0, cancelBarAnimation));
					player.getObserveController().removeObserver(this);
				}
			};
			player.getObserveController().attach(observer);
			PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), getTalkDelay(), startBarAnimation));
			PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.START_QUESTLOOT, 0, getObjectId()), true);
			player.getController().addTask(TaskId.ACTION_ITEM_NPC, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.END_QUESTLOOT, 0, getObjectId()), true);
					PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), getTalkDelay(), cancelBarAnimation));
					player.getObserveController().removeObserver(observer);
					handleUseItemFinish(player);
				}
			}, delay));
		} else {
			handleUseItemFinish(player);
		}
	}
	
	protected void handleUseItemFinish(Player player) {
		if (player.getLevel() >= 45) {
			// 是否要通过次元漩涡渗透敌区？ / Do you want to infiltrate the enemy zone through the Dimensional Vortex ?
			// 将自动把你从当前小队/联盟移出并加入渗透联盟。 / This will automatically remove you from your current group/alliance and and place you in an infiltration alliance.
			AI2Actions.addRequest(this, player, SM_QUESTION_WINDOW.STR_ASK_PASS_BY_INVADE_DIRECT_PORTAL, getOwner().getObjectId(), CANCEL_DIALOG_METERS, new AI2Request() {
				private boolean decisionTaken = false;
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					if (!decisionTaken) {
			            switch (getNpcId()) {
							case 703093: //Directportal_Start_LF3_DF3 In.
								transferRvrElyos1(responder);
							break;
							case 703094: //Directportal_Start_LF3_LF3 In.
								transferRvrElyos2(responder);
							break;
							case 703095: //Directportal_Start_DF3_LF3 In.
								transferRvrAsmodians1(responder);
							break;
							case 703096: //Directportal_Start_DF3_DF3 In.
								transferRvrAsmodians2(responder);
							break;
						}
						decisionTaken = true;
					}
				}
				@Override
				public void denyRequest(Creature requester, Player responder) {
					decisionTaken = true;
				}
			});
		} else {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_INVADE_DIRECT_PORTAL_LEVEL_LIMIT);
		}
	}
	
	protected int getTalkDelay() {
		return getObjectTemplate().getTalkDelay() * 1000;
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
   /**
	 * @param responder 天族 / Elyos
	 */
    private void transferRvrElyos1(Player responder) {
        TeleportService2.teleportTo(responder, 220040000, 720.6933f, 2137.6619f, 305.9253f, (byte) 90);
    }
	private void transferRvrElyos2(Player responder) {
        TeleportService2.teleportTo(responder, 210040000, 1370.1682f, 671.9592f, 180.76796f, (byte) 12);
    }
	
   /**
	 * @param responder 魔族 / Asmodians
	 */
	private void transferRvrAsmodians1(Player responder) {
        TeleportService2.teleportTo(responder, 210040000, 862.318f, 318.10703f, 139.89037f, (byte) 6);
    }
	private void transferRvrAsmodians2(Player responder) {
        TeleportService2.teleportTo(responder, 220040000, 790.0702f, 1434.9897f, 312.33909f, (byte) 66);
    }
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Legion_Advance_CorridorAI2.this);
			}
		}, 3540000);
	}
}
