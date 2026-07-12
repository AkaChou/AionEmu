package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.RecipeService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 制作 Learn 动作模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer, MrPoke, KID
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CraftLearnAction")
public class CraftLearnAction extends AbstractItemAction {
	@XmlAttribute
	protected int recipeid;

	/** 执行 / act. */
	@Override
	public void act(Player player, Item parentItem, Item targetItem) {
		player.getController().cancelUseItem();
		if (player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1)) {
			if (RecipeService.addRecipe(player, recipeid, false)) {
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_USE_ITEM(new DescriptionId(parentItem.getItemTemplate().getNameId())));
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId()));
			}
		}
	}

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		return RecipeService.validateNewRecipe(player, recipeid) != null;
	}

	/** 返回配方 ID / Returns the recipe id */
	public int getRecipeId() {
		return recipeid;
	}
}
