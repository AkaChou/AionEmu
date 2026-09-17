package com.aionemu.gameserver.model.templates.item.actions;

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
import com.aionemu.gameserver.model.templates.teleport.MultiReturn;
import com.aionemu.gameserver.model.templates.teleport.MultiReturnLocationList;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.MultiReturnService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.Getter;

/**
 * MultiReturn 动作模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MultiReturnAction")
public class MultiReturnAction extends AbstractItemAction {
	/**
	 * 6 为天族，7 为魔族。 / 6 for ELYOS, 7 for ASMODIANS.
	 */
	@XmlAttribute(name = "id")
	private int id;

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		return true;
	}

	public boolean canAct(Player player, int selectedMapIndex) {
		MultiReturnLocationList returnData = getReturnData(selectedMapIndex);
		if (returnData == null) {
			return false;
		}
		if (TeleportService2.isAbyssEntryWorld(returnData.getWorldId())
				&& !TeleportService2.meetsAbyssEntryRequirement(player)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_TELEPORT_TO_ABYSS);
			return false;
		}
		if (TeleportService2.isBalaureaEntryWorld(returnData.getWorldId())
				&& !TeleportService2.meetsBalaureaEntryRequirement(player, returnData.getWorldId())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return false;
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, Item targetItem) {
	}

	/** 执行 / act. */
	public void act(final Player player, final Item MultiReturn, final int SelectedMapIndex) {
		MultiReturnLocationList returnData = getReturnData(SelectedMapIndex);
		if (!canAct(player, SelectedMapIndex)) {
			return;
		}
		final int worldId = returnData.getWorldId();
		PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
				MultiReturn.getObjectId(), MultiReturn.getItemTemplate().getTemplateId(), 3000, 0, 0));
		player.getController().cancelTask(TaskId.ITEM_USE);
		final ItemUseObserver observer = new ItemUseObserver() {
			/** 中止 / abort. */
			@Override
			public void abort() {
				if (player.getController().cancelTask(TaskId.ITEM_USE) == null) {
					player.getObserveController().removeObserver(this);
					return;
				}
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						MultiReturn.getObjectId(), MultiReturn.getItemTemplate().getTemplateId(), 0, 2, 0));
				player.getObserveController().removeObserver(this);
				player.removeItemCoolDown(MultiReturn.getItemTemplate().getUseLimits().getDelayId());
			}
		};
		player.getObserveController().attach(observer);
		player.getController().scheduleTask(TaskId.ITEM_USE, new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				player.getObserveController().removeObserver(observer);
				if (player.getInventory().decreaseByObjectId(MultiReturn.getObjectId(), 1)) {
					int locId = MultiReturnService.getTeleportWorldId(worldId, player.getRace());
					if (locId != 0) {
						MultiReturnService.Teleport(player, locId, worldId);
					}
				}
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						MultiReturn.getObjectId(), MultiReturn.getItemTemplate().getTemplateId(), 0, 1, 0));
			}
		}, 3000);
	}

	private MultiReturnLocationList getReturnData(int selectedMapIndex) {
		MultiReturn multiReturn = DataManager.MULTI_RETURN_ITEM_DATA.getMultiReturnById(getId());
		if (multiReturn == null || multiReturn.getMultiReturnList() == null
				|| selectedMapIndex < 0 || selectedMapIndex >= multiReturn.getMultiReturnList().size()) {
			return null;
		}
		return multiReturn.getReturnDataById(selectedMapIndex);
	}
}
