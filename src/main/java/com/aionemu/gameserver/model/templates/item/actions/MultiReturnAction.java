package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.teleport.MultiReturnLocationList;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.services.teleport.MultiReturnService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * MultiReturn 动作模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MultiReturnAction")
public class MultiReturnAction extends AbstractItemAction {
	/**
	 * 6 为天族，7 为魔族。 / 6 for ELYOS, 7 for ASMODIANS.
	 */
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

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
	public void act(final Player player, final Item MultiReturn, final int SelectedMapIndex) {
		PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
				MultiReturn.getObjectId(), MultiReturn.getItemTemplate().getTemplateId(), 3000, 0, 0));
		player.getController().cancelTask(TaskId.ITEM_USE);
		final ItemUseObserver observer = new ItemUseObserver() {
			/** 中止 / abort. */
			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						MultiReturn.getObjectId(), MultiReturn.getItemTemplate().getTemplateId(), 0, 2, 0));
				player.getObserveController().removeObserver(this);
				player.removeItemCoolDown(MultiReturn.getItemTemplate().getUseLimits().getDelayId());
			}
		};
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				player.getObserveController().removeObserver(observer);
				if (player.getInventory().decreaseByObjectId(MultiReturn.getObjectId(), 1)) {
					int MultiReturnId = getId();
					com.aionemu.gameserver.model.templates.teleport.MultiReturn rItem = DataManager.MULTI_RETURN_ITEM_DATA
							.getMultiReturnById(MultiReturnId);
					if (rItem != null && rItem.getMultiReturnList() != null) {
						MultiReturnLocationList ReturnData = rItem.getReturnDataById(SelectedMapIndex);
						if (ReturnData != null) {
							int ReturnCount = rItem.getMultiReturnList().size();
							if (SelectedMapIndex <= (ReturnCount - 1)) {
								int worldId = ReturnData.getWorldId();
								int LocId = MultiReturnService.getTeleportWorldId(worldId, player.getRace());
								MultiReturnService.Teleport(player, LocId, worldId);
							}
						}
					}
				}
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						MultiReturn.getObjectId(), MultiReturn.getItemTemplate().getTemplateId(), 0, 1, 0));
			}
		}, 3000));
	}
}
