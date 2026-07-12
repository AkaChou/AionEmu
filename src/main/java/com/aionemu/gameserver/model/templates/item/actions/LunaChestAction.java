package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 月华宝箱动作模板（静态数据/XML）。
 * XML template.
 */

public class LunaChestAction extends AbstractItemAction {

	@XmlAttribute
	protected int count;

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (parentItem == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, Item targetItem) {
		player.getController().cancelUseItem();
		PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), 0, parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId(), 1000, 0, 0));
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			/** 运行 / run. */
			@Override
			public void run() {
				int openCount = getOpenCount(parentItem.getItemCount());
				boolean succ = player.getInventory().decreaseByObjectId(parentItem.getObjectId(), openCount);
				PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), 0, parentItem.getObjectId(), parentItem.getItemId(), 0, 1, 0));
				if (succ) {
					int lunaReward = getLunaReward(count, openCount);
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300423, new Object[] { new DescriptionId(parentItem.getItemTemplate().getNameId()) }));
					player.setLunaAccount(player.getLunaAccount() + lunaReward);
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GETLUNA(player.getName(), lunaReward));
					PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
				}
			}
		}, 1000));
	}

	static int getOpenCount(long itemCount) {
		return (int) Math.max(1, itemCount);
	}

	static int getLunaReward(int count, int openCount) {
		return count * openCount;
	}
}
