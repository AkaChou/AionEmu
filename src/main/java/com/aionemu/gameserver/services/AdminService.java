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
package com.aionemu.gameserver.services;

import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author KID
 */
@Slf4j
public class AdminService {
	private static volatile ObjectProvider<AdminService> instanceProvider;
	private List<Integer> list;

	@Slf4j(topic = "GMITEMRESTRICTION")
	private static class ItemRestrictionLog {
	}

	public static AdminService getInstance() {
		ObjectProvider<AdminService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	public static void setInstanceProvider(ObjectProvider<AdminService> instanceProvider) {
		AdminService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final AdminService instance = new AdminService();
	}

	public AdminService() {
		list = new ArrayList<Integer>();
		if (AdminConfig.ENABLE_TRADEITEM_RESTRICTION)
			reload();
	}

	public void reload() {
		if (list.size() > 0) {
			list.clear();
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(Config.configFile("administration/item.restriction.txt")));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#") || line.trim().length() == 0) {
					continue;
				}
				String pt = line.split("#")[0].replaceAll(" ", "");
				list.add(Integer.parseInt(pt));
			}
		} catch (IOException e) {
			log.error("Failed to load item restriction list", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.warn("Failed to close item restriction list", e);
				}
			}
		}

		log.info("AdminService loaded {} operational items", list.size());
	}

	public boolean canOperate(Player player, Player target, Item item, String type) {
		return canOperate(player, target, item.getItemId(), type);
	}

	public boolean canOperate(Player player, Player target, int itemId, String type) {
		if (!AdminConfig.ENABLE_TRADEITEM_RESTRICTION) {
			return true;
		}
		if (target != null && target.getAccessLevel() > 0) {// allow between gms
			return true;
		}
		if (player.getAccessLevel() > 0 && player.getAccessLevel() < 4) { // run check only for 1-3 level gms
			boolean value = list.contains(itemId);
			String str = "GM " + player.getName() + "|" + player.getObjectId() + " (" + type + "): " + itemId
					+ "|result=" + value;
			if (target != null) {
				str += "|target=" + target.getName() + "|" + target.getObjectId();
			}
			ItemRestrictionLog.log.info(str);
			if (!value) {
				PacketSendUtility.sendMessage(player, "You cannot use " + type + " with this item.");
			}
			return value;
		} else {
			return true;
		}
	}
}
