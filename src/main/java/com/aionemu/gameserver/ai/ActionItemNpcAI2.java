package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_OBJECT;
//import com.aionemu.gameserver.services.ThreesUpgradeService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 可交互道具型 NPC AI：处理使用进度条、观察者中断与使用完成回调。
 * Use-item style NPC AI that handles the progress bar, abort observer, and use-finish callback.
 */
@AIName("useitem")
public class ActionItemNpcAI2 extends NpcAI2
{
	protected int startBarAnimation = 1;
	protected int cancelBarAnimation = 2;
	
	/**
	 * 玩家开始与本 NPC 对话/交互。
	 * Player starts dialog/interaction with this NPC.
	 *
	 * 玩家 / player
	 */
	@Override
	protected void handleDialogStart(Player player) {
		handleUseItemStart(player);
	}
	
	/**
	 * 开始使用交互物（进度条）。
	 * Start using the action item (progress bar).
	 *
	 * @param player 玩家 / player
	 */
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
	
	/**
	 * 使用交互物完成时的逻辑。
	 * Logic when action-item use finishes.
	 *
	 * @param player 玩家 / player
	 */
	protected void handleUseItemFinish(Player player) {
		if (getOwner().isInInstance()) {
			AI2Actions.handleUseItemFinish(this, player);
		}
	}

	/**
	 * 返回交互进度条时长（毫秒）。
	 * Return interaction progress-bar duration in milliseconds.
	 */
	protected int getTalkDelay() {
		return getObjectTemplate().getTalkDelay() * 1000;
	}
}
