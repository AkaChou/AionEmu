package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceLimitService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 副本 TimeClear 模板（静态数据/XML）。
 * XML template.
 */

public class InstanceTimeClear extends AbstractItemAction {
	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, Item targetItem) {
	}

	/** 执行 / act. */
	public void act(final Player player, final Item parentItem, final int SelectedSyncId) {
		if (player.getInstanceLimits().get(SelectedSyncId) == null
				|| player.getInstanceLimits().get(SelectedSyncId).getUsed() == 0) {
			player.getController().cancelTask(TaskId.ITEM_USE);
			player.removeItemCoolDown(parentItem.getItemTemplate().getUseLimits().getDelayId());
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_COOL_TIME_INIT);
			return;
		}
		PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
				parentItem.getObjectId(), parentItem.getItemId(), 1000, 0, 0));
		final ItemUseObserver observer = new ItemUseObserver() {
			/** 中止 / abort. */
			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				player.removeItemCoolDown(parentItem.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
						.STR_ITEM_CANCELED(new DescriptionId(parentItem.getItemTemplate().getNameId())));
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId(), 0, 2, 0), true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				player.getObserveController().removeObserver(observer);
				if (parentItem.getActivationCount() > 1) {
					parentItem.setActivationCount(parentItem.getActivationCount() - 1);
				} else {
					player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1);
				}
				InstanceLimitService.restoreEntry(player, SelectedSyncId);
				PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						parentItem.getObjectId(), parentItem.getItemId(), 0, 1, 0));
			}
		}, 1000));
	}
}
