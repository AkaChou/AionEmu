package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_EXP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Purifier 经验动作模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PurifierExpAction")
public class PurifierExpAction extends AbstractItemAction {
	@XmlAttribute(name = "extraction_cost")
	protected int extractionCost;

	@XmlAttribute(name = "percent")
	protected boolean isPercent;

	@XmlAttribute(name = "extraction_reward")
	protected int extractionReward;

	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
	 */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (parentItem == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		if (player.getCommonData().getExp() == 0) {
			return false;
		}
		if (player.getInventory().isFull()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_WAREHOUSE_FULL_INVENTORY);
			return false;
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, Item targetItem) {
		PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId().intValue(),
				parentItem.getObjectId().intValue(), parentItem.getItemTemplate().getTemplateId(), 3000, 0, 0));
		player.getController().cancelTask(TaskId.ITEM_USE);
		final ItemUseObserver observer = new ItemUseObserver() {
			/** 中止 / abort. */
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_DECOMPOSE_ITEM_CANCELED(parentItem.getNameId()));
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId().intValue(),
						parentItem.getObjectId().intValue(), parentItem.getItemTemplate().getTemplateId(), 0, 2, 0));
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			public void run() {
				player.getObserveController().removeObserver(observer);
				int expExtraction = 0;
				if (isPercent) {
					expExtraction = (int) (player.getCommonData().getExpNeed() / 100) * extractionCost;
				} else {
					expExtraction = extractionCost;
				}
				player.getCommonData().setExp(player.getCommonData().getExp() - expExtraction, false);
				ItemService.addItem(player, extractionReward, 1);
				player.getInventory().decreaseByItemId(parentItem.getItemId(), 1);
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId().intValue(),
						parentItem.getObjectId().intValue(), parentItem.getItemTemplate().getTemplateId(), 0, 1, 0));
			}
		}, 3000));
		PacketSendUtility.sendPacket(player,
				new SM_STATUPDATE_EXP(player.getCommonData().getExpShown(), player.getCommonData().getExpRecoverable(),
						player.getCommonData().getExpNeed(), player.getCommonData().getCurrentReposteEnergy(),
						player.getCommonData().getMaxReposteEnergy()));
	}
}
