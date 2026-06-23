/*
 * This file is part of Encom.
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
package admincommands;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Wakizashi, Imaginary
 *
 */
public class Reskin extends AdminCommand {
	public Reskin()	{
		super("reskin");
    }

	@Override
	public void execute(Player admin, String... params) {
		if (params.length != 2) {
			onFail(admin, null);
			return;
		}

		Player target = admin;
		VisibleObject creature = admin.getTarget();
		if (admin.getTarget() instanceof Player) {
			target = (Player) creature;
		}
		
		int oldItemId = parseItemId(params[0]);
		int newItemId = parseItemId(params[1]);
		
		if (oldItemId == 0) {
			PacketSendUtility.sendMessage(admin, "Invalid old item ID or name: " + params[0]);
			return;
		}
		
		if (newItemId == 0) {
			PacketSendUtility.sendMessage(admin, "Invalid new item ID or name: " + params[1]);
			return;
		}

		List<Item> items = target.getInventory().getItemsByItemId(oldItemId);
		if(items.size() == 0) {
			PacketSendUtility.sendMessage(admin, "You don't have this item in your inventory.");
			return;
		}
		
		Iterator<Item> iter = items.iterator();
		if (iter.hasNext()) {
			Item item = iter.next();
			item.setItemSkinTemplate(DataManager.ITEM_DATA.getItemTemplate(newItemId));
			PacketSendUtility.sendMessage(admin, "Reskin Successfull.");
		} else {
			PacketSendUtility.sendMessage(admin, "Error: No items to reskin.");
		}
	}
	
	private int parseItemId(String param) {
		if (param == null || param.isEmpty()) {
			return 0;
		}
		
		if (param.startsWith("[item:")) {
			Pattern pattern = Pattern.compile("\\[item:(\\d+)");
			Matcher matcher = pattern.matcher(param);
			
			if (matcher.find()) {
				return Integer.parseInt(matcher.group(1));
			}
			return 0;
		}
		
		try {
			return Integer.parseInt(param);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	@Override
	public void onFail(Player admin, String message) {
		PacketSendUtility.sendMessage(admin, "syntax //reskin <Old Item ID> <New Item ID>");
	}
}