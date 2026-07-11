package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Extract 经验动作模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExtractExpAction")
public class ExtractExpAction extends AbstractItemAction {
	@XmlAttribute(name = "expextractionrate")
	protected Integer expextractionrate;

	@XmlAttribute(name = "reward")
	protected Integer reward;

	public ExtractExpAction() {
	}

	public ExtractExpAction(Integer expextractionrate) {
		this.expextractionrate = expextractionrate;
	}

	/** 获取比率。 / Returns the rate. */
	public Integer getRate() {
		return expextractionrate;
	}

	/** 设置比率。 / Sets the rate. */
	public void setRate(Integer expextractionrate) {
		this.expextractionrate = expextractionrate;
	}

	/** 获取奖励。 / Returns the reward. */
	public Integer getReward() {
		return reward;
	}

	/** 设置奖励。 / Sets the reward. */
	public void setReward(Integer reward) {
		this.reward = reward;
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
		ItemTemplate itemTemplate = parentItem.getItemTemplate();
		ItemService.addItem(player, getReward(), 1);
		player.getCommonData().addExp((long) -((player.getCommonData().getExpNeed() * getRate()) / 100f),
				RewardType.HUNTING);
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
				parentItem.getObjectId(), itemTemplate.getTemplateId()), true);
		player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1);
	}
}
