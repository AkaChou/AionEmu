package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Extract 欧比斯动作模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExtractAbyssAction")
public class ExtractAbyssAction extends AbstractItemAction {
	@XmlAttribute(name = "apextractionrate")
	protected Integer apextractionrate;

	protected String itemCategory;

	public ExtractAbyssAction() {
	}

	public ExtractAbyssAction(Integer apextractionrate, String itemCategory) {
		this.apextractionrate = apextractionrate;
		this.itemCategory = itemCategory;
	}

	/** 获取比率。 / Returns the rate. */
	public Integer getRate() {
		return apextractionrate;
	}

	/** 设置比率。 / Sets the rate. */
	public void setRate(Integer apextractionrate) {
		this.apextractionrate = apextractionrate;
	}

	/** 获取物品分类。 / Returns the item category. */
	public String getItemCategory() {
		return itemCategory;
	}

	/** 设置物品分类。 / Sets the item category. */
	public void setItemCategory(String itemCategory) {
		this.itemCategory = itemCategory;
	}

	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
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
	public void act(final Player player, final Item parentItem, final Item targetItem) {
		player.getController().cancelUseItem();
		PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(),
				parentItem.getItemTemplate().getTemplateId(), 3000, 0, 0));
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				if (targetItem.getItemTemplate().getAcquisition().getRequiredAp() != 0) {
					AbyssPointsService.addAp(player,
							(int) (targetItem.getItemTemplate().getAcquisition().getRequiredAp()
									* 1000f)));
					player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1);
					player.getInventory().decreaseItemCount(targetItem, 1);
				}
				PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						parentItem.getObjectId(), parentItem.getItemId(), 0, 1, 0));
			}
		}, 3000));
	}
}
