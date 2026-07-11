package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 经验加成道具动作模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BonusAddExpAction")
public class BonusAddExpAction extends AbstractItemAction {

	@XmlAttribute(name = "rate")
	protected Integer rate;

	@XmlAttribute()
	protected boolean isPercent = true;

	public BonusAddExpAction() {
	}

	public BonusAddExpAction(Integer rate) {
		this.rate = rate;
	}

	/** 获取比率。 / Returns the rate. */
	public Integer getRate() {
		return rate;
	}

	/** 设置比率。 / Sets the rate. */
	public void setRate(Integer rate) {
		this.rate = rate;
	}

	/**
	 * 是否可以执行动作。
	 * Whether the action can be performed.
	 */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (parentItem == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		return true;
	}

	/**
	 * 执行动作：按比例增加经验并播放使用动画。
	 * Performs the action: adds experience by rate and plays usage animation.
	 */
	@Override
	public void act(final Player player, final Item parentItem, final Item targetItem) {
		long exp = player.getCommonData().getExpNeed();
		long expPercent = Math.round((exp * rate) / 100f);
		if (player.getInventory().decreaseByObjectId(parentItem.getObjectId().intValue(), 1)) {
			player.getCommonData().addExp(expPercent, null);
			player.getObserveController().notifyItemuseObservers(parentItem);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GET_EXP2(expPercent));
			ItemTemplate itemTemplate = parentItem.getItemTemplate();
			PacketSendUtility.broadcastPacket(player,
					new SM_ITEM_USAGE_ANIMATION(player.getObjectId().intValue(), parentItem.getObjectId().intValue(),
							itemTemplate.getTemplateId()),
					true);
		}
	}
}
