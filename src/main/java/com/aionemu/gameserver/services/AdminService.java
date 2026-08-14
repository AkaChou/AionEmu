package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
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
 * GM 物品限制服务，控制管理员对受限物品的交易/操作权限。
 * Admin item-restriction service controlling GM trade and operation permissions for restricted items.
 *
 * @author KID
 */
@Slf4j
public class AdminService {
	private static volatile ObjectProvider<AdminService> instanceProvider;
	/** 受限物品 ID 列表 / Restricted item ID list */
	private List<Integer> list;

	@Slf4j(topic = "GMITEMRESTRICTION")
	private static class ItemRestrictionLog {
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static AdminService getInstance() {
		ObjectProvider<AdminService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<AdminService> instanceProvider) {
		AdminService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final AdminService instance = new AdminService();
	}

	/**
	 * 构造服务；若启用交易限制则加载配置。
	 * Constructs the service; reloads the restriction list when trade restriction is enabled.
	 */
	public AdminService() {
		list = new ArrayList<Integer>();
		if (AdminConfig.ENABLE_TRADEITEM_RESTRICTION)
			reload();
	}

	/**
	 * 从配置文件重新加载受限物品列表。
	 * Reloads the restricted item list from the configuration file.
	 */
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
			log.error(I18n.get("log.2dd4f31d928c", e));
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.warn(I18n.get("log.1a564ebb3da2", e));
				}
			}
		}

		log.info(I18n.get("log.68a0fce84f7f", list.size()));
	}

	/**
	 * 检查玩家是否可对指定物品执行操作（基于 Item 对象）。
	 * Checks whether the player may operate on the given item (Item overload).
	 *
	 * @param player 操作者 / operator
	 * @param target 目标玩家，可为 null / target player, may be null
	 * @param item 目标物品 / item
	 * @param type 操作类型描述 / operation type description
	 * @return 允许则为 true / true if allowed
	 */
	public boolean canOperate(Player player, Player target, Item item, String type) {
		return canOperate(player, target, item.getItemId(), type);
	}

	/**
	 * 检查玩家是否可对指定物品 ID 执行操作。
	 * Checks whether the player may operate on the given item ID.
	 *
	 * @param player 操作者 / operator
	 * @param target 目标玩家，可为 null / target player, may be null
	 * @param itemId 目标物品 ID / item id
	 * @param type 操作类型描述 / operation type description
	 * @return 允许则为 true / true if allowed
	 */
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
