package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.skinskill.SkillSkinList;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 技能 Animation 动作模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SkillAnimationAction")
public class SkillAnimationAction extends AbstractItemAction {

	@XmlAttribute(name = "skin_id")
	protected int skinId;
	@XmlAttribute(name = "minutes")
	protected int minutes;
	private int expireTime = 0;

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (skinId == 0 || parentItem == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		if (player.getSkillSkinList() != null && player.getSkillSkinList().contains(skinId)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_COSTUME_SKILL_ALREADY_HAS_COSTUME);
			return false;
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(Player player, Item parentItem, Item targetItem) {
		ItemTemplate itemTemplate = parentItem.getItemTemplate();
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), itemTemplate.getTemplateId()), true);
		if (minutes > 0) {
			expireTime = (int) (System.currentTimeMillis() / 1000 + minutes * 60);
		}
		player.getSkillSkinList().addSkillSkin(skinId, minutes * 60, expireTime);
		Item item = player.getInventory().getItemByObjId(parentItem.getObjectId());
		player.getInventory().delete(item);
	}
}
