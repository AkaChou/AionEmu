package com.aionemu.gameserver.model.templates.item.actions;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Read 动作模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReadAction")
public class ReadAction extends AbstractItemAction {

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, Item parentItem, Item targetItem) {
		final int itemObjId = parentItem.getObjectId();
		final int id = parentItem.getItemTemplate().getTemplateId();

		PacketSendUtility.broadcastPacket(player,
				new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, id, 50, 0, 0), true);
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, id, 0, 1, 0), true);
			}
		}, 50);
	}
}
