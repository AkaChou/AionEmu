/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_EDIT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_UPDATE_PLAYER_APPEARANCE;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.utils.PacketSendUtility;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DyeAction")
public class DyeAction extends AbstractItemAction implements HouseDyeAction {
	@XmlAttribute(name = "color")
	protected String color;

	@XmlAttribute
	private Integer minutes;

	@XmlAttribute
	private boolean custom;

	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (targetItem == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		return true;
	}

	private int getColorBGRA() {
		if (color == null || color.equals("no")) {
			return 0;
		} else {
			int rgb = Integer.parseInt(color, 16);
			return 0xFF | ((rgb & 0xFF) << 24) | ((rgb & 0xFF00) << 8) | ((rgb & 0xFF0000) >>> 8);
		}
	}

	public boolean isCustom() {
		return custom;
	}

	public int getColor(int customColor) {
		return custom ? customColor : getColorBGRA();
	}

	@Override
	public void act(Player player, Item parentItem, Item targetItem) {
		if (custom) {
			return;
		}
		act(player, parentItem, targetItem, 0);
	}

	public void act(Player player, Item parentItem, Item targetItem, int customColor) {
		if (custom && customColor == 0) {
			return;
		}
		if (!player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1)) {
			return;
		}
		if (targetItem.getItemTemplate().isItemDyePermitted()) {
			int color = getColor(customColor);
			if (color == 0) {
				targetItem.setItemColor(0);
				targetItem.setColorExpireTime(0);
			} else {
				targetItem.setItemColor(custom ? color : parentItem.getItemTemplate().getTemplateId());
				if (minutes != null)
					targetItem.setColorExpireTime((int) (System.currentTimeMillis() / 1000 + minutes * 60));
			}
			if (player.getEquipment().getEquippedItemByObjId(targetItem.getObjectId()) != null) {
				PacketSendUtility.broadcastPacket(player, new SM_UPDATE_PLAYER_APPEARANCE(player.getObjectId(),
						player.getEquipment().getEquippedForApparence()), true);
				player.getEquipment().setPersistentState(PersistentState.UPDATE_REQUIRED);
			} else {
				player.getInventory().setPersistentState(PersistentState.UPDATE_REQUIRED);
			}
			ItemPacketService.updateItemAfterInfoChange(player, targetItem);
		}
	}

	public int getColor() {
		return getColorBGRA();
	}

	public boolean canAct(Player player, Item parentItem, HouseObject<?> targetHouseObject) {
		if (targetHouseObject == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		if (color.equals("no") && targetHouseObject.getColor() == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_PAINT_ERROR_CANNOTREMOVE);
			return false;
		}
		boolean canPaint = targetHouseObject.getObjectTemplate().getCanDye();
		if (!canPaint) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_PAINT_ERROR_CANNOTPAINT);
		}
		return canPaint;
	}

	public void act(Player player, Item parentItem, HouseObject<?> targetHouseObject) {
		if (!player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1)) {
			return;
		}
		if (color.equals("no")) {
			targetHouseObject.setColor(null);
		} else {
			targetHouseObject.setColor(Integer.parseInt(color, 16));
		}
		float x = targetHouseObject.getX();
		float y = targetHouseObject.getY();
		float z = targetHouseObject.getZ();
		int rotation = targetHouseObject.getRotation();
		PacketSendUtility.sendPacket(player, new SM_HOUSE_EDIT(7, 0, targetHouseObject.getObjectId()));
		PacketSendUtility.sendPacket(player, new SM_HOUSE_EDIT(5, targetHouseObject.getObjectId(), x, y, z, rotation));
		targetHouseObject.spawn();
		int objectName = targetHouseObject.getObjectTemplate().getNameId();
		if (color.equals("no")) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_PAINT_REMOVE_SUCCEED(objectName));
		} else {
			int paintName = parentItem.getItemTemplate().getNameId();
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_PAINT_SUCCEED(objectName, paintName));
		}
	}
}
