package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerAppearanceDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.templates.cosmeticitems.CosmeticItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 外观物品动作模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CosmeticItemAction")
public class CosmeticItemAction extends AbstractItemAction {

	@XmlAttribute(name = "name")
	protected String cosmeticName;

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		CosmeticItemTemplate template = DataManager.COSMETIC_ITEMS_DATA.getCosmeticItemsTemplate(cosmeticName);
		if (template == null) {
			return false;
		}
		if (!template.getRace().equals(player.getRace())) {
			return false;
		}
		if (!template.getGenderPermitted().equals("ALL")) {
			return player.getGender().toString().equals(template.getGenderPermitted());
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, Item parentItem, Item targetItem) {
		CosmeticItemTemplate template = DataManager.COSMETIC_ITEMS_DATA.getCosmeticItemsTemplate(cosmeticName);
		PlayerAppearance playerAppearance = player.getPlayerAppearance();
		String type = template.getType();
		int id = template.getId();
        switch (type) {
            case "hair_color":
                playerAppearance.setHairRGB(id);
                break;
            case "face_color":
                playerAppearance.setSkinRGB(id);
                break;
            case "lip_color":
                playerAppearance.setLipRGB(id);
                break;
            case "eye_color":
                playerAppearance.setEyeRGB(id);
                break;
            case "hair_type":
                playerAppearance.setHair(id);
                break;
            case "face_type":
                playerAppearance.setFace(id);
                break;
            case "voice_type":
                playerAppearance.setVoice(id);
                break;
            case "makeup_type":
                playerAppearance.setTattoo(id);
                break;
            case "tattoo_type":
                playerAppearance.setDeco(id);
                break;
            case "preset_name":
                CosmeticItemTemplate.Preset preset = template.getPreset();
                playerAppearance.setEyeRGB((preset.getEyeColor()));
                playerAppearance.setLipRGB((preset.getLipColor()));
                playerAppearance.setHairRGB((preset.getHairColor()));
                playerAppearance.setSkinRGB((preset.getEyeColor()));
                playerAppearance.setHair((preset.getHairType()));
                playerAppearance.setFace((preset.getFaceType()));
                playerAppearance.setHeight((preset.getScale()));
                break;
        }
		DAOManager.getDAO(PlayerAppearanceDAO.class).store(player);
		player.getInventory().delete(targetItem);
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
		player.clearKnownlist();
		player.updateKnownlist();
	}
}
