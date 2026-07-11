package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * F2p 动作模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "F2pAction")
public class F2pAction extends AbstractItemAction {
	@XmlAttribute
	protected String pack;

	@XmlAttribute
	protected Integer minutes;

	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
	 */
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (parentItem == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		if (player.getF2p() != null && player.getF2p().getF2pAccount() != null
				&& player.getF2p().getF2pAccount().getActive()) {
			PacketSendUtility.sendWarnMessageOnCenter(player, "You cannot accumulate 2 <Gold Pack> at the same time.");
			return false;
		}
		return true;
	}

	/** 执行 / act. */
	public void act(final Player player, final Item parentItem, Item targetItem) {
		player.getController().cancelUseItem();
		PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId().intValue(),
				parentItem.getObjectId().intValue(), parentItem.getItemTemplate().getTemplateId(), 1000, 0, 0));
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				boolean succ = player.getInventory().decreaseByObjectId(parentItem.getObjectId().intValue(), 1);
				PacketSendUtility.broadcastPacketAndReceive(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId().intValue(),
								parentItem.getObjectId().intValue(), parentItem.getItemId(), 0, 1, 0));
				if (succ) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300423,
							new Object[] { new DescriptionId(parentItem.getItemTemplate().getNameId()) }));
					GameFeatureServices.f2pService().onAddF2p(player, minutes);
				}
			}
		}, 1000));
	}
}
